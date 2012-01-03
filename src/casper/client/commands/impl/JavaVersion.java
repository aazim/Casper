package casper.client.commands.impl;

import casper.client.Client;
import casper.client.commands.Action;
import casper.client.commands.Command;
import casper.client.script.User;
import casper.net.json.JSONException;
import casper.net.Packet;

/**
 * Simply prints the version of Java to the requesting admin.
 */
@Command("java")
public class JavaVersion implements Action {

    public void execute(Client client, Packet p) throws JSONException {
        client.print(User.getJavaVersion());
    }
}
