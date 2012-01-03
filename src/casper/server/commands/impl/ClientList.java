package casper.server.commands.impl;

import casper.net.Packet;
import casper.server.handlers.AdminHandler;
import casper.server.handlers.ClientHandler;
import casper.server.Server;
import casper.server.commands.Command;
import casper.server.commands.Name;
import casper.server.commands.Usage;

import java.util.Set;

@Name("list")
@Usage("/list")
public class ClientList implements Command {
    @Override
    public void execute(Server server, AdminHandler handler, Set<ClientHandler> targets, String... args) {
        Set<String> hosts = server.getClientHosts();
        StringBuilder b = new StringBuilder("Listing clients:\n");
        for (String host : hosts) {
            b.append(host).append('\n');
        }
        b.append("(total: ").append(hosts.size()).append(")");
        Packet p = new Packet(2);
        p.writeCString(b.toString());
        handler.write(p);
    }
}
