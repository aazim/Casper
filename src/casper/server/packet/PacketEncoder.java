package casper.server.packet;

import casper.net.Downstream;
import casper.net.Packet;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

public class PacketEncoder extends SimpleChannelDownstreamHandler {

    public void writeRequested(ChannelHandlerContext ctx, MessageEvent evt) {
        Packet message = (Packet) evt.getMessage();
        int opCode = message.getOpcode();
        int length = 0;
        for (Downstream d : Downstream.values()) {
            if (d.getOpcode() == opCode) {
                length = d.length();
            }
        }
        int pos = message.getPosition();
        ChannelBuffer buff;
        if (length >= 0) {
            buff = ChannelBuffers.buffer(pos + 1);
            buff.writeByte(opCode);
            buff.writeBytes(message.getBuffer(), 0, pos);
        } else if (length == -1) {
            buff = ChannelBuffers.buffer(pos + 2);
            buff.writeByte(opCode);
            buff.writeByte(pos);
            buff.writeBytes(message.getBuffer(), 0, pos);
        } else if (length == -2) {
            buff = ChannelBuffers.buffer(pos + 4);
            buff.writeByte(opCode);
            buff.writeMedium(pos);
            buff.writeBytes(message.getBuffer(), 0, pos);
        } else {
            throw new IllegalStateException("invalid packet length!");
        }
        Channels.write(ctx, evt.getFuture(), buff);
    }

}
