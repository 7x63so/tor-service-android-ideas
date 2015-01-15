package xyz.gwh.test.tor.util;

/**
 * Helper class for running code that might need a few attempts to succeed.
 */
public class Tryable {

    private static final int DEFAULT_ATTEMPT_INTERVAL = 1000;

    private int attemptInterval;

    public interface Action {
        /**
         * Returns true on successful execution.
         */
        public boolean execute();
    }

    public Tryable(int attemptInterval) {
        this.attemptInterval = attemptInterval;
    }

    public Tryable() {
        this(DEFAULT_ATTEMPT_INTERVAL);
    }

    /**
     * Attempt to perform the action for the max number of attempts given.
     */
    public void attempt(Action action, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            if (!action.execute()) {
                try {
                    Thread.sleep(attemptInterval);
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        }
    }
}