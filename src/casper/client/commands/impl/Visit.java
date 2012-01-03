package casper.client.commands.impl;

import casper.client.Client;
import casper.client.commands.Action;
import casper.client.commands.Command;
import casper.client.script.Browser;
import casper.net.Packet;
import casper.net.json.JSONArray;
import casper.net.json.JSONException;

import java.awt.*;
import java.net.URI;
import java.net.URL;

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

        boolean silent = false;

        for (int i = 0; i < params.length(); i++) {
            if (params.getString(i).equals("-silent")) {
                silent = true;
            }
        }

        try {

            String url = params.getString(0);

            if (!silent) {
                Desktop desktop = Desktop.getDesktop();
                URI uri = new URI(url);
                desktop.browse(uri);
            } else {
                Browser browser = new Browser(new URL(url));
                browser.readFully();
            }

        } catch (Exception ignored) {
        }

    }

}
