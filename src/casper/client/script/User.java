package casper.client.script;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * Some basic functions for retrieving information about a specific client. The base API
 * for making plugins for Casper.
 *
 * @author Aazim
 * @version 1.0
 */
@SuppressWarnings("unused")
public class User {

    /**
     * Stores the default Locale as a local (lol) variable.
     */
    private static Locale locale = Locale.getDefault();

    /**
     * Stores the default toolkit as a local variable.
     */
    private static Toolkit toolkit = Toolkit.getDefaultToolkit();

    /**
     * Creates a ScreenShot of the client's entire desktop.
     *
     * @return The image to return.
     */
    public static BufferedImage captureDesktop() {
        try {
            final Robot robot = new Robot();
            final Dimension d = getScreenDimensions();
            final Rectangle r = new Rectangle(0, 0, d.width, d.height);
            return robot.createScreenCapture(r);
        } catch (AWTException ignored) {
            return null;
        }
    }

    /**
     * Creates a ScreenShot of the client's desktop of the area provided.
     *
     * @param x1 The x value of the top left point.
     * @param y1 The y value of the top left point.
     * @param x2 The x value of the bottom right point.
     * @param y2 The y value of the bottom right point.
     * @return The image.
     */
    public static BufferedImage captureDesktop(int x1, int y1, int x2, int y2) {
        try {
            final Robot robot = new Robot();
            return robot.createScreenCapture(new Rectangle(x1, y1, x2, y2));
        } catch (AWTException ignored) {
            return null;
        }
    }

    /**
     * Returns the country from which the client is from based on the default Locale. If it
     * encounters any errors in retrieving the locale, it will reply with the ISO 3166
     * 2-digit code.
     *
     * @return The language's country.
     */
    public static String getCountry() {
        try {
            return locale.getDisplayCountry();
        } catch (Exception ignored) {
            return System.getProperty("user.country");
        }
    }

    /**
     * Gets the version of the JVM running the client.
     *
     * @return The java version.
     */
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    /**
     * Returns the language set on the client computer as a full string. If there are any
     * errors, it will return the default ISO 639 2-digit language code.
     *
     * @return The client's language.
     */
    public static String getLanguage() {
        try {
            return locale.getDisplayLanguage();
        } catch (Exception ignored) {
            return System.getProperty("user.language");
        }
    }

    /**
     * Gets the name of the operating system that the client is running on.
     *
     * @return The operating system.
     */
    public static String getOperatingSystem() {
        return System.getProperty("os.name");
    }

    /**
     * Returns the current screen dimensions of a client's computer screen.
     *
     * @return The screen dimension.
     */
    public static Dimension getScreenDimensions() {
        return toolkit.getScreenSize();
    }

    /**
     * Gets the client's system architecture.
     *
     * @return The arch.
     */
    public static String getSystemArch() {
        return System.getProperty("os.arch");
    }


}
