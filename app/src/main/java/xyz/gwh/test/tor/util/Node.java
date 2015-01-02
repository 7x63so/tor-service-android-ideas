package xyz.gwh.test.tor.util;

import java.util.StringTokenizer;

/**
 * Represents a node in the network.
 */
public class Node {
    String status;
    String id;
    String name;
    String ipAddress;
    String country;
    String organization;

    public Node(String circuitStatus, String circuitPath) {
        StringTokenizer st = new StringTokenizer(circuitPath, ",");

        while (st.hasMoreTokens()) {
            String nodePath = st.nextToken();

            String[] nodeParts;

            if (nodePath.contains("=")) {
                nodeParts = nodePath.split("=");
            } else {
                nodeParts = nodePath.split("~");
            }

            if (nodeParts.length == 1) {
                id = nodeParts[0].substring(1);
                name = id;
            } else if (nodeParts.length == 2) {
                id = nodeParts[0].substring(1);
                name = nodeParts[1];
            }

            status = circuitStatus;
        }
    }
}
