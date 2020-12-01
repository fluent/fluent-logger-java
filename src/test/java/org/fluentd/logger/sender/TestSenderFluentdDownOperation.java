package org.fluentd.logger.sender;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.fluentd.logger.sender.RawSocketSender;
import org.fluentd.logger.sender.Sender;
import org.fluentd.logger.util.MockFluentd;
import org.fluentd.logger.util.MockFluentd.MockProcess;
import org.junit.Ignore;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;

public class TestSenderFluentdDownOperation {

    /**
     * if Sender object was created when fluentd doesn't work, ...
     */
    @Test @Ignore
    public void testFluentdDownOperation01() throws Exception {
        int port = 25225;
        MessagePack msgpack = new MessagePack();
        msgpack.register(Event.class, Event.EventTemplate.INSTANCE);
        BufferPacker packer = msgpack.createBufferPacker();
        long timestamp = System.currentTimeMillis() / 1000;

        // start senders
        RawSocketSender sender = new RawSocketSender("localhost", port);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("t1k1", "t1v1");
        data.put("t1k2", "t1v2");
        sender.emit("tag.label1", timestamp, data);

        packer.write(new Event("tag.label1", timestamp, data));
        byte[] bytes1 = packer.toByteArray();
        assertArrayEquals(bytes1, sender.getBuffer());

        Map<String, Object> data2 = new LinkedHashMap<String, Object>();
        data2.put("t2k1", "t2v1");
        data2.put("t2k2", "t2v2");
        sender.emit("tag.label2", timestamp, data2);

        packer.write(new Event("tag.label2", timestamp, data2));
        byte[] bytes2 = packer.toByteArray();
        assertArrayEquals(bytes2, sender.getBuffer());

        // close sender sockets
        sender.close();
    }

    /**
     * if emit method was invoked when fluentd doesn't work, ...
     */
    @Ignore @Test
    public void testFluentdDownOperation02()throws Exception {
        int port = 25225;
        MessagePack msgpack = new MessagePack();
        msgpack.register(Event.class, Event.EventTemplate.INSTANCE);
        BufferPacker packer = msgpack.createBufferPacker();
        long timestamp = System.currentTimeMillis();

        // start mock server
        MockFluentd server = new MockFluentd(port, new MockFluentd.MockProcess() {
            public void process(MessagePack msgpack, Socket socket) throws IOException {
                System.out.println("server closing");
                socket.close();
                System.out.println("server closed");
            }
        });
        server.start();

        // start senders
        RawSocketSender sender = new RawSocketSender("localhost", port);

        // server close
        server.close();

        // sleep a little bit
        Thread.sleep(1000);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("t1k1", "t1v1");
        data.put("t1k2", "t1v2");
        for (int i = 0; i < 3; ++i) {
        System.out.println("sender emit");
        sender.emit("tag.label1", data);
        }

        packer.write(new Event("tag.label1", timestamp, data));
        byte[] bytes1 = packer.toByteArray();
        assertArrayEquals(bytes1, sender.getBuffer());

        Map<String, Object> data2 = new LinkedHashMap<String, Object>();
        data2.put("t2k1", "t2v1");
        data2.put("t2k2", "t2v2");
        sender.emit("tag.label2", data2);

        packer.write(new Event("tag.label2", timestamp, data2));
        byte[] bytes2 = packer.toByteArray();
        assertArrayEquals(bytes2, sender.getBuffer());

        // close sender sockets
        sender.close();
    }
}
