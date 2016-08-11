package org.fluentd.logger.sender;

import org.fluentd.logger.util.MockFluentd;
import org.fluentd.logger.util.MockFluentd.MockProcess;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.unpacker.Unpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class TestRawSocketSender {

    @Test
    public void testNormal01() throws Exception {
        // start mock fluentd
        int port = MockFluentd.randomPort();
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
        fluentd.waitUntilReady();

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

        // wait for unpacking event data on fluentd
        Thread.sleep(2000);

        // close mock server sockets
        fluentd.close();


        // check data
        assertEquals(2, elist.size());
        {
            Event e = elist.get(0);
            assertEquals("tag.label1", e.tag);
            assertEquals("t1v1", e.data.get("t1k1"));
            assertEquals("t1v2", e.data.get("t1k2"));
        }
        {
            Event e = elist.get(1);
            assertEquals("tag.label2", e.tag);
            assertEquals("t2v1", e.data.get("t2k1"));
            assertEquals("t2v2", e.data.get("t2k2"));
        }
    }



    @Test
    public void testNormal02() throws Exception {
        // start mock fluentd
        int port = MockFluentd.randomPort(); // Use a random port available
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
        fluentd.waitUntilReady();

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

        // wait for unpacking event data on fluentd
        Thread.sleep(2000);

        // close mock server sockets
        fluentd.close();


        // check data
        assertEquals(count, elist.size());
    }

    @Test
    public void testNormal03() throws Exception {
        // start mock fluentds
        final MockFluentd[] fluentds = new MockFluentd[2];
        final List[] elists = new List[2];
        final int[] ports = new int[2];
        ports[0] = MockFluentd.randomPort();
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
        fluentds[0].waitUntilReady();

        ports[1] = MockFluentd.randomPort();
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
        fluentds[1].waitUntilReady();

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

        // wait for unpacking event data on fluentd
        Thread.sleep(2000);

        // close mock server sockets
        fluentds[0].close();
        fluentds[1].close();


        // check data
        assertEquals(counts[0], elists[0].size());
        assertEquals(counts[1], elists[1].size());
    }

    @Test
    public void testTimeout() throws InterruptedException {
        final AtomicBoolean socketFinished = new AtomicBoolean(false);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                RawSocketSender socketSender = null;
                try {
                    // try to connect to test network
                    socketSender = new RawSocketSender("192.0.2.1", 24224, 200, 8 * 1024);
                }
                finally {
                    if (socketSender != null) {
                        socketSender.close();
                    }
                    socketFinished.set(true);
                }
            }
        });

        while(!socketFinished.get())
            Thread.yield();

        assertTrue(socketFinished.get());
        executor.shutdownNow();
    }

    @Test
    public void testBufferingAndResending() throws InterruptedException, IOException {
        final ConcurrentLinkedQueue<Event> readEvents = new ConcurrentLinkedQueue<Event>();
        final CountDownLatch countDownLatch = new CountDownLatch(4);
        int port = MockFluentd.randomPort();
        MockProcess mockProcess = new MockFluentd.MockProcess() {
            public void process(MessagePack msgpack, Socket socket) throws IOException {
                BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                try {
                    Unpacker unpacker = msgpack.createUnpacker(in);
                    while (true) {
                        Event e = unpacker.read(Event.class);
                        readEvents.add(e);
                        countDownLatch.countDown();
                    }
                } catch (EOFException e) {
                    // e.printStackTrace();
                }
            }
        };

        MockFluentd fluentd = new MockFluentd(port, mockProcess);
        fluentd.start();
        fluentd.waitUntilReady();

        Sender sender = new RawSocketSender("localhost", port);
        assertFalse(sender.isConnected());
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("key0", "v0");
        sender.emit("tag0", data);
        assertTrue(sender.isConnected());

        // close fluentd to make the next sending failed
        TimeUnit.MILLISECONDS.sleep(500);

        fluentd.closeClientSockets();

        TimeUnit.MILLISECONDS.sleep(500);

        data = new HashMap<String, Object>();
        data.put("key0", "v1");
        sender.emit("tag0", data);
        assertFalse(sender.isConnected());

        // wait to avoid the suppression of reconnection
        TimeUnit.MILLISECONDS.sleep(500);

        data = new HashMap<String, Object>();
        data.put("key0", "v2");
        sender.emit("tag0", data);

        data = new HashMap<String, Object>();
        data.put("key0", "v3");
        sender.emit("tag0", data);

        countDownLatch.await(500, TimeUnit.MILLISECONDS);

        sender.close();

        fluentd.close();

        assertEquals(4, readEvents.size());

        Event event = readEvents.poll();
        assertEquals("tag0", event.tag);
        assertEquals(1, event.data.size());
        assertTrue(event.data.keySet().contains("key0"));
        assertTrue(event.data.values().contains("v0"));

        event = readEvents.poll();
        assertEquals("tag0", event.tag);
        assertEquals(1, event.data.size());
        assertTrue(event.data.keySet().contains("key0"));
        assertTrue(event.data.values().contains("v1"));

        event = readEvents.poll();
        assertEquals("tag0", event.tag);
        assertEquals(1, event.data.size());
        assertTrue(event.data.keySet().contains("key0"));
        assertTrue(event.data.values().contains("v2"));

        event = readEvents.poll();
        assertEquals("tag0", event.tag);
        assertEquals(1, event.data.size());
        assertTrue(event.data.keySet().contains("key0"));
        assertTrue(event.data.values().contains("v3"));
    }

    @Test
    public void testReconnectAfterBufferFull() throws Exception {
        final CountDownLatch bufferFull = new CountDownLatch(1);

        // start mock fluentd
        int port = MockFluentd.randomPort(); // Use a random port available
        final List<Event> elist = new ArrayList<Event>();
        final MockFluentd fluentd = new MockFluentd(port, new MockFluentd.MockProcess() {
            public void process(MessagePack msgpack, Socket socket) throws IOException {
                try {
                    BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                    Unpacker unpacker = msgpack.createUnpacker(in);
                    while (true) {
                        Event e = unpacker.read(Event.class);
                        elist.add(e);
                    }
                } catch (EOFException e) {
                    // ignore
                } finally {
                    socket.close();
                }
            }
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    bufferFull.await(20, TimeUnit.SECONDS);
                    fluentd.start();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // start senders
        Sender sender = new RawSocketSender("localhost", port);
        String tag = "tag";
        int i;
        for (i = 0; i < 1000000; i++) {     // Enough to fill the sender's buffer
            Map<String, Object> record = new HashMap<String, Object>();
            record.put("num", i);
            record.put("str", "name" + i);

            if (bufferFull.getCount() > 0) {
                // Fill the sender's buffer
                if (!sender.emit(tag, record)) {
                    // Buffer full. Need to recover the fluentd
                    bufferFull.countDown();
                    Thread.sleep(2000);
                }
            }
            else {
                // Flush the sender's buffer after the fluentd starts
                sender.emit(tag, record);
                break;
            }
        }

        // close sender sockets
        sender.close();

        // wait for unpacking event data on fluentd
        Thread.sleep(2000);

        // close mock server sockets
        fluentd.close();

        // check data
        assertEquals(0, bufferFull.getCount());
        assertEquals(i, elist.size());
    }

    @Test
    public void testBufferOverflow() throws Exception {
        // start mock fluentd
        int port = MockFluentd.randomPort();
        MockFluentd fluentd = new MockFluentd(port, new MockFluentd.MockProcess() {
            public void process(MessagePack msgpack, Socket socket) throws IOException {
                BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                try {
                    Unpacker unpacker = msgpack.createUnpacker(in);
                    while (true) {
                        unpacker.read(Event.class);
                    }
                    //socket.close();
                } catch (EOFException e) {
                    // ignore
                }
            }
        });
        fluentd.start();

        // start senders
        Sender sender = new RawSocketSender("localhost", port, 3000, 256);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("large", randomString(512));
        boolean success = sender.emit("tag.label1", data);
        assertFalse(success);

        // close sender sockets
        sender.close();
        // close mock server sockets
        fluentd.close();
    }

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ ";

    private String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);

        Random rnd = new Random();
        for (int i = 0; i < len; i++) {
            if (i != 0 && i % 128 == 0) {
                sb.append("\r\n");
            }

            sb.append(CHARS.charAt(rnd.nextInt(CHARS.length())));
        }

        return sb.toString();
    }
}
