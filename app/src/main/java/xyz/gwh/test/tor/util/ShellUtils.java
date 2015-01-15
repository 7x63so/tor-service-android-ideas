package xyz.gwh.test.tor.util;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for Android processes.
 */
public final class ShellUtils {

    private ShellUtils() {
        // restrict instantiation
    }

    /**
     * The Android ps <name> command
     */
    private static final String CMD_PS = "toolbox ps -o pid,name %s";

    public static int findProcessId(String cmdStr) throws IOException {
        //TODO: need to test this with adb shell
        //https://stackoverflow.com/questions/17925635/java-regex-extract-pid-from-ps-command

        int procId = -1;
        String processKey = new File(cmdStr).getName();

        try {
            runCommand(String.format(CMD_PS, processKey));
        } catch (Exception e) {
            Broadcaster.getInstance().log("There was an error when destroying process:" + processKey);
        }

        return procId;
    }

    /**
     * Runs a command in a new shell.
     */
    public static void runCommand(String command) throws Exception {
        Shell shell = Shell.startShell();
        shell.add(new SimpleCommand(command)).waitForFinish();
        shell.close();
    }

    /**
     * Kills all processes running with the given file.
     */
    public static void killProcess(File file) throws IOException {
        try {
            String processName = file.getName();
            Shell shell = Shell.startShell();
            Toolbox toolbox = new Toolbox(shell);

            toolbox.killAll(processName);
            shell.close();

            Broadcaster.getInstance().log("Found " + file.getName() + " - killing now...");
        } catch (Exception e) {
            // log?
        }
    }
}