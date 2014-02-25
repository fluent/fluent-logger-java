package org.fluentd.logger.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * Calcurate exponential delay for reconnecting
 */
public class ExponentialDelayReconnector implements Reconnector {
    private static final Logger LOG = LoggerFactory.getLogger(ExponentialDelayReconnector.class);

    private double wait = 0.5;

    private double waitIncrRate = 1.5;

    private double waitMax = 60;

    private int waitMaxCount;

    private LinkedList<Long> errorHistory;

    public ExponentialDelayReconnector() {
        waitMaxCount = getWaitMaxCount();
        errorHistory = new LinkedList<Long>();
    }

    private int getWaitMaxCount() {
        double r = waitMax / wait;
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

        double suppressSec;
        if (size < waitMaxCount) {
            suppressSec = wait * Math.pow(waitIncrRate, size - 1);
        } else {
            suppressSec = waitMax;
        }

        return (!(timestamp - errorHistory.getLast() < suppressSec));
    }
}
