package casper.admin;

import casper.admin.ui.AdminGUI;
import casper.admin.ui.ServersGUI;
import casper.net.Connection;
import casper.net.Downstream;
import casper.net.Packet;
import casper.net.Upstream;

import javax.swing.*;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class Admin implements Runnable, MessageListener {

    /**
     * The server array.
     */
    public static String[] SERVERS = Configuration.SERVERS;

    /**
     * The generic PONG reply.
     */
    private static final byte[] PONG_DATA = new Packet(0, 0).toByteArray();

    /**
     * Create a connection array with a length of the # of servers.
     */
    private Connection[] connections = new Connection[SERVERS.length];

    /**
     * Create the "last ping" array for each of the servers.
     */
    private long[] pings = new long[SERVERS.length];

    /**
     * Stores the initial connecting packet as a byte array.
     */
    private byte[] connect;

    /**
     * Stores the initial authenticating packet as a byte array.
     */
    private byte[] auth;

    /**
     * Instance of MessageListener to be passed to the GUI
     */
    private MessageListener listener;

    /**
     * Is the client running?
     */
    private boolean running;

    /**
     * Set the current opcode to -1 so it initializes connections to servers.
     */
    private int opcode = -1;

    /**
     * Stores the length of the packet.
     */
    private int length;

    /**
     * Creates the byte buffer
     */
    private byte[] buffer;

    /**
     * Pointer
     */
    private int ptr;

    /**
     * Upstream lock.
     */
    private final Object UPSTREAM_LOCK = new Object();

    /**
     * Packet queue, so the bot does not get flooded.
     */
    private final Queue<Packet> packetQueue = new LinkedList<Packet>();

    /**
     * Queue for outputting.
     */
    private final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(5);

    /**
     * The main code.
     *
     * @param args Unused.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.jtattoo.plaf.hifi.HiFiLookAndFeel");
        } catch (Exception ignored) {
        }
        ServersGUI serversGUI = new ServersGUI();
        while (serversGUI.isVisible()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Admin admin = new Admin(Configuration.ADMIN_DISPLAY, Configuration.ADMIN_USERNAME,
                                Configuration.ADMIN_PASSWORD);
        AdminGUI frame = new AdminGUI(admin);
        frame.setVisible(true);
        admin.setListener(frame);
        new Thread(admin).start();
        admin.scan();
        frame.dispose();
        admin.stop();
    }

    public static void setServers(String[] servers) {
        SERVERS = servers;
    }

    /**
     * Creates a new instance of the Admin client.
     *
     * @param display  The display name to show on the server.
     * @param username The username to login with.
     * @param password The password to login with.
     */
    public Admin(String display, String username, String password) {
        Packet packet = new Packet(1, 2);
        packet.writeShort(0);
        this.connect = packet.toByteArray();
        packet = new Packet(2, display.length() + username.length() + password.length() + 2);
        packet.writeCString(display);
        packet.writeCString(username);
        packet.writeCString(password);
        byte[] buff = packet.getBuffer();
        this.auth = new byte[buff.length + 2];
        this.auth[0] = (byte) packet.getOpcode();
        this.auth[1] = (byte) buff.length;
        System.arraycopy(buff, 0, this.auth, 2, buff.length);
    }

    /**
     * Sets the listener for the GUI.
     *
     * @param listener The MessageListener.
     */
    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public void scan() {
        while (true) {
            String line;
            try {
                line = this.queue.take().trim();
            } catch (InterruptedException e) {
                break;
            }

            if (line.equals("exit")) {
                break;
            }
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("/")) {
                Packet packet = new Packet(4);
                packet.writeCString("");
                int space = line.indexOf(" ");
                if (space == -1) {
                    packet.writeCString(line.substring(1));
                    packet.writeByte(0);
                } else {
                    String[] tokens = line.substring(space + 1).split(" ");
                    packet.writeCString(line.substring(1, space));
                    packet.writeByte(tokens.length);
                    for (String token : tokens) {
                        System.out.print(token + ", ");
                        packet.writeCString(token);
                    }
                }
                enqueue(packet);
            } else {
                Packet packet = new Packet(3);
                packet.writeCString(line);
                enqueue(packet);
            }
        }
    }

    public void messageReceived(String server, String message) {
        this.queue.add(message);
    }

    public void enqueue(Packet packet) {
        synchronized (this.UPSTREAM_LOCK) {
            this.packetQueue.add(packet);
        }
    }

    public void stop() {
        this.running = false;
        for (Connection c : this.connections)
            if (c != null)
                c.disconnect();
    }

    public void run() {
        this.running = true;
        while (this.running) {
            for (int i = 0; i < this.connections.length; i++)
                try {
                    if (this.connections[i] == null) {
                        String server = SERVERS[i];
                        String[] tokens = server.split(":");
                        this.connections[i] = new Connection(new Socket(tokens[0], Integer.parseInt(tokens[1])));
                        this.connections[i].write(this.connect, 0, this.connect.length);
                        this.connections[i].write(this.auth, 0, this.auth.length);
                        this.pings[i] = System.currentTimeMillis();
                    }
                } catch (IOException ex) {
                    this.listener.messageReceived(SERVERS[i], ex.getMessage());
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException ignored) {
                    }
                }
            handleDownstream();
            handleUpstream();
        }
    }

    /**
     * Returns the length of the packet assigned to the provided opcode.
     *
     * @param opCode The opcode of the packet you want the length for.
     * @return len     The length of the packet.
     */
    private int getUpstreamLength(int opCode) {
        int len = 0;
        for (Upstream u : Upstream.values()) {
            if (u.getOpcode() == opCode) {
                len = u.length();
            }
        }
        return len;
    }

    private int getDownstreamLength(int opCode) {
        int len = 0;
        for (Downstream u : Downstream.values()) {
            if (u.getOpcode() == opCode) {
                len = u.length();
            }
        }
        return len;
    }

    private void handleDownstream() {
        for (int i = 0; i < this.connections.length; i++)
            try {
                if (this.connections[i] == null) {
                    continue;
                }
                if (System.currentTimeMillis() - this.pings[i] > 120000L) {
                    throw new IOException("timeout");
                }

                int available = this.connections[i].available();

                if (available == 0) {
                    continue;
                }
                if (this.opcode == -1) {
                    this.opcode = (this.connections[i].read() & 0xFF);
                    this.length = getDownstreamLength(this.opcode);
                }

                if (this.length == -1) {
                    if (available > 0) {
                        this.length = (this.connections[i].read() & 0xFF);
                        available--;
                    } else {
                        continue;
                    }
                } else if (this.length == -2) {
                    if (available > 2) {
                        this.length = ((this.connections[i].read() & 0xFF) << 16 | (this.connections[i].read() & 0xFF) << 8 | this.connections[i].read() & 0xFF);
                        available -= 3;
                    } else {
                        continue;
                    }
                }

                if (this.buffer == null) {
                    this.buffer = new byte[this.length];
                }

                if (this.length > 0) {
                    int len = available <= this.length ? available : this.length;
                    this.connections[i].read(this.ptr, len, this.buffer);
                    this.length -= len;
                    this.ptr += len - 1;
                }

                if (this.length > 0) {
                    continue;
                }
                Packet packet = new Packet(this.opcode, this.buffer);

                if (this.opcode == 0) {
                    this.pings[i] = System.currentTimeMillis();
                    this.connections[i].write(PONG_DATA, 0, PONG_DATA.length);
                } else if (this.opcode == 2) {
                    String message = packet.readCString();
                    this.listener.messageReceived(SERVERS[i], message);
                } else {
                    this.listener.messageReceived(SERVERS[i], "uop " + this.opcode);
                }

                this.opcode = -1;
                this.ptr = 0;
                this.buffer = null;
            } catch (IOException ex) {
                ex.printStackTrace();
                if (this.connections[i] != null) {
                    this.connections[i].disconnect();
                }
                this.connections[i] = null;
            }
    }

    private void handleUpstream() {
        synchronized (this.UPSTREAM_LOCK) {
            Iterator it = this.packetQueue.iterator();
            while (it.hasNext()) {
                Packet p = (Packet) it.next();
                int op = p.getOpcode();
                int len = getUpstreamLength(op);
                int pos = p.getPosition();
                byte[] data;
                if (len > 0) {
                    data = new byte[1 + len];
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
                    data[1] = (byte) (pos >> 16 & 0xFF);
                    data[2] = (byte) (pos >> 8 & 0xFF);
                    data[3] = (byte) (pos & 0xFF);
                    System.arraycopy(p.getBuffer(), 0, data, 3, pos);
                } else {
                    throw new IllegalStateException("invalid packet length!");
                }
                for (int i = 0; i < this.connections.length; i++) {
                    if (this.connections[i] == null) continue;
                    try {
                        this.connections[i].write(data, 0, data.length);
                    } catch (IOException ex) {
                        this.listener.messageReceived(SERVERS[i], ex.toString() + " | resetting...");
                        this.connections[i].disconnect();
                        this.connections[i] = null;
                    }
                }
                it.remove();
            }
        }
    }
}
