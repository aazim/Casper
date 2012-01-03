package casper.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Connection is the class used for creating socket connections by each of the following:
 * <ul>
 * <li>The Server.</li>
 * <li>The Admin Panel.</li>
 * <li>The Client.</li>
 * </ul>
 */
public class Connection {

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public Connection(Socket socket) throws IOException {
        socket.setSoTimeout(60000);
        socket.setTcpNoDelay(true);
        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    public int read() throws IOException {
        return this.in.read();
    }

    public void read(int offSet, int length, byte[] data) throws IOException {
        if (this.in.read(data, offSet, length) <= 0)
            throw new IOException("EOF");
    }

    public void write(byte[] data, int offSet, int length) throws IOException {
        this.out.write(data, offSet, length);
    }

    public int available() throws IOException {
        return this.in.available();
    }

    public void disconnect() {
        try {
            if (this.in != null) this.in.close();
            if (this.out != null) this.out.close();
            if (this.socket != null) this.socket.close();
        } catch (IOException ignored) {
        }
    }

}
