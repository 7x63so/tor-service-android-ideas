package xyz.gwh.test.tor.controller;

import xyz.gwh.test.tor.R;
import xyz.gwh.test.tor.service.ServiceInfo;
import xyz.gwh.test.tor.util.Broadcaster;
import xyz.gwh.test.tor.util.ShellUtils;
import xyz.gwh.test.tor.util.Tryable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Controller for Polipo commands.
 */
public class PolipoController {

    private static final int MAX_START_ATTEMPTS = 3;
    private static final int PORT_HTTP = 8118;

    private static final String KEY_SOCKS_PARENT_PROXY = "socksParentProxy";
    private static final String KEY_PROXY_PORT = "proxyPort";

    private ServiceInfo serviceInfo;

    public PolipoController(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    public void startPolipo() throws Exception {
        Broadcaster.getInstance().log("Starting polipo process");

        if (!isPolipoRunning()) {
            // should we be doing this every time?
            writeToConfig();

            String pathPolipo = serviceInfo.filePolipo.getCanonicalPath();
            String pathPolipoConfig = serviceInfo.filePolipoConfig.getCanonicalPath();
            String command = pathPolipo + " -c " + pathPolipoConfig + " &";

            ShellUtils.runCommand(command);

            new Tryable().attempt(new Tryable.Action() {
                @Override
                public boolean execute() {
                    if (!isPolipoRunning()) {
                        Broadcaster.getInstance().log("Couldn't find Polipo process... retrying...\n");
                        return false;
                    }

                    Broadcaster.getInstance().logFormat(R.string.privoxy_is_running_on_port_, Integer.toString(PORT_HTTP));
                    return true;
                }
            }, MAX_START_ATTEMPTS);
        }
    }

    public void stopPolipo() throws IOException {
        ShellUtils.killProcess(serviceInfo.filePolipo);
    }

    private boolean isPolipoRunning() {
        try {
            return ShellUtils.findProcessId(serviceInfo.filePolipo.getCanonicalPath()) != -1;
        } catch (IOException e) {
            return false;
        }
    }

    private void writeToConfig() throws IOException {
        Properties props = new Properties();

        props.load(new FileReader(serviceInfo.filePolipoConfig));
        props.put(KEY_SOCKS_PARENT_PROXY, "\"localhost:" + serviceInfo.socksPort + "\"");
        props.put(KEY_PROXY_PORT, Integer.toString(serviceInfo.httpPort));
        props.store(new FileWriter(serviceInfo.filePolipoConfig), "Updated Polipo config");
    }
}