package xyz.gwh.test.tor.exception;

/**
 * Thrown if a tor binary resource could not be installed.
 */
public class ResourceNotInstalledException extends Exception {
    public ResourceNotInstalledException(String message) {
        super(message);
    }
}
