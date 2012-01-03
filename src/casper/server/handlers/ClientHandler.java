package casper.server.handlers;

import casper.net.Packet;
import casper.server.Configuration;
import casper.server.Server;
import org.jboss.netty.channel.*;

import java.io.IOException;

public class ClientHandler extends SimpleChannelUpstreamHandler {

    private Server context;
    private Channel channel;
    private boolean registered;

    public ClientHandler(Server server) {
        this.context = server;
        this.registered = false;
    }

    public void write(Packet packet) {
        this.channel.write(packet);
    }

    public Channel getChannel() {
        return this.channel;
    }

    public void messageReceived(ChannelHandlerContext chc, MessageEvent e) {
        Packet packet = (Packet) e.getMessage();
        int opcode = packet.getOpcode();

        if (opcode == 0) {
            return;
        }

        if (opcode == 1) {
            int v = packet.readShort();
            this.channel = chc.getChannel();
            if (v == 0) {
                chc.getPipeline().remove(this);
                chc.getPipeline().addLast("handler", new AdminHandler(this.context));
            } else if (v == Configuration.VERSION) {
                String ip = Server.getRemoteIp(this.channel);
                if (this.context.getClientHandler(ip) == null) {
                    this.context.registerClient(ip, this);
                    this.registered = true;
                } else {
                    this.channel.close();
                }
            } else {
                Packet p = new Packet(1);
                p.writeCString(Server.getUpdateUrl());
                this.channel.write(p).addListener(ChannelFutureListener.CLOSE);
            }
        } else if (!this.registered) {
            this.channel.close();
            return;
        }
        switch (opcode) {
            case 3:
                String message = "CLIENT_MSG [" + Server.getRemoteIp(this.channel) + "] " + packet.readCString();
                Packet p = new Packet(2, message.length() + 1);
                p.writeCString(message);
                for (AdminHandler h : this.context.getAdminHandlers())
                    h.write(p);
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void exceptionCaught(ChannelHandlerContext chc, ExceptionEvent evt) {
        Throwable t = evt.getCause();
        if (!(t instanceof IOException))
            t.printStackTrace();
    }

    public void channelClosed(ChannelHandlerContext chc, ChannelStateEvent e) {
        if (this.registered) {
            this.context.unregisterClient(Server.getRemoteIp(this.channel));
            this.registered = false;
        }
    }

}

