package org.fluentd.logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fluentd.logger.sender.Event;
import org.fluentd.logger.sender.NullSender;
import org.fluentd.logger.sender.Sender;
import org.junit.Ignore;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.unpacker.Unpacker;

public class TestFluentLoggerNormalOperation {

    private static List<Event> no01 = new ArrayList<Event>();

    @Test
    public void testNormalOperation01() throws Exception {
        int port = 25225;

        // start mock server
        MockFluentd server = new MockFluentd(port, new MockFluentd.MockProcess() {
            public void process(MessagePack msgpack, Socket socket) throws IOException {
                BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                Unpacker unpacker = msgpack.createUnpacker(in);
                no01.add(unpacker.read(Event.class));
                no01.add(unpacker.read(Event.class));
                socket.close();
            }
        });
        server.start();

        // create logger object
        FluentLogger logger = FluentLogger.getLogger("tag", "localhost", port);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("t1k1", "t1v1");
        data.put("t1k2", "t1v2");
        logger.log("label1", data);

        Map<String, Object> data2 = new HashMap<String, Object>();
        data2.put("t2k1", "t2v1");
        data2.put("t2k2", "t2v2");
        logger.log("label2", data2);

        // close mock server sockets
        server.close();

        // sleep a little bit
        Thread.sleep(100);

        // check data
        assertEquals(2, no01.size());
        {
            Event e = no01.get(0);
            assertEquals("tag.label1", e.tag);
            assertEquals("t1v1", e.data.get("t1k1"));
            assertEquals("t1v2", e.data.get("t1k2"));
        }
        {
            Event e = no01.get(1);
            assertEquals("tag.label2", e.tag);
            assertEquals("t2v1", e.data.get("t2k1"));
            assertEquals("t2v2", e.data.get("t2k2"));
        }

        // close and delete
        FluentLogger.close();
    }

    @Ignore @Test
    public void testNormalOperation02() throws Exception {
        // create logger object
        FluentLogger logger = FluentLogger.getLogger("tag");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("t1k1", "t1v1");
        data.put("t1k2", "t1v2");
        logger.log("label1", data);

        Map<String, Object> data2 = new HashMap<String, Object>();
        data2.put("t2k1", "t2v1");
        data2.put("t2k2", "t2v2");
        logger.log("label2", data2);

        logger.flush();

        // close and delete
        FluentLogger.close();
    }

    @Test
    public void testWithNullSender() throws Exception {
        // use NullSender
        Properties props = System.getProperties();
        props.setProperty(Config.FLUENT_SENDER_CLASS, NullSender.class.getName());

        FluentLogger flog = FluentLogger.getLogger("tag");
        {
            Map<String, Object> data = new HashMap<String, Object>();
            assertTrue(flog.log("label", data));
        }
        {
            Map<String, Object> data = new HashMap<String, Object>();
            assertTrue(flog.log("label", data, System.currentTimeMillis() / 1000));
        }

        Sender sender = flog.sender;
        assertTrue(sender instanceof NullSender);

        // close and delete
        FluentLogger.close();

        props.remove(Config.FLUENT_SENDER_CLASS);
    }

    @Test
    public void testClose() throws Exception {
        // use NullSender
        Properties props = System.getProperties();
        props.setProperty(Config.FLUENT_SENDER_CLASS, NullSender.class.getName());

        // create logger objects
        FluentLogger.getLogger("tag1");
        FluentLogger.getLogger("tag2");
        FluentLogger.getLogger("tag3");

        Map<String, FluentLogger> loggers;
        {
            loggers = FluentLogger.getLoggers();
            assertEquals(3, loggers.size());
        }

        // close and delete
        FluentLogger.close();
        {
            loggers = FluentLogger.getLoggers();
            assertEquals(0, loggers.size());
        }

        props.remove(Config.FLUENT_SENDER_CLASS);
    }
}
