package casper.server.packet;

import casper.net.Packet;
import casper.net.Upstream;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 *
 */
public class PacketDecoder extends FrameDecoder {
    @Override
    protected Object decode(ChannelHandlerContext chc, Channel channel, ChannelBuffer buf) {
        if (buf.readableBytes() <= 0) {
            return null;
        }
        buf.markReaderIndex();
        int opcode = buf.readUnsignedByte();
        int length = 0;
        for (Upstream u : Upstream.values()) {
            if (u.getOpcode() == opcode) {
                length = u.length();
            }
        }
        if (length == -1) {
            if (buf.readableBytes() < 1) {
                buf.resetReaderIndex();
                return null;
            }
            length = buf.readUnsignedByte();
        } else if (length == -2) {
            if (buf.readableBytes() < 3) {
                buf.resetReaderIndex();
                return null;
            }
            length = buf.readUnsignedMedium();
        }
        if (buf.readableBytes() < length) {
            buf.resetReaderIndex();
            return null;
        }
        byte[] data = new byte[length];

        if (length > 0) {
            buf.readBytes(data);
        }
        return new Packet(opcode, data);
    }
}
