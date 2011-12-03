package org.fluentd.logger.sender;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluentd.logger.MockFluentd;
import org.fluentd.logger.MockFluentd.MockProcess;
import org.fluentd.logger.sender.RawSocketSender;
import org.fluentd.logger.sender.Sender;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.unpacker.Unpacker;

public class TestSenderNormalOperation {

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
    }
}
