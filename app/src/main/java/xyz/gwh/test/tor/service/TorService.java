package xyz.gwh.test.tor.service;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import net.freehaven.tor.control.EventHandler;
import xyz.gwh.test.tor.controller.TorController;
import xyz.gwh.test.tor.util.Broadcaster;
import xyz.gwh.test.tor.exception.PermissionsNotSetException;
import xyz.gwh.test.tor.exception.ResourceNotInstalledException;
import xyz.gwh.test.tor.resources.ResourceManager;
import xyz.gwh.test.tor.resources.Torrc;
import xyz.gwh.test.tor.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static xyz.gwh.test.tor.resources.ResourceManager.FILENAME_CONTROL_PORT;

public class TorService extends Service implements EventHandler, Broadcaster.OnStatusChangedListener {

    private static final String TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE = "Bootstrapped 100%";
    private static final String BINARY_TOR_VERSION = "0.2.5.10-openssl1.0.1i-nonPIE-polipofix";
    private static final String DIRECTORY_TOR_BINARY = "bin";
    private static final String DIRECTORY_TOR_DATA = "data";
    private static final String STATUS_BUILT = "BUILT";

    private static final String LOG_MESSAGE = "Message (\"%s\"): %s";
    private static final String LOG_CIRCUIT = "Circuit (\"%s\"): %s";
    private static final String LOG_STREAM_STATUS = "StreamStatus (\"%s\"): %s";
    private static final String LOG_OR_CONN_STATUS = "orConnStatus (\"%s\"): %s";

    public static final String INTENT_START = "start";
    public static final String INTENT_STOP = "stop";
    public static final String INTENT_FLUSH = "flush";
    public static final String INTENT_NEWNYM = "newnym";
    public static final String INTENT_VPN = "vpn";
    public static final String INTENT_UPDATE = "update";

    private static final String ARG_TORRC = "ARG_TORRC";

    private TorController torController;
    private ResourceManager resourceManager;

    private TorStatus torStatus;
    private long totalTrafficWritten;
    private long totalTrafficRead;
    private long lastWritten;
    private long lastRead;

    @Override
    public void onCreate() {
        super.onCreate();

        Broadcaster.initialize(this);
        Broadcaster.getInstance().registerOnStatusChangedListener(this);

        File dirBin = getDir(DIRECTORY_TOR_BINARY, Application.MODE_PRIVATE);
        File dirCache = getDir(DIRECTORY_TOR_DATA, Application.MODE_PRIVATE);
        resourceManager = new ResourceManager(this, dirBin.getAbsolutePath());

        try {
            installBinaries();
            torController = new TorController(dirCache.getCanonicalPath(), resourceManager.getInstalledResources());
        } catch (Exception e) {
            Broadcaster.getInstance().log("Unable to start Tor: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        TorCommand torCommand = new TorCommand(intent);
        new Thread(torCommand).start();

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onStatusChanged(TorStatus status) {
        torStatus = status;
    }

    @Override
    public void circuitStatus(String status, String circID, String path) {
        Broadcaster.getInstance().log(String.format(LOG_CIRCUIT, circID, status));

        if (status.equals(STATUS_BUILT)) {
            if (torStatus == TorStatus.CONNECTING) {
                Broadcaster.getInstance().status(TorStatus.ON);
            }

            Broadcaster.getInstance().status(torStatus);
        }
    }

    @Override
    public void streamStatus(String status, String streamID, String target) {
        Broadcaster.getInstance().log(String.format(LOG_STREAM_STATUS, streamID, status));
    }

    @Override
    public void orConnStatus(String status, String orName) {
        String nodeName = StringUtils.parseNodeName(orName);
        Broadcaster.getInstance().log(String.format(LOG_OR_CONN_STATUS, nodeName, status));
    }

    @Override
    public void bandwidthUsed(long read, long written) {
        String readStr = StringUtils.formatTrafficCount(read);
        String writtenStr = StringUtils.formatTrafficCount(written);
        String bandwidthLog = readStr + " \u2193" + " / " + writtenStr + " \u2191";

        //TODO: handle notifications!

        totalTrafficWritten += written;
        totalTrafficRead += read;
        lastWritten = written;
        lastRead = read;

        Broadcaster.getInstance().traffic(lastWritten, lastRead, totalTrafficWritten, totalTrafficRead);
    }

    @Override
    public void newDescriptors(List<String> orList) {
        // unused
    }

    @Override
    public void message(String severity, String msg) {
        Broadcaster.getInstance().log(severity + ": " + msg);

        if (msg.contains(TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE)) {
            Broadcaster.getInstance().status(torStatus);
        }
    }

    @Override
    public void unrecognized(String type, String msg) {
        Broadcaster.getInstance().log(String.format(LOG_MESSAGE, type, msg));
    }

    private void installBinaries() throws ResourceNotInstalledException, PermissionsNotSetException {
        Broadcaster.getInstance().log("checking for Tor version " + BINARY_TOR_VERSION);

        if (!resourceManager.isVersion(BINARY_TOR_VERSION)) {
            Broadcaster.getInstance().log("upgrading binaries to latest version: " + BINARY_TOR_VERSION);
            resourceManager.installResources();
        }
    }

    private class TorCommand implements Runnable {
        private Intent intent;
        private Torrc torrc;

        public TorCommand(Intent intent) {
            this.intent = intent;

            if (intent.hasExtra(ARG_TORRC) && intent.getSerializableExtra(ARG_TORRC) != null) {
                torrc = (Torrc) intent.getSerializableExtra(ARG_TORRC);
            } else {
                Torrc.Builder builder = Torrc.Builder.DEFAULT;

                try {
                    String controlPortWriteToFile = resourceManager.getInstalledResources().get(FILENAME_CONTROL_PORT).getCanonicalPath();
                    builder.setControlPortWriteToFile(controlPortWriteToFile);
                    torrc = builder.build();
                } catch (IOException e) {
                    // throw?
                }
            }
        }

        @Override
        public void run() {
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(INTENT_START)) {
                torController.startTor(torrc);
            } else if (action.equals(INTENT_STOP)) {
                torController.stopTor();
            } else if (action.equals(INTENT_NEWNYM)) {
                torController.newIdentity();
            } else if (action.equals(INTENT_FLUSH)) {
                //TODO: flush trans proxy
            } else if (action.equals(INTENT_UPDATE)) {
                torController.setConfig(torrc);
            } else if (action.equals(INTENT_VPN)) {
                //TODO: start the vpn
            } else {
                Broadcaster.getInstance().log("TorService received an unrecognized command: " + action);
            }
        }
    }
}