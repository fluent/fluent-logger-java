package org.fluentd.logger;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluentd.logger.Sender;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;


public class TestSenderFluentdDownOperation {

    private static List<Sender.Event> fdo01 = new ArrayList<Sender.Event>();

    @Test
    public void testFluentdDownOperation01() throws Exception {
        int port = 24224;
        MessagePack msgpack = new MessagePack();
        BufferPacker packer = msgpack.createBufferPacker();
        long timestamp = System.currentTimeMillis();

        // start senders
        Sender sender = new Sender("localhost", port);
        Map<String, String> data = new HashMap<String, String>();
        data.put("t1k1", "t1v1");
        data.put("t1k2", "t1v2");
        sender.emit("tag.label1", timestamp, data);

        packer.write(new Sender.Event("tag.label1", timestamp, data));
        byte[] bytes1 = packer.toByteArray();
        assertArrayEquals(bytes1, sender.getBuffer());

        Map<String, String> data2 = new HashMap<String, String>();
        data2.put("t2k1", "t2v1");
        data2.put("t2k2", "t2v2");
        sender.emit("tag.label2", timestamp, data2);

        packer.write(new Sender.Event("tag.label2", timestamp, data2));
        byte[] bytes2 = packer.toByteArray();
        assertArrayEquals(bytes2, sender.getBuffer());

        // close sender sockets
        sender.close();
    }
}
