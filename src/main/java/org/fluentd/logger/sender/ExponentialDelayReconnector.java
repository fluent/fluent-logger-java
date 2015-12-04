package org.fluentd.logger.sender;

import java.util.LinkedList;

/**
 * Calculates exponential delay for reconnecting. The start delay is 50ms and exponentially grows to max 60 seconds in
 * function of the number of connection errors.
 */
public class ExponentialDelayReconnector implements Reconnector {

    private double waitMillis = 50; // Start wait is 50ms

    private double waitIncrRate = 1.5;

    private double waitMaxMillis = 60 * 1000; // Max wait is 1 minute

    private int waitMaxCount;

    private LinkedList<Long> errorHistory;

    public ExponentialDelayReconnector() {
        waitMaxCount = getWaitMaxCount();
        errorHistory = new LinkedList<Long>();
    }

    private int getWaitMaxCount() {
        double r = waitMaxMillis / waitMillis;
        for (int j = 1; j <= 100; j++) {
            if (r < waitIncrRate) {
                return j + 1;
            }
            r = r / waitIncrRate;
        }
        return 100;
    }

    public void addErrorHistory(long timestamp) {
        errorHistory.addLast(timestamp);
        if (errorHistory.size() > waitMaxCount) {
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

        double suppressMillis;
        if (size < waitMaxCount) {
            suppressMillis = waitMillis * Math.pow(waitIncrRate, size - 1);
        } else {
            suppressMillis = waitMaxMillis;
        }

        return (timestamp - errorHistory.getLast()) >= suppressMillis;
    }
}
