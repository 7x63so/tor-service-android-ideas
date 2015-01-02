package xyz.gwh.test.tor.util;

/**
 * Utility class for manipulating Strings.
 */
public final class StringUtils {

    private StringUtils() {
        // restrict instantiation
    }

    /**
     * Converts the supplied argument into a string.
     * Under 2Mb, returns "xxx.xKb"
     * Over 2Mb, returns "xxx.xxMb"
     */
    public static String formatTrafficCount(long count) {
        int divisor;
        String unit;

        if (count < 1e6) {
            divisor = 10;
            unit = "Kbps";
        } else {
            divisor = 100;
            unit = "Mbps";
        }

        return ((float) ((int) (count * divisor / 1024 / 1024)) / divisor + unit);
    }

    public static String parseNodeName(String node) {
        if (node.contains("=")) {
            return (node.substring(node.indexOf("=") + 1));
        } else if (node.contains("~")) {
            return (node.substring(node.indexOf("~") + 1));
        } else {
            return node;
        }
    }

    public static String parsePort(String port) {
        String[] portSplit = port.split(":");

        if (portSplit.length > 1) {
            return portSplit[1];
        }

        return port;
    }
}
