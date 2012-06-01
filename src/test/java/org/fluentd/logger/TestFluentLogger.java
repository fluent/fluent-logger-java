package org.fluentd.logger;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fluentd.logger.sender.Event;
import org.fluentd.logger.sender.NullSender;
import org.fluentd.logger.util.MockFluentd;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.unpacker.Unpacker;

public class TestFluentLogger {

    @Test
    public void testNormal01() throws Exception {
        // start mock fluentd
        int port = 25225;
        String host = "localhost";
        final List<Event> elist = new ArrayList<Event>();
        MockFluentd fluentd = new MockFluentd(port, new MockFluentd.MockProcess() {
            public void process(MessagePack msgpack, Socket socket) throws IOException {
                BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                try {
                    Unpacker unpacker = msgpack.createUnpacker(in);
                    while (true) {
                        Event e = unpacker.read(Event.class);
                        elist.add(e);
                    }
                    //socket.close();
                } catch (EOFException e) {
                    // ignore
                }
            }
        });
        fluentd.start();

        // start loggers
        FluentLogger logger = FluentLogger.getLogger("testtag", host, port);
        {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("k1", "v1");
            data.put("k2", "v2");
            logger.log("test01", data);
        }
        {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("k3", "v3");
            data.put("k4", "v4");
            logger.log("test01", data);
        }

        // close loggers
        logger.close();

        // close mock fluentd
        fluentd.close();

        // wait for unpacking event data on fluentd
        Thread.sleep(1000);

        // check data
        assertEquals(2, elist.size());
        assertEquals("testtag.test01", elist.get(0).tag);
        assertEquals("testtag.test01", elist.get(1).tag);
    }

    @Test
    public void testNormal02() throws Exception {
        // start mock fluentd
        int port = 25225;
        String host = "localhost";
        final List[] elists = new List[2];
        elists[0] = new ArrayList<Event>();
        elists[1] = new ArrayList<Event>();
        MockFluentd fluentd = new MockFluentd(port, new MockFluentd.MockProcess() {
            public void process(MessagePack msgpack, Socket socket) throws IOException {
                BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                try {
                    Unpacker unpacker = msgpack.createUnpacker(in);
                    while (true) {
                        Event e = unpacker.read(Event.class);
                        if (e.tag.startsWith("testtag00")) {
                            elists[0].add(e); // testtag00
                        } else {
                            elists[1].add(e); // testtag01
                        }
                    }
                    //socket.close();
                } catch (EOFException e) {
                    // ignore
                }
            }
        });
        fluentd.start();

        // start loggers
        FluentLogger[] loggers = new FluentLogger[2];
        int[] counts = new int[2]; 
        loggers[0] = FluentLogger.getLogger("testtag00", host, port);
        {
            for (int i = 0; i < counts[0]; i++) {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("k1", "v1");
                data.put("k2", "v2");
                loggers[0].log("test00", data);
            }
        }
        loggers[1] = FluentLogger.getLogger("testtag01", host, port);
        {
            for (int i = 0; i < counts[1]; i++) {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("k3", "v3");
                data.put("k4", "v4");
                loggers[1].log("test01", data);
            }
        }

        // close loggers
        FluentLogger.closeAll();

        // close mock fluentd
        fluentd.close();

        // wait for unpacking event data on fluentd
        Thread.sleep(1000);

        // check data
        assertEquals(counts[0], elists[0].size());
        for (Object obj : elists[0]) {
            Event e = (Event) obj;
            assertEquals("testtag00.test00", e.tag);
        }
        assertEquals(counts[1], elists[1].size());
        for (Object obj : elists[1]) {
            Event e = (Event) obj;
            assertEquals("testtag00.test00", e.tag);
        }
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
        FluentLogger.closeAll();
        {
            loggers = FluentLogger.getLoggers();
            assertEquals(0, loggers.size());
        }

        props.remove(Config.FLUENT_SENDER_CLASS);
    }
}
