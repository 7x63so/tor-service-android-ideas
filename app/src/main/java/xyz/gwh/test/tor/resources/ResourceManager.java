/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package xyz.gwh.test.tor.resources;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;
import xyz.gwh.test.tor.R;
import xyz.gwh.test.tor.exception.PermissionsNotSetException;
import xyz.gwh.test.tor.exception.ResourceNotInstalledException;
import xyz.gwh.test.tor.util.IOUtils;
import xyz.gwh.test.tor.util.ShellUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the installation of Tor resources.
 */
public final class ResourceManager {

    public static final String FILENAME_TOR = "tor";
    public static final String FILENAME_TORRC = "torrc";
    public static final String FILENAME_POLIPO = "polipo";
    public static final String FILENAME_POLIPO_CONF = "torpolipo.conf";
    public static final String FILENAME_GEOIP = "geoip";
    public static final String FILENAME_GEOIP6 = "geoip6";
    public static final String FILENAME_OBFSCLIENT = "obfsclient";
    public static final String FILENAME_IPTABLES = "xtables";
    public static final String FILENAME_CONTROL_PORT = "control.txt";
    public static final String FILENAME_AUTH_COOKIE = "control_auth_cookie";

    private static final String CMD_VERSION = "%s --version";
    private static final String CMD_DELETE_FILES_IN_DIR = "rm -rf -f %s/*";
    private static final String PERMISSION_EXECUTABLE = "770";

    private static final Map<String, Integer> resources = new HashMap<String, Integer>();

    static {
        resources.put(FILENAME_TORRC, R.raw.torrc);
        resources.put(FILENAME_POLIPO_CONF, R.raw.torpolipo);
        resources.put(FILENAME_TOR, R.raw.tor);
        resources.put(FILENAME_POLIPO, R.raw.polipo);
        resources.put(FILENAME_OBFSCLIENT, R.raw.obfsclient);
        resources.put(FILENAME_IPTABLES, R.raw.xtables);
        resources.put(FILENAME_GEOIP, R.raw.geoip);
        resources.put(FILENAME_GEOIP6, R.raw.geoip6);
    }

    private Map<String, File> installedResources;
    private String pathInstallDir;
    private Context context;
    private String installedVersion = "";

    public ResourceManager(Context context, String pathInstallDir) {
        this.pathInstallDir = pathInstallDir;
        this.context = context;

        installedResources = new HashMap<String, File>();
    }

    /**
     * Clears installation directory, installs binaries and sets them as executable.
     */
    public void installResources() throws ResourceNotInstalledException, PermissionsNotSetException {
        installedResources.clear();

        try {
            deleteFilesInPath(pathInstallDir);
        } catch (Exception e) {
            throw new ResourceNotInstalledException("Unable to delete files in the installation directory: " + e.getMessage());
        }

        for (String filename : resources.keySet()) {
            File file = new File(pathInstallDir, filename);
            installRawResource(file, resources.get(filename));
            setBinaryAsExecutable(file);
            installedResources.put(filename, file);
        }
    }

    /**
     * Compares the installed version of Tor to the input String.
     * Lazily instantiates the installed version.
     */
    public boolean isVersion(@Nullable String input) {
        if (input != null) {
            if (installedVersion.isEmpty()) {
                installedVersion = getInstalledVersion();
            }
            return installedVersion.equals(input);
        }
        return false;
    }

    /**
     * Returns a map of installed resources in the format <Filename, File>.
     */
    @NonNull
    public Map<String, File> getInstalledResources() {
        return installedResources;
    }

    /**
     * Queries the Tor binary for its version.
     */
    private String getInstalledVersion() {
        String version = "Unknown";

        try {
            String pathTor = installedResources.get(FILENAME_TOR).getCanonicalPath();
            String cmd = String.format(CMD_VERSION, pathTor);

            version = ShellUtils.runCommand(cmd).output;
        } catch (Exception e) {
            // return "Unknown"
        }
        return version;
    }

    /**
     * Deletes all files in directory at given path.
     */
    private void deleteFilesInPath(String path) throws Exception {
        String cmd = String.format(CMD_DELETE_FILES_IN_DIR, path);
        ShellUtils.runCommand(cmd);
    }

    /**
     * Installs resource for given id.
     */
    private void installRawResource(File file, int resId) throws ResourceNotInstalledException {
        InputStream is = context.getResources().openRawResource(resId);

        try {
            IOUtils.writeToFile(is, file, false, true);
        } catch (IOException e) {
            throw new ResourceNotInstalledException("Unable to install " + file.getAbsolutePath());
        }
    }

    /**
     * Attempts to change permissions on file to executable.
     */
    private void setBinaryAsExecutable(File file) throws PermissionsNotSetException {
        try {
            Shell shell = Shell.startShell();
            Toolbox toolbox = new Toolbox(shell);
            toolbox.setFilePermissions(file.getAbsolutePath(), PERMISSION_EXECUTABLE);
            shell.close();
        } catch (Exception e) {
            throw new PermissionsNotSetException("Unable to set executable permissions on " + file.getAbsolutePath());
        }
    }
}