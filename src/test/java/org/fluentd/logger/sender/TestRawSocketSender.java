package org.fluentd.logger.sender;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluentd.logger.util.MockFluentd;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.unpacker.Unpacker;


public class TestRawSocketSender {

    @Test
    public void testNormal01() throws Exception {
        // start mock fluentd
        int port = 25225;
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

        // start senders
        Sender sender = new RawSocketSender("localhost", port);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("t1k1", "t1v1");
        data.put("t1k2", "t1v2");
        sender.emit("tag.label1", data);

        Map<String, Object> data2 = new HashMap<String, Object>();
        data2.put("t2k1", "t2v1");
        data2.put("t2k2", "t2v2");
        sender.emit("tag.label2", data2);

        // close sender sockets
        sender.close();

        // close mock server sockets
        fluentd.close();

        // wait for unpacking event data on fluentd
        Thread.sleep(1000);

        // check data
        assertEquals(2, elist.size());
        {
            Event e = elist.get(0);
            assertEquals("tag.label1", e.tag);
            assertEquals("t1v1", ((Map<?, ?>)e.data).get("t1k1"));
            assertEquals("t1v2", ((Map<?, ?>)e.data).get("t1k2"));
        }
        {
            Event e = elist.get(1);
            assertEquals("tag.label2", e.tag);
            assertEquals("t2v1", ((Map<?, ?>)e.data).get("t2k1"));
            assertEquals("t2v2", ((Map<?, ?>)e.data).get("t2k2"));
        }
    }

    @Test
    public void testNormal02() throws Exception {
        // start mock fluentd
        int port = 25226;
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

        // start senders
        Sender sender = new RawSocketSender("localhost", port);
        int count = 10000;
        for (int i = 0; i < count; i++) {
            String tag = "tag:i";
            Map<String, Object> record = new HashMap<String, Object>();
            record.put("i", i);
            record.put("n", "name:" + i);
            sender.emit(tag, record);
        }

        // close sender sockets
        sender.close();

        // close mock server sockets
        fluentd.close();

        // wait for unpacking event data on fluentd
        Thread.sleep(1000);

        // check data
        assertEquals(count, elist.size());
    }

    @Test
    public void testNormal03() throws Exception {
        // start mock fluentds
        final MockFluentd[] fluentds = new MockFluentd[2];
        final List[] elists = new List[2];
        final int[] ports = new int[2];
        ports[0] = 25227;
        RawSocketSender rawSocketSender = new RawSocketSender("localhost", ports[0]);   // it should be failed to connect to fluentd
        elists[0] = new ArrayList<Event>();
        fluentds[0] = new MockFluentd(ports[0], new MockFluentd.MockProcess() {
            public void process(MessagePack msgpack, Socket socket) throws IOException {
                BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                try {
                    Unpacker unpacker = msgpack.createUnpacker(in);
                    while (true) {
                        Event e = unpacker.read(Event.class);
                        elists[0].add(e);
                    }
                    //socket.close();
                } catch (EOFException e) {
                    // ignore
                }
            }
        });
        fluentds[0].start();
        ports[1] = 25228;
        elists[1] = new ArrayList<Event>();
        fluentds[1] = new MockFluentd(ports[1], new MockFluentd.MockProcess() {
            public void process(MessagePack msgpack, Socket socket) throws IOException {
                BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                try {
                    Unpacker unpacker = msgpack.createUnpacker(in);
                    while (true) {
                        Event e = unpacker.read(Event.class);
                        elists[1].add(e);
                    }
                    //socket.close();
                } catch (EOFException e) {
                    // ignore
                }
            }
        });
        fluentds[1].start();

        // start senders
        Sender[] senders = new Sender[2];
        int[] counts = new int[2];
        senders[0] = rawSocketSender;
        counts[0] = 10000;
        for (int i = 0; i < counts[0]; i++) {
            String tag = "tag:i";
            Map<String, Object> record = new HashMap<String, Object>();
            record.put("i", i);
            record.put("n", "name:" + i);
            senders[0].emit(tag, record);
        }
        senders[1] = new RawSocketSender("localhost", ports[1]);
        counts[1] = 10000;
        for (int i = 0; i < counts[1]; i++) {
            String tag = "tag:i";
            Map<String, Object> record = new HashMap<String, Object>();
            record.put("i", i);
            record.put("n", "name:" + i);
            senders[1].emit(tag, record);
        }

        // close sender sockets
        senders[0].close();
        senders[1].close();

        // close mock server sockets
        fluentds[0].close();
        fluentds[1].close();

        // wait for unpacking event data on fluentd
        Thread.sleep(1000);

        // check data
        assertEquals(counts[0], elists[0].size());
        assertEquals(counts[1], elists[1].size());
    }

    @Test
    public void testNormal04_SOMapInSOMap() throws Exception {
        List<Event> elist = new ArrayList<Event>();
        int port = 25225;
        MockFluentd mock = createMock(elist, port);
        try{
            // start senders
            Sender sender = new RawSocketSender("localhost", port);
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("t1k1", "t1v1");
            data.put("t1k2", "t1v2");
            Map<String, Object> data2 = new HashMap<String, Object>();
            data2.put("t2k1", "t2v1");
            data2.put("t2k2", "t2v2");
            data.put("data2", data2);
            sender.emit("tag.label1", data);
            // close sender sockets
            sender.close();
        } finally{
            // close mock server sockets
            mock.close();
        }

        // wait for unpacking event data on fluentd
        Thread.sleep(1000);

        // check data
        assertEquals(1, elist.size());
        {
            Event e = elist.get(0);
            assertEquals("tag.label1", e.tag);
            assertEquals("t1v1", ((Map<?, ?>)e.data).get("t1k1"));
            assertEquals("t1v2", ((Map<?, ?>)e.data).get("t1k2"));
            Map<?, ?> data2 = (Map<?, ?>)((Map<?, ?>)e.data).get("data2");
            assertEquals("t2v1", data2.get("t2k1"));
            assertEquals("t2v2", data2.get("t2k2"));
        }
    }

    @Test
    public void testNormal05_WrappersAndString() throws Exception {
        List<Event> elist = new ArrayList<Event>();
        int port = 25225;
        MockFluentd mock = createMock(elist, port);
        try{
            // start senders
            Sender sender = new RawSocketSender("localhost", port);
            sender.emit("tag.label1", (short)1);
            sender.emit("tag.label1", 2);
            sender.emit("tag.label1", 3L);
            sender.emit("tag.label1", 4.4f);
            sender.emit("tag.label1", 5.5);
            sender.emit("tag.label1", true);
            sender.emit("tag.label1", 'A');
            sender.emit("tag.label1", "hello");
            // close sender sockets
            sender.close();
        } finally{
            // close mock server sockets
            mock.close();
        }

        // wait for unpacking event data on fluentd
        Thread.sleep(1000);

        // check data
        assertEquals(8, elist.size());
        {
            int i = 0;
            assertEquals((long)1, elist.get(i++).data);
            assertEquals((long)2, elist.get(i++).data);
            assertEquals((long)3, elist.get(i++).data);
            assertEquals(4.4, (Double)elist.get(i++).data, 0.001);
            assertEquals(5.5, (Double)elist.get(i++).data, 0.001);
            assertEquals(true, elist.get(i++).data);
            assertEquals((long)'A', elist.get(i++).data);
            assertEquals("hello", elist.get(i++).data);
        }
    }

    public static class Msg{
        public Msg() {
        }
        public Msg(String name, int age, boolean live) {
            this.name = name;
            this.age = age;
            this.live = live;
        }
        public int getAge() {
            return age;
        }
        public String getName() {
            return name;
        }
        public boolean isLive() {
            return live;
        }

        private String name;
        private int age;
        private boolean live;
    }
    @Test
    public void testNormal06_Object() throws Exception {
        List<Event> elist = new ArrayList<Event>();
        int port = 25225;
        EventTemplate.INSTANCE = new MapStyleEventTemplate();
        MockFluentd mock = createMock(elist, port);
        try{
            Sender sender = new RawSocketSender("localhost", port);
            sender.emit("tag.label1", new Msg("suzuki", 30, true));
            sender.close();
        } finally{
            mock.close();
        }

        Thread.sleep(1000);

        // check data
        assertEquals(1, elist.size());
        {
            Map<?, ?> m = (Map<?, ?>)elist.get(0).data;
            assertEquals("suzuki", m.get("name"));
            assertEquals((long)30, m.get("age"));
            assertEquals(true, m.get("live"));
        }
    }

    private MockFluentd createMock(final List<Event> elist, int port) throws IOException{
        // start mock fluentd
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
        return fluentd;
    }
}
