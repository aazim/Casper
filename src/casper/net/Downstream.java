package casper.net;

public enum Downstream {

    PING(0, 0),
    UPDATE(1, -1),
    MESSAGE(2, -2),
    COMMAND(3, -2);

    private final int opcode;
    private int length;

    private Downstream(int opcode, int length) {
        this.opcode = opcode;
        this.length = length;
    }

    public final int getOpcode() {
        return opcode;
    }

    public int length() {
        return length;
    }

    public static int length(int opcode) {
        Downstream d = Downstream.values()[opcode];
        return d.length();
    }

}
