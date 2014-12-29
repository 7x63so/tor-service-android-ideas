package xyz.gwh.test.tor.exception;

/**
 * Thrown if chown command failed to change permissions on a file.
 */
public class PermissionsNotSetException extends Exception {
    public PermissionsNotSetException(String message) {
        super(message);
    }
}
