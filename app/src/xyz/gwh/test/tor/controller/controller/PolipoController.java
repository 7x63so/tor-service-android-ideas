package xyz.gwh.test.tor.controller;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Controls the Polipo binary.
 */
public class PolipoController implements PolipoController {

    private static final String PORT_HTTP = "8118";
    private static final String PORT_SOCKS = "9050";
    private static final String KEY_SOCKS_PARENT_PROXY = "socksParentProxy";
    private static final String KEY_PROXY_PORT = "proxyPort";

    private Broadcaster broadcaster;
    private File filePolipo;

    public PolipoController(Broadcaster broadcaster, File filePolipo) {
        this.broadcaster = broadcaster;
        this.filePolipo = filePolipo;
    }

    @Override
    public void startPolipo() throws IOException {
        //TODO...
    }

    @Override
    public void stopPolipo() {
        //TODO...
    }

    private void setPorts() throws IOException {
        Properties props = new Properties();
        props.load(new FileReader(filePolipo));

        props.put(KEY_SOCKS_PARENT_PROXY, "\"localhost:" + PORT_SOCKS + "\"");
        props.put(KEY_PROXY_PORT, PORT_HTTP);

        props.store(new FileWriter(filePolipo), "Updated ports");
    }
}
