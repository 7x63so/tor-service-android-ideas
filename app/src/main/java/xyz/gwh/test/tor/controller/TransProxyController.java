package xyz.gwh.test.tor.controller;

import xyz.gwh.test.tor.util.ShellUtils;

import java.io.File;

/**
 * Controller for transparent proxying.
 *
 * //TODO: implement setTransparentProxyingByApp(), logging, exceptions
 */
public class TransProxyController {

    private enum IpVersion {
        V4, V6
    }

    private static final int STANDARD_DNS_PORT = 53;

    private static final String PATH_XBIN_IPV4 = "/system/xbin/iptables";
    private static final String PATH_BIN_IPV4 = "/system/bin/iptables";
    private static final String PATH_XBIN_IPV6 = "/system/xbin/ip6tables";
    private static final String PATH_BIN_IPV6 = "/system/bin/ip6tables";

    private static final String IPTABLES_APPEND_RULES_COMMAND = "--append";
    private static final String IPTABLES_DELETE_RULES_COMMAND = "--delete";

    private String pathIptablesV4;
    private String pathIptablesV6;

    /**
     * The controller will send commands to xtables.
     */
    public TransProxyController(File fileXtables) {
        pathIptablesV4 = fileXtables.getAbsolutePath() + " iptables";
        pathIptablesV6 = fileXtables.getAbsolutePath() + " ip6tables";
    }

    /**
     * The controller will send commands to the system iptables.
     */
    public TransProxyController() {
        pathIptablesV4 = findIptablesPath(IpVersion.V4);
        pathIptablesV6 = findIptablesPath(IpVersion.V6);
    }

    /**
     * Enables transparent proxying.
     */
    public void enableProxy(int uid, int dnsPort, int proxyPort) throws Exception {
        setIpV6Rules(uid, IPTABLES_APPEND_RULES_COMMAND);
        setProxyRules(uid, IPTABLES_APPEND_RULES_COMMAND, dnsPort, proxyPort);
    }

    /**
     * Disables transparent proxying.
     */
    public void disableProxy(int uid, int dnsPort, int proxyPort) throws Exception {
        setIpV6Rules(uid, IPTABLES_DELETE_RULES_COMMAND);
        setProxyRules(uid, IPTABLES_DELETE_RULES_COMMAND, dnsPort, proxyPort);
    }

    /**
     * Deletes all proxy rules from the chain.
     */
    public void flushRules() throws Exception {
        String commandFormat;
        String command;

        commandFormat = "%s -t nat --flush";
        command = String.format(commandFormat, pathIptablesV4);
        ShellUtils.runCommand(command);

        commandFormat = "%s -t filter --flush";
        command = String.format(commandFormat, pathIptablesV4);
        ShellUtils.runCommand(command);

        setIpV6Rules(IPTABLES_DELETE_RULES_COMMAND);
    }

    /**
     * Enable proxy for tethering.
     */
    public void enableTetheringProxy(int dnsPort, int proxyPort) throws Exception {
        String[] hwinterfaces = new String[]{"usb0", "wl0.1"};

        for (String hwinterface : hwinterfaces) {
            String tcpCommand = pathIptablesV4 + " -t nat --append PREROUTING -i " + hwinterface + " -p udp --dport 53 -j REDIRECT --to-ports" + dnsPort;
            ShellUtils.runCommand(tcpCommand);

            String udpCommand = pathIptablesV4 + " -t nat --append PREROUTING -i " + hwinterface + " -p tcp -j REDIRECT --to-ports" + proxyPort;
            ShellUtils.runCommand(udpCommand);
        }
    }

    /**
     * Appends or deletes rules for proxying depending on the command given.
     */
    private void setProxyRules(int uid, String iptablesCommand, int dnsPort, int proxyPort) throws Exception {
        String natTableCommand = pathIptablesV4 + " -t nat " + iptablesCommand + " OUTPUT ";
        String filterTableCommand = pathIptablesV4 + " -t filter " + iptablesCommand + " OUTPUT ";
        String command;

        // allow everything for Tor
        command = natTableCommand + "-m owner --uid-owner " + uid + " -j ACCEPT";
        ShellUtils.runCommand(command);

        // allow loopback
        command = natTableCommand + "-o lo -j ACCEPT";
        ShellUtils.runCommand(command);

        // set up port redirection
        command = natTableCommand + "-p tcp ! -d 127.0.0.1 -m owner ! --uid-owner " + uid + " -m tcp --syn -j REDIRECT --to-ports " + proxyPort;
        ShellUtils.runCommand(command);

        // same for DNS
        command = natTableCommand + "-p udp ! -d 127.0.0.1 -m owner ! --uid-owner " + uid + " --dport " + STANDARD_DNS_PORT + " -j REDIRECT --to-ports " + dnsPort;
        ShellUtils.runCommand(command);

        // allow access to transproxy port
        command = filterTableCommand + "-p tcp -m tcp --dport %d -j ACCEPT";
        ShellUtils.runCommand(command);

        // allow access to local HTTP port
        command = filterTableCommand + "-p tcp -m tcp --dport %d -j ACCEPT";
        ShellUtils.runCommand(command);

        // allow access to local SOCKS port
        command = filterTableCommand + "-p tcp -m tcp --dport %d -j ACCEPT";
        ShellUtils.runCommand(command);

        // allow access to local DNS port
        command = filterTableCommand + "-p udp -m udp --dport %d -j ACCEPT";
        ShellUtils.runCommand(command);

        // reject all other packets
        command = filterTableCommand + "-m owner ! --uid-owner %d ! -d 127.0.0.1 -j REJECT";
        ShellUtils.runCommand(command);
    }

    /**
     * Appends or deletes rules for IPv6 traffic depending on the command given.
     */
    private void setIpV6Rules(int uid, String iptablesCommand) throws Exception {
        String uidCommand = "";

        if (uid != -1) {
            uidCommand = " -m owner --uid-owner " + uid;
        }

        String command = pathIptablesV6 + iptablesCommand + " OUTPUT " + uidCommand + " -j DROP";
        ShellUtils.runCommand(command);
    }

    /**
     * Convenience method for calling setIpV6Rules(int,String) without a uid.
     */
    private void setIpV6Rules(String iptablesCommand) throws Exception {
        setIpV6Rules(-1, iptablesCommand);
    }

    /**
     * Returns the native iptables path or an empty String.
     * Looks in /system/xbin/ and /system/bin/
     */
    private String findIptablesPath(IpVersion ipVersion) {
        String xbinPath = "";
        String binPath = "";
        File file;

        switch (ipVersion) {
            case V4:
                xbinPath = PATH_XBIN_IPV4;
                binPath = PATH_BIN_IPV4;
                break;
            case V6:
                xbinPath = PATH_XBIN_IPV6;
                binPath = PATH_BIN_IPV6;
                break;
        }

        file = new File(xbinPath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }

        file = new File(binPath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }

        return "";
    }
}