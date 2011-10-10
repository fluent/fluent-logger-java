package org.fluentd.logger;

import java.util.HashMap;
import java.util.Map;

import org.fluentd.logger.Sender;
import org.junit.Test;

public class TestSimple {

    @Test
    public void testSimple() throws Exception {
	// start fluentd
	Sender sender = new Sender("debug", "localhost", 24224);
	Map<String, String> data = new HashMap<String, String>();
	data.put("k1", "v1");
	data.put("k2", "v2");
	sender.emit("test", data);
	sender.emit("test", data);
	sender.close();
    }
}
