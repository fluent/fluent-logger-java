package org.fluentd.logger.sender;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestNullSender {

    @Test
    public void testNormal() throws Exception {
        //public NullSender(String host, int port, int timeout, int bufferCapacity) {
        NullSender sender = new NullSender("localhost", 24224, 3 * 1000, 1 * 1024 * 1024);

        // emit
        {
            Map<String, Object> data = new HashMap<String, Object>();
            assertTrue(sender.emit("test", data));
        }
        {
            Map<String, Object> data = new HashMap<String, Object>();
            assertTrue(sender.emit("test", System.currentTimeMillis() / 1000, data));
        }

        // flush
        sender.flush();
        assertTrue(true);

        // close
        sender.close();
    }
}
