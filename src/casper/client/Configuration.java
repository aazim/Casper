package casper.client;

public class Configuration {

    /**
     * The server list that the client can connect to.&nbsp;There must be at least
     * one working server on the list.&nbsp;The client randomly selects a server
     * from this list to connect to. Please enter servers in the following format:
     * "domain:port". Examples:
     * <ul>
     * <li>"server1.casper.me:8817",</li>
     * <li>"server2.casper.me:8817",</li>
     * <li>"server1.sexypajamaparty.net:8817"</li>
     * </ul>
     */
    private static final String[] SERVERS = {
            "127.0.0.1:8817"
    };

    /**
     * The current client version.&nbsp;This needs to be changed every time you
     * want the clients to perform an update.&nbsp;The version number must also
     * be changed accordingly in the server.Configuration class.
     */
    private static final int VERSION = 1;

    /**
     * The maximum amount of time to wait for a PING command to be issued by the
     * Server.&nbsp;If the client does not receive a PING in the allotted time
     * that is provided below (in milliseconds), it will throw a "PING TIMEOUT"
     * and then attempt to reconnect.
     */
    private static final long TIMEOUT_MILLIS = 70000;

    /**
     * This sets the time (in milliseconds) to wait before reconnecting to the
     * Server after a "PING TIMEOUT" or some other unseen error.
     */
    private static final long RECONNECT_MILLIS = 15000;

    /**
     * @return The servers provided as an array.
     */
    public static String[] servers() {
        return SERVERS;
    }

    /**
     * @return The current version of the Client.
     */
    public static int version() {
        return VERSION;
    }

    /**
     * @return The time before throwing a ping timeout.
     */
    public static long timeout() {
        return TIMEOUT_MILLIS;
    }

    /**
     * @return The time to sleep before reconnecting to the Server.
     */
    public static long reconnect() {
        return RECONNECT_MILLIS;
    }


}
