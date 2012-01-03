package casper.server.commands;

import casper.server.handlers.AdminHandler;
import casper.server.handlers.ClientHandler;
import casper.server.Server;

import java.util.Set;

public abstract interface Command {

    /**
     * Executes the command on the server.
     *
     * @param paramServer        The server from which the command is originating.
     * @param paramAdminHandler  The admin handler of all of the online admins.
     * @param paramSet           The set of all of the online clients.
     * @param paramArrayOfString The parameters of the command.
     */
    public abstract void execute(Server paramServer, AdminHandler paramAdminHandler, Set<ClientHandler> paramSet,
                                 String[] paramArrayOfString);

}
