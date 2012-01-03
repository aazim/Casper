package casper.server.commands.impl;

import casper.net.Downstream;
import casper.net.Packet;
import casper.server.handlers.AdminHandler;
import casper.server.handlers.ClientHandler;
import casper.server.Server;
import casper.server.commands.Command;
import casper.server.commands.Name;
import casper.server.commands.Usage;

import java.util.Set;

/**
 * This class allows the server to send commands to the clients.&nbsp;It will create a packet with all of the
 * required bits of information, then push the packet out through the connection.&nbsp;It is up to the maker
 * of the plugin to handle the data on the client side.
 *
 * @author Aazim
 * @version 1.1
 */
@Name("send")
@Usage("<param1> <param2> <param...>")
public class SendCommand implements Command {

    @Override
    public void execute(Server server, AdminHandler handler, Set<ClientHandler> targets, String... args) {

        final String command = args[0];

        int length = command.length();

        for (String s : args) {
            if (s.equals(command)) {
                continue;
            }
            length += s.length() + 1;
        }

        Packet p = new Packet(Downstream.COMMAND.getOpcode(), length);

        for (String param : args) {
            p.writeCString(param);
        }

        for (ClientHandler c : targets) c.write(p);

    }
}