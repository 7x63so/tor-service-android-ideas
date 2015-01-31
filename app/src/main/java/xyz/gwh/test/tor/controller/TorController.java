package xyz.gwh.test.tor.controller;

import android.support.annotation.Nullable;
import net.freehaven.tor.control.EventHandler;
import net.freehaven.tor.control.TorControlConnection;
import xyz.gwh.test.tor.R;
import xyz.gwh.test.tor.resources.Torrc;
import xyz.gwh.test.tor.service.ServiceInfo;
import xyz.gwh.test.tor.service.TorStatus;
import xyz.gwh.test.tor.util.Broadcaster;
import xyz.gwh.test.tor.util.CommandResult;
import xyz.gwh.test.tor.util.IOUtils;
import xyz.gwh.test.tor.util.ShellUtils;
import xyz.gwh.test.tor.util.Tryable;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for Tor commands, using the jtorcontrol library.
 */
public class TorController {

    private static final String CMD_RESULT = "Tor (%s): %s";
    private static final String CMD_VERIFY = "%s DataDirectory %s --verify-config";
    private static final String CMD_RUN = "%s DataDirectory %s";
    private static final String IP_LOCALHOST = "127.0.0.1";

    private static final String SIGNAL_NEWNYM = "NEWNYM";
    private static final String SIGNAL_RELOAD = "RELOAD";
    private static final String SIGNAL_HALT = "HALT";

    private static final List<String> EVENTS = Arrays.asList("ORCONN", "CIRC", "NOTICE", "WARN", "ERR", "BW");

    private static final int MAX_CONNECTION_ATTEMPTS = 3;

    private TorControlConnection controlConnection;
    private PolipoController polipoController;

    private ServiceInfo serviceInfo;

    public TorController(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
        polipoController = new PolipoController(serviceInfo);
    }

    public void startTor(@Nullable Torrc torrc) {
        if (!isTorRunning()) {
            Broadcaster.getInstance().log(R.string.status_starting_up);

            try {
                startTorProcess(torrc);
            } catch (Exception e) {
                Broadcaster.getInstance().log("Unable to start Tor: " + e.toString(), e);
                Broadcaster.getInstance().status(TorStatus.OFF);

                //TODO: notifications...
                stopTor();
            }
        }
    }

    public boolean isTorRunning() {
        if (controlConnection != null) {
            try {
                return ShellUtils.isProcessRunning(serviceInfo.fileTor.getName());
            } catch (Exception e) {
                // return false, log?
            }
        }
        return false;
    }

    public void stopTor() {
        try {
            Broadcaster.getInstance().log("Using control port to shutdown Tor");
            Broadcaster.getInstance().log("Sending HALT signal to Tor process");

            // use signal() instead of shutdownTor() so we wait for response
            if (controlConnection != null) {
                controlConnection.signal(SIGNAL_HALT);
            }

            ShellUtils.killProcess(serviceInfo.fileTor);
            polipoController.stopPolipo();
        } catch (IOException e) {
            Broadcaster.getInstance().log("There was a problem shutting Tor down - try again?");
        }
    }

    public void setConfig(@Nullable Torrc torrc) {
        if (torrc != null && isTorRunning()) {
            try {
                controlConnection.setConf(torrc.asCollection());
                controlConnection.saveConf();
                controlConnection.signal(SIGNAL_RELOAD);
            } catch (IOException e) {
                Broadcaster.getInstance().log("There was a problem setting the configuration: " + e.getMessage());
            }
        }
    }

    public void newIdentity() {
        if (isTorRunning()) {
            try {
                controlConnection.signal(SIGNAL_NEWNYM);
            } catch (IOException e) {
                // the user should probably be alerted of this, maybe throw exception in signature?
                Broadcaster.getInstance().log("Could not switch to a new identity!");
            }
        }
    }

    // not sure when to set this...
    public void setEventHandler(@Nullable final EventHandler eventHandler) {
        if (eventHandler != null) {
            controlConnection.setEventHandler(eventHandler);
            try {
                controlConnection.setEvents(EVENTS);
            } catch (IOException e) {
                Broadcaster.getInstance().log("Could not set the EventHandler: " + e.getMessage());
            }
        }
    }

    private void startTorProcess(Torrc torrc) {
        Broadcaster.getInstance().status(TorStatus.CONNECTING);
        Broadcaster.getInstance().log(R.string.status_starting_up);

        try {
            String pathDirCache = serviceInfo.dirCache.getCanonicalPath();
            String pathTor = serviceInfo.fileTor.getCanonicalPath();
            String cmdVerify = String.format(CMD_VERIFY, pathTor, pathDirCache);
            String cmdRun = String.format(CMD_RUN, pathTor, pathDirCache);

            execute(cmdVerify);
            execute(cmdRun);

            connectToControlPort();
            authenticateWithCookie();

            //TODO: handle sockets...
            //initControlConnection(false);

            controlConnection.setConf(torrc.asCollection());

            polipoController.startPolipo();
        } catch (Exception e) {
            // ignore - check if Tor is running below
        }

        if (isTorRunning()) {
            Broadcaster.getInstance().log("Tor started; process id=" + getTorPid());
        } else {
            Broadcaster.getInstance().status(TorStatus.OFF);
            Broadcaster.getInstance().log("Unable to start Tor");
        }
    }

    private void connectToControlPort() {
        final int port = getControlPort();
        Broadcaster.getInstance().log("Waiting for control port...");

        new Tryable().attempt(new Tryable.Action() {
            @Override
            public boolean execute() {
                try {
                    Broadcaster.getInstance().log("Connecting to control port: " + port);

                    Socket torSocket = new Socket(IP_LOCALHOST, port);
                    torSocket.setSoTimeout(0);

                    controlConnection = new TorControlConnection(torSocket);
                    controlConnection.launchThread(true);

                    Broadcaster.getInstance().log("SUCCESS connected to Tor control port.");
                    return true;
                } catch (Exception e) {
                    Broadcaster.getInstance().log("Error connecting to Tor local control port: " + e.getMessage(), e);
                }
                return false;
            }
        }, MAX_CONNECTION_ATTEMPTS);
    }

    private void authenticateWithCookie() throws Exception {
        if (!serviceInfo.fileAuthCookie.exists()) {
            Broadcaster.getInstance().log("Tor authentication cookie does not exist yet");
            throw new Exception("Couldn't authenticate, no cookie!");
        }

        byte[] cookie = new byte[(int) serviceInfo.fileAuthCookie.length()];
        DataInputStream fis = new DataInputStream(new FileInputStream(serviceInfo.fileAuthCookie));
        fis.read(cookie);
        fis.close();

        controlConnection.authenticate(cookie);
        Broadcaster.getInstance().log("SUCCESS - authenticated to control port.");
        Broadcaster.getInstance().log(R.string.tor_process_starting);
    }

    private int getControlPort() {
        try {
            if (serviceInfo.fileControlPort.exists()) {
                Broadcaster.getInstance().log("Reading control port config file: " + serviceInfo.fileControlPort.getCanonicalPath());

                String port = IOUtils.readLine(serviceInfo.fileControlPort);
                if (!port.isEmpty()) {
                    String[] lineParts = port.split(":");
                    return Integer.parseInt(lineParts[1]);
                }
            } else {
                Broadcaster.getInstance().log("Control Port config file does not yet exist (waiting for tor): " + serviceInfo.fileControlPort.getCanonicalPath());
            }
        } catch (Exception e) {
            Broadcaster.getInstance().log("unable to read control port config file");
        }
        return -1;
    }

    private int getTorPid() {
        if (isTorRunning()) {
            String pidStr;
            try {
                pidStr = controlConnection.getInfo("process/pid");
                return Integer.parseInt(pidStr);
            } catch (IOException e) {
                // return -1
            } catch (NumberFormatException e) {
                // return -1
            }
        }
        return -1;
    }

    private void execute(String cmdStr) throws Exception {
        CommandResult result = ShellUtils.runCommand(cmdStr);

        int exitCode = result.exitCode;
        String output = result.output;

        //TODO: custom exception
        if (exitCode != 0 && output != null && output.length() > 0) {
            String message = String.format(CMD_RESULT, Integer.toString(exitCode), output);

            Broadcaster.getInstance().log(message);
            throw new Exception(message);
        }
    }

    private void initControlConnection(boolean isReconnect) throws Exception {
        //TODO: not entirely sure what we're doing here...
        Broadcaster.getInstance().status(TorStatus.CONNECTING);
        String confSocks = controlConnection.getInfo("net/listeners/socks");

        //if we are reconnected then we don't need to reset the ports
        if (!isReconnect) {
            try {
                ServerSocket ss = new ServerSocket(serviceInfo.socksPort);
                ss.close();

                Broadcaster.getInstance().log("Local SOCKS port: " + serviceInfo.socksPort);
            } catch (Exception e) {
                Broadcaster.getInstance().log("Error setting TransProxy port to: " + serviceInfo.socksPort);
            }

            try {
                ServerSocket ss = new ServerSocket(serviceInfo.proxyPort);
                ss.close();

                Broadcaster.getInstance().log("Local TransProxy port: " + serviceInfo.proxyPort);
            } catch (Exception e) {
                Broadcaster.getInstance().log("ERROR setting TransProxy port to: " + serviceInfo.proxyPort);
            }

            try {
                ServerSocket ss = new ServerSocket(serviceInfo.dnsPort);
                ss.close();

                Broadcaster.getInstance().log("Local DNSPort port: " + serviceInfo.proxyPort);
            } catch (Exception e) {
                Broadcaster.getInstance().log("ERROR setting DNSport to: " + serviceInfo.dnsPort);
            }
        }
    }
}