package casper.client.commands;

import casper.client.Client;
import casper.net.json.JSONException;
import casper.net.Packet;

public abstract interface Action {

    /**
     * Executes the actions assigned to a specific command.
     *
     * @param client The client that issued the command.
     * @param p      The packet that the client read.
     * @throws JSONException For some reason JSON fucks up?
     */
    public abstract void execute(Client client, Packet p) throws JSONException;

}