package casper.server;

import casper.net.Packet;
import casper.server.commands.Command;
import casper.server.commands.Name;
import casper.server.commands.Usage;
import casper.server.commands.impl.AdminList;
import casper.server.commands.impl.ClientList;
import casper.server.commands.impl.SendCommand;
import casper.server.handlers.AdminHandler;
import casper.server.handlers.ClientHandler;
import casper.server.packet.PacketDecoder;
import casper.server.packet.PacketEncoder;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Aazim
 */
public class Server {

    /**
     * This is a nasty way to do it, but this array stores all of the classes in the commands package,
     * if the classes are not declared in this array, they are not loaded. I will change this later on
     * when I have time.
     */
    public static final Class<?>[] HANDLERS = {
            AdminList.class,
            ClientList.class,
            SendCommand.class
    };

    /**
     * In order to make sure that clients are still connected to the server, Casper uses PING and PONG
     * messages. The way this works is: the server sends a PING_PACKET (created below) to the client,
     * the client then responds with a PONG_PACKET, in doing so, shows that the client is still online.
     */
    private static final Packet PING_PACKET = new Packet(0, 0);

    /**
     * There is a sort of account manager for people to have specific access to the server. Currently
     * the only account type is an "admin." This Map simply stores the admin account in a simple
     * [Username, Password] format.
     */
    private static final Map<String, String> accounts = new HashMap<String, String>();

    /**
     * The timer used for sending PING requests to the clients.
     *
     * @see PingTask
     */
    public final Timer timer = new HashedWheelTimer();

    /**
     * When a new client connects and is registered, it is stored in his Map. They are stored in an
     * [IP Address, ClientHandler] format.
     */
    private final Map<String, ClientHandler> clients = Collections.synchronizedMap(
            new HashMap<String, ClientHandler>());

    /**
     * The same as the the client Map, except instead of in an using a key as the IP Address, it uses
     * the channel that the admin is connected on.
     */
    private final Map<Channel, AdminHandler> admins = Collections.synchronizedMap(new HashMap<Channel, AdminHandler>());

    /**
     * The Map to store to commands in. The [String, Command] refers to the [Name, What to execute]
     * format, making it very easy to use.
     */
    private final Map<String, Command> handlers = new HashMap<String, Command>();

    /**
     * Authenticates a user to the server.
     *
     * @param user     The username provided.
     * @param password The password provided.
     * @return If the password matches the correct password for the username.
     */
    public static boolean auth(String user, String password) {
        return password.equals(accounts.get(user));
    }

    /**
     * Gets the URL of the newest client.jar
     *
     * @return the url.
     */
    public static String getUpdateUrl() {
        return Configuration.UPDATE;
    }

    /**
     * The main function of this class, all it does is add the admin account, and then create
     * a new instance.
     *
     * @param args Unused.
     */
    public static void main(String[] args) {
        accounts.put(Configuration.ADMIN_USERNAME, Configuration.ADMIN_PASSWORD);
        new Server();
    }

    /**
     * Gets the IP Address of the connection on the specific Channel.
     *
     * @param channel The connection
     * @return The remote ip address.
     */
    public static String getRemoteIp(Channel channel) {
        String str = channel.getRemoteAddress().toString();
        return str.substring(1, str.indexOf(":"));
    }

    /**
     * New instance of the Server class.
     */
    public Server() {
        for (Class<?> handler : HANDLERS) {
            try {
                this.handlers.put(handler.getAnnotation(Name.class).value(),
                                  handler.asSubclass(Command.class).newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("decoder", new PacketDecoder());
                pipeline.addLast("encoder", new PacketEncoder());
                pipeline.addLast("timeout",
                                 new ReadTimeoutHandler(Server.this.timer, Configuration.PING_RATE_SECS * 2));
                pipeline.addLast("handler", new ClientHandler(Server.this));
                return pipeline;
            }
        });
        bootstrap.bind(new InetSocketAddress(Configuration.PORT));
    }

    /**
     * Registered the client with the server, and adds them into the Map.
     *
     * @param host   The IP Address of the client
     * @param client The instance of the client handler.
     */
    public void registerClient(String host, ClientHandler client) {
        System.out.println("registered " + host);
        for (AdminHandler h : getAdminHandlers()) {
            h.message("REGISTER " + host);
        }
        this.timer.newTimeout(new PingTask(client.getChannel()), Configuration.PING_RATE_SECS, TimeUnit.SECONDS);
        this.clients.put(host, client);
    }

    /**
     * Unregisters the client from the server.
     *
     * @param host The host to unregister.
     */
    public void unregisterClient(String host) {
        System.out.println("unregistered " + host);
        for (AdminHandler h : getAdminHandlers()) {
            h.message("UNREGISTER " + host);
        }
        this.clients.remove(host);
    }

    /**
     * Register a client as an admin.
     *
     * @param channel The channel through which the client is connected.
     * @param admin   The instance of the AdminHandler.
     * @param display The display name of the Administrator.
     */
    public void registerAdmin(Channel channel, AdminHandler admin, String display) {
        this.admins.put(channel, admin);
        this.timer.newTimeout(new PingTask(admin.getChannel()), Configuration.PING_RATE_SECS, TimeUnit.SECONDS);
        System.out.println("registered admin " + channel);
        for (AdminHandler h : getAdminHandlers())
            h.message("AUTH " + display + "@" + channel.getLocalAddress() + " is now an Administrator.");
    }

    /**
     * Remove the admin from the server.
     *
     * @param channel The channel that the admin was connected through.
     * @param display The display name of the admin.
     */
    public void unregisterAdmin(Channel channel, String display) {
        this.admins.remove(channel);
        System.out.println("unregistered admin " + channel);
        for (AdminHandler h : getAdminHandlers())
            h.message("UNAUTH " + display + "@" + channel.getLocalAddress() + " has disconnected.");
    }

    /**
     * Executes a command on the server.
     *
     * @param command The command to execute.
     * @param handler The admin that issued the command.
     * @param targets The clients affected by the command.
     * @param args    The parameters passed through the command.
     * @return True all the time...
     */
    public boolean execute(String command, AdminHandler handler, Set<ClientHandler> targets, String[] args) {
        Command h = this.handlers.get(command);
        if (h != null) {
            h.execute(this, handler, targets, args);
            return true;
        } else {
            h = this.handlers.get("send");
            String newArgs = command + " " + Arrays.toString(args);
            args = newArgs.split(" ");
            h.execute(this, handler, targets, args);
            return true;
        }
    }

    /**
     * If a command is executed with the wrong parameters, it will send call this function to
     * send it to the admin who issued the command.
     *
     * @param command The command to find the syntax for.
     * @return The syntax of the command.
     */
    public Usage getUsage(String command) {
        Command h = this.handlers.get(command);
        if (h != null) {
            return h.getClass().getAnnotation(Usage.class);
        }
        return null;
    }

    /**
     * Gets all of the IP Addresses of connected clients.
     *
     * @return All of the ips.
     */
    public Set<String> getClientHosts() {
        return this.clients.keySet();
    }

    /**
     * Find a client based on their ip address.
     *
     * @param host The IP Address to find.
     * @return The client.
     */
    public ClientHandler getClientHandler(String host) {
        return this.clients.get(host);
    }

    /**
     * Gets a set of clients based on a regex.
     *
     * @param host The regex of the IPs to find.
     * @return The collection of clients.
     */
    public Set<ClientHandler> getClientHandlers(Pattern host) {
        HashSet<ClientHandler> result = new HashSet<ClientHandler>();
        for (Map.Entry<String, ClientHandler> e : this.clients.entrySet()) {
            if (host.matcher(e.getKey()).matches()) {
                result.add(e.getValue());
            }
        }
        return result;
    }

    /**
     * Gets all of the clients connected.
     *
     * @return All of the connected clients.
     */
    public Set<ClientHandler> getClientHandlers() {
        HashSet<ClientHandler> result = new HashSet<ClientHandler>();
        result.addAll(this.clients.values());
        return result;
    }

    /**
     * Gets all of the Admins connected.
     *
     * @return The admins.
     */
    public Collection<AdminHandler> getAdminHandlers() {
        return this.admins.values();
    }

    /**
     * This sends a <code>#PING_PACKET</code> to every online client every X seconds.
     */
    private class PingTask implements org.jboss.netty.util.TimerTask {
        private final Channel chc;

        public PingTask(Channel chc) {
            this.chc = chc;
        }

        @Override
        public void run(Timeout timeout) {
            if ((!timeout.isCancelled()) && (this.chc.isOpen())) {
                this.chc.write(Server.PING_PACKET);
                Server.this.timer.newTimeout(this, Configuration.PING_RATE_SECS, TimeUnit.SECONDS);
            }
        }
    }

}
