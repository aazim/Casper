package casper.server.commands.impl;

import casper.net.Packet;
import casper.server.handlers.AdminHandler;
import casper.server.handlers.ClientHandler;
import casper.server.Server;
import casper.server.commands.Command;
import casper.server.commands.Name;
import casper.server.commands.Usage;

import java.util.Collection;
import java.util.Set;

@Name("alist")
@Usage("/alist")
public class AdminList implements Command {
    @Override
    public void execute(Server server, AdminHandler handler, Set<ClientHandler> targets, String... args) {
        Collection<AdminHandler> hosts = server.getAdminHandlers();
        System.out.println("Listing admins.");
        StringBuilder b = new StringBuilder("Admins:\n");
        for (AdminHandler host : hosts) {
            b.append(host.getChannel()).append('\n');
        }
        b.append("(total: ").append(hosts.size()).append(")");
        Packet p = new Packet(2);
        p.writeCString(b.toString());
        handler.write(p);
    }
}