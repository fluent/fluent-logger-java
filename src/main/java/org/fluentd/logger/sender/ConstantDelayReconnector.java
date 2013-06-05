package org.fluentd.logger.sender;

import java.util.LinkedList;

/**
 * Calculate exponential delay for reconnecting
 */
public class ConstantDelayReconnector implements Reconnector {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger
            .getLogger(ExponentialDelayReconnector.class.getName());

    private double wait = 50; // Default wait to 50 ms

    private int maxErrorHistorySize = 100;

    private LinkedList<Long> errorHistory = new LinkedList<Long>();

    public ConstantDelayReconnector() {
        errorHistory = new LinkedList<Long>();
    }

    public ConstantDelayReconnector(int wait) {
        this.wait = wait;
        errorHistory = new LinkedList<Long>();
    }

    public void addErrorHistory(long timestamp) {
        errorHistory.addLast(timestamp);
        if (errorHistory.size() > maxErrorHistorySize) {
            errorHistory.removeFirst();
        }
    }

    public boolean isErrorHistoryEmpty() {
        return errorHistory.isEmpty();
    }

    public void clearErrorHistory() {
        errorHistory.clear();
    }

    public boolean enableReconnection(long timestamp) {
        int size = errorHistory.size();
        if (size == 0) {
            return true;
        }

        return (!(timestamp - errorHistory.getLast() < wait));
    }

}