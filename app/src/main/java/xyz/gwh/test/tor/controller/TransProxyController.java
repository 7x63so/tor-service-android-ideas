package xyz.gwh.test.tor.controller;

import xyz.gwh.test.tor.util.Broadcaster;
import xyz.gwh.test.tor.util.ShellUtils;

import java.io.File;
import java.util.List;

/**
 * Controller for transparent proxying.
 * TODO: notifications should be handled with Broadcaster
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

    private static final String ORBOT_PACKAGE = "org.torproject.android";

    private String pathIptablesV4;
    private String pathIptablesV6;

    private int uid;
    private int dnsPort;
    private int proxyPort;

    /**
     * The controller will send commands to xtables.
     */
    public TransProxyController(int uid, int dnsPort, int proxyPort, File fileXtables) {
        this.uid = uid;
        this.dnsPort = dnsPort;
        this.proxyPort = proxyPort;

        pathIptablesV4 = fileXtables.getAbsolutePath() + " iptables";
        pathIptablesV6 = fileXtables.getAbsolutePath() + " ip6tables";
    }

    /**
     * The controller will send commands to the system iptables.
     */
    public TransProxyController(int uid, int dnsPort, int proxyPort) {
        this.uid = uid;
        this.dnsPort = dnsPort;
        this.proxyPort = proxyPort;

        pathIptablesV4 = findIptablesPath(IpVersion.V4);
        pathIptablesV6 = findIptablesPath(IpVersion.V6);
    }

    /**
     * Enables transparent proxying for everything.
     */
    public void enableProxy(int httpPort, int socksPort) throws Exception {
        setIpV6Rules(uid, IPTABLES_APPEND_RULES_COMMAND);
        setProxyRules(httpPort, socksPort, IPTABLES_APPEND_RULES_COMMAND);
    }

    /**
     * Disables transparent proxying for everything.
     */
    public void disableProxy(int httpPort, int socksPort) throws Exception {
        setIpV6Rules(uid, IPTABLES_DELETE_RULES_COMMAND);
        setProxyRules(httpPort, socksPort, IPTABLES_DELETE_RULES_COMMAND);
    }

    /**
     * Enables transparent proxying for each app in the list.
     */
    public void enableProxy(List<TorifiedApp> apps) throws Exception {
        setProxyRules(IPTABLES_APPEND_RULES_COMMAND, apps);
    }

    /**
     * Disables transparent proxying for each app in the list.
     */
    public void disableProxy(List<TorifiedApp> apps) throws Exception {
        setProxyRules(IPTABLES_DELETE_RULES_COMMAND, apps);
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
     * Appends or deletes rules for proxying each app in the list.
     */
    private void setProxyRules(String iptablesCommand, List<TorifiedApp> apps) throws Exception {
        String command;

        command = pathIptablesV4 + " -t nat " + iptablesCommand + " OUTPUT -p udp --dport " + STANDARD_DNS_PORT + " -j REDIRECT --to-ports " + dnsPort;
        ShellUtils.runCommand(command);

        // build up array of shell cmds to execute under one root context
        for (TorifiedApp app : apps) {
            boolean isAppendRulesCommand = IPTABLES_APPEND_RULES_COMMAND.equals(iptablesCommand);
            boolean isOrbotPackage = app.getUsername().equals(ORBOT_PACKAGE);
            if (!isOrbotPackage && (isAppendRulesCommand || app.isTorified())) {
                Broadcaster.getInstance().log("transproxy for app: " + app.getUsername() + " (" + app.getUid() + "): enable=" + isAppendRulesCommand);

                setIpV6Rules(app.getUid(), iptablesCommand);

                // Set up port redirection
                command = pathIptablesV4 + " -t nat " + iptablesCommand + " OUTPUT  -p tcp ! -d 127.0.0.1 -m owner --uid-owner " + uid + " -m tcp --syn -j REDIRECT --to-ports " + proxyPort;
                ShellUtils.runCommand(command);

                // Reject all other outbound packets
                command = pathIptablesV4 + " -t filter " + iptablesCommand + " OUTPUT -m owner --uid-owner " + app.getUid() + " ! -d 127.0.0.1 -j REJECT";
                ShellUtils.runCommand(command);
            }
        }
    }

    /**
     * Appends or deletes rules for proxying.
     */
    private void setProxyRules(int httpPort, int socksPort, String iptablesCommand) throws Exception {
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

        //TODO: dns leak and tcp leak protection commands if debug logging enabled

        // allow access to transproxy port
        command = filterTableCommand + "-p tcp -m tcp --dport " + proxyPort + " -j ACCEPT";
        ShellUtils.runCommand(command);

        // allow access to local HTTP port
        command = filterTableCommand + "-p tcp -m tcp --dport " + httpPort + " -j ACCEPT";
        ShellUtils.runCommand(command);

        // allow access to local SOCKS port
        command = filterTableCommand + "-p tcp -m tcp --dport " + socksPort + " -j ACCEPT";
        ShellUtils.runCommand(command);

        // allow access to local DNS port
        command = filterTableCommand + "-p udp -m udp --dport " + dnsPort + " -j ACCEPT";
        ShellUtils.runCommand(command);

        // reject all other packets
        command = filterTableCommand + "-m owner ! --uid-owner " + uid + " ! -d 127.0.0.1 -j REJECT";
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