package casper.server;

public class Configuration {

    /**
     * The port to listen on.
     */
    public static final int PORT = 8817;

    /**
     * The current version of the server & the clients.&nbsp;Required to be changed if you want
     * the clients to update to a newer version.
     */
    public static final int VERSION = 1;

    /**
     * The URL of the always up to date client file.&nbsp;This will tell all of the clients with
     * a lower version the location to update from.
     */
    public static final String UPDATE = "http://www.website.com/client.jar";

    /**
     * How often do you want the server to ping all of the clients.&nbsp;A lower number should be
     * used if your client load is below about 500.&nbsp; After 500, I suggest using over 30
     * seconds per PING to client.
     */
    public static final int PING_RATE_SECS = 60;

    /**
     * The required username for clients to use to gain ADMIN access to the server.
     */
    public static final String ADMIN_USERNAME = "root";

    /**
     * The required password to allow authentication of a client to access ADMIN powers.
     */
    public static final String ADMIN_PASSWORD = "somerandompassword123";

}
