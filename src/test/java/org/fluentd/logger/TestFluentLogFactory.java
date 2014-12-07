package org.fluentd.logger;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestFluentLogFactory {
    private FluentLoggerFactory loggerFactory;

    @Before
    public void setup() {
        loggerFactory = new FluentLoggerFactory();
    }

    @Test
    public void testGetLogger() {
        FluentLogger loggerA0 = loggerFactory.getLogger("tagprefix_a");
        FluentLogger loggerA1 = loggerFactory.getLogger("tagprefix_a");
        FluentLogger loggerB0 = loggerFactory.getLogger("tagprefix_b");
        FluentLogger loggerA_lh0 = loggerFactory.getLogger("tagprefix_a", "localhost", 1234);
        FluentLogger loggerA_lh1 = loggerFactory.getLogger("tagprefix_a", "127.0.0.1", 1234);
        assertTrue(loggerA0 == loggerA1);
        assertTrue(loggerA0 != loggerB0);
        assertTrue(loggerA0 != loggerA_lh0);
        assertTrue(loggerA_lh0 != loggerA_lh1);
    }

    @Test
    public void testItHoldsLoggersOverGC() throws InterruptedException {
        ArrayList<FluentLogger> loggers = new ArrayList<FluentLogger>();
        for(int i=0; i<100; i++) {
            loggers.add(loggerFactory.getLogger("testtag" + i, "localhost", 999));
        }
        System.gc();
        Thread.sleep(1000);
        assertEquals(loggers.size(), loggerFactory.getLoggers().size());

        FluentLogger head = loggers.get(0);
        FluentLogger tail = loggers.get(loggers.size() - 1);
        loggers.clear();
        System.gc();
        Thread.sleep(1000);
        assertEquals(2, loggerFactory.getLoggers().size());
    }
}
