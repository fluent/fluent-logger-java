package org.fluentd.logger.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Handles constant delay for reconnecting. The default delay is 50 ms.
 */
public class ConstantDelayReconnector implements Reconnector {
    private static final Logger LOG = LoggerFactory.getLogger(ConstantDelayReconnector.class);

    private double wait = 50; // Default wait to 50 ms

    private static final int MAX_ERROR_HISTORY_SIZE = 100;

    private Deque<Long> errorHistory = new LinkedList<Long>();

    public ConstantDelayReconnector() {
        errorHistory = new LinkedList<Long>();
    }

    public ConstantDelayReconnector(int wait) {
        this.wait = wait;
        errorHistory = new LinkedList<Long>();
    }

    public void addErrorHistory(long timestamp) {
        errorHistory.addLast(timestamp);
        if (errorHistory.size() > MAX_ERROR_HISTORY_SIZE) {
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
        return errorHistory.isEmpty() || timestamp - errorHistory.getLast() >= wait;
    }
}
