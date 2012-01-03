package casper.net;

public enum Upstream {

    PONG(0, 0),
    UPDATE(1, 2),
    AUTH(2, -1),
    MESSAGE(3, -1),
    COMMAND(4, -1),
    CLIENT_MESSAGE(5, -1);

    private int opcode;
    private int length;

    private Upstream(int opcode, int length) {
        this.opcode = opcode;
        this.length = length;
    }

    public int getOpcode() {
        return opcode;
    }

    public int length() {
        return length;
    }

    public static int length(int opcode) {
        Upstream u = Upstream.values()[opcode];
        return u.length();
    }

}
