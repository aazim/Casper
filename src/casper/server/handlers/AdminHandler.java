package casper.server.handlers;

import casper.net.Packet;
import casper.net.Upstream;
import casper.server.Server;
import casper.server.commands.Usage;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The AdminHandler class handles everything pertaining to the clients that have
 * authenticated themselves as administrators.
 *
 * @author Aazim
 */

public class AdminHandler extends SimpleChannelUpstreamHandler {

    /**
     * Stores the instance of the Server class.
     */
    private Server server;

    /**
     * The channel of which the information is sent.
     */
    private Channel channel;

    /**
     * Stores whether or not the admin is authenticated.
     */
    private boolean authenticated;

    /**
     * Stores the display name of the admin.
     */
    private String display;

    /**
     * Set the required variables for this class.
     *
     * @param server The instance of the Server class.
     */
    public AdminHandler(Server server) {
        this.server = server;
        this.authenticated = false;
    }

    /**
     * Sends data through the channel as a message packet.
     *
     * @param message The message to send.
     */
    public void message(String message) {
        Packet p = new Packet(2, message.length() + 1);
        p.writeCString(message);
        this.channel.write(p);
    }

    /**
     * Sends data through the channel.
     *
     * @param packet The data to send.
     */
    public void write(Packet packet) {
        this.channel.write(packet);
    }

    /**
     * Gets the channel.
     *
     * @return The channel of the admin.
     */
    public Channel getChannel() {
        return this.channel;
    }

    /**
     * Handles any message sent to the server by an authenticated client.
     *
     * @param chc The handler of the channel.
     * @param e   The MessageEvent from the received message.
     */
    @Override
    public void messageReceived(ChannelHandlerContext chc, MessageEvent e) {
        Packet packet = (Packet) e.getMessage();
        Upstream upstream = Upstream.values()[packet.getOpcode()];
        switch (upstream) {
            case PONG:
                return;
            case UPDATE:
                return;
            case AUTH: {
                this.channel = chc.getChannel();
                this.display = packet.readCString();
                String username = packet.readCString();
                String password = packet.readCString();
                if (Server.auth(username, password)) {
                    this.server.registerAdmin(this.channel, this, display);
                    this.authenticated = true;
                } else {
                    System.out.println("Failed login attempt.");
                    this.channel.close();
                }
                return;
            }
            case MESSAGE: {
                String message = new StringBuilder("ADMIN_MSG [").append(display).append("] ")
                        .append(packet.readCString()).toString();
                Packet p = new Packet(2, message.length() + 1);
                p.writeCString(message);
                for (AdminHandler ah : this.server.getAdminHandlers()) {
                    ah.write(p);
                }
                return;
            }
            case COMMAND:
                String regex = packet.readCString();
                String command = packet.readCString();
                int argc = packet.readByte();
                String[] argp = new String[argc];
                for (int i = 0; i < argc; i++)
                    argp[i] = packet.readCString();
                Set<ClientHandler> targets;
                if (regex.isEmpty()) {
                    targets = this.server.getClientHandlers();
                } else {
                    targets = this.server.getClientHandlers(Pattern.compile(regex));
                }
                try {
                    if (this.server.execute(command, this, targets, argp)) {
                        String message = new StringBuilder("COMMAND [COMMAND] ").append(display).append("@")
                                .append(Server.getRemoteIp(this.channel)).append(" issued: ").append(command)
                                .append(" ").append(Arrays.toString(argp)).append(" -> ")
                                .append(regex.isEmpty() ? "*" : regex).toString();
                        Packet p = new Packet(2, message.length() + 1);
                        p.writeCString(message);
                        for (AdminHandler ah : this.server.getAdminHandlers()) {
                            ah.write(p);
                        }
                    }
                } catch (Exception ex) {
                    String message = ex.getMessage();
                    Usage usage = this.server.getUsage(command);
                    if (usage != null) {
                        message = new StringBuilder().append(message).append("\n\t\tUsage for ").append(command)
                                .append(": ").append(usage.value()).toString();
                    }
                    message(message);
                }
                return;
        }
        System.out.println(new StringBuilder().append("[ADMIN Handler]: uop ").append(packet.getOpcode()).append(" ")
                                   .append(packet.readCString()).toString());
    }

    /**
     * Throws exceptions from this class, without stopping the server. This will not
     * show up any IOException errors.
     *
     * @param chc The channel handler.
     * @param evt The exception that is to be thrown.
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void exceptionCaught(ChannelHandlerContext chc, ExceptionEvent evt) {
        Throwable t = evt.getCause();
        if (!(t instanceof IOException))
            t.printStackTrace();
    }

    /**
     * Removes the admin authentication from the server if the channel of the admin closes.
     *
     * @param chc The channel handler.
     * @param e   The Event caused by the change in state of the channel.
     */
    public void channelClosed(ChannelHandlerContext chc, ChannelStateEvent e) {
        if (this.authenticated) {
            this.server.unregisterAdmin(this.channel, display);
            this.authenticated = false;
        }
    }

}