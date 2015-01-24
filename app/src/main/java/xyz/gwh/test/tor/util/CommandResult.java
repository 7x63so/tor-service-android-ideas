package xyz.gwh.test.tor.util;

/**
 * Container for exit code and output returned by shell command.
 */
public final class CommandResult {
    public final int exitCode;
    public final String output;

    public CommandResult(int exitCode, String output) {
        this.exitCode = exitCode;
        this.output = output;
    }
}