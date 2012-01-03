package casper.net;

import casper.net.json.JSONArray;

@SuppressWarnings("unused")
public class Packet {

    private int opcode;
    private byte[] data;
    private int ptr = 0;

    public Packet(int opcode) {
        this(opcode, 1024);
    }

    public Packet(int opcode, int initialSize) {
        this.data = new byte[initialSize];
        this.opcode = opcode;
    }

    public Packet(int opcode, byte[] buffer) {
        this.opcode = opcode;
        this.data = buffer;
    }

    public void writeBytes(byte[] b) {
        int len = b.length;
        if (this.ptr >= this.data.length - len + 1) {
            expand(len);
        }
        for (int i = 0; i < len; i++)
            this.data[(this.ptr++)] = b[i];
    }

    public void writeByte(int b) {
        if (this.ptr >= this.data.length) {
            expand(1);
        }
        this.data[(this.ptr++)] = (byte) b;
    }

    public void writeShort(int s) {
        if (this.ptr >= this.data.length - 1) {
            expand(2);
        }
        this.data[(this.ptr++)] = (byte) (s >> 8);
        this.data[(this.ptr++)] = (byte) s;
    }

    public void writeInt(int i) {
        if (this.ptr >= this.data.length - 3) {
            expand(4);
        }
        this.data[(this.ptr++)] = (byte) (i >> 24);
        this.data[(this.ptr++)] = (byte) (i >> 16);
        this.data[(this.ptr++)] = (byte) (i >> 8);
        this.data[(this.ptr++)] = (byte) i;
    }

    public void writeLong(long l) {
        if (this.ptr >= this.data.length - 7) {
            expand(8);
        }
        writeInt((int) (l >> 32));
        writeInt((int) l);
    }

    public void writeCString(String s) {
        writeBytes(s.getBytes());
        writeByte(0);
    }

    public void writeChunk(byte[] data) {
        writeInt(data.length);
        writeBytes(data);
    }

    public int readByte() {
        return this.data[(this.ptr++)];
    }

    public int readShort() {
        return (short) (this.data[(this.ptr++)] << 8 | this.data[(this.ptr++)] & 0xFF);
    }

    public int readInt() {
        return this.data[(this.ptr++)] << 24 | (this.data[(this.ptr++)] & 0xFF) << 16 | (this.data[(this.ptr++)] & 0xFF) << 8 | this.data[(this.ptr++)] & 0xFF;
    }

    public long readLong() {
        return ((0xFFFFFFFF & readInt()) << 32) + (0xFFFFFFFF & readInt());
    }

    public JSONArray readJSONArray() {
        JSONArray jsonArray = new JSONArray();
        while (this.remaining() > 0) {
            jsonArray.put(this.readCString().replaceAll("[,\\[\\]]", ""));
        }
        return jsonArray;
    }

    public String readCString() {
        int i = this.ptr;
        while (this.data[(this.ptr++)] != 0) ;
        return new String(this.data, i, this.ptr - i - 1);
    }

    public byte[] readChunk() {
        int len = readInt();
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            data[i] = data[(this.ptr++)];
        }
        return data;
    }

    public void reset() {
        this.ptr = 0;
    }

    public void skip(int len) {
        this.ptr += len;
    }

    public void seek(int pos) {
        this.ptr = pos;
    }

    public int remaining() {
        return this.data.length - this.ptr;
    }

    public int getOpcode() {
        return this.opcode;
    }

    public int getPosition() {
        return this.ptr;
    }

    public byte[] getBuffer() {
        return this.data;
    }

    public byte[] toByteArray() {
        byte[] out = new byte[this.ptr + 1];
        out[0] = (byte) this.opcode;
        System.arraycopy(this.data, 0, out, 1, this.ptr);
        return out;
    }

    private void expand(int amount) {
        byte[] orig = this.data;
        int len = orig.length;
        if (this.ptr > len) {
            amount += len - this.ptr;
        }
        this.data = new byte[len + amount];
        System.arraycopy(orig, 0, this.data, 0, orig.length);
    }

}