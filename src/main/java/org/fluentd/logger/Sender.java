//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 Muga Nishizawa
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package org.fluentd.logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.MessagePack;
import org.msgpack.annotation.Message;
import org.slf4j.LoggerFactory;


public class Sender {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Sender.class);

    @Message
    public static class Event {
        public String tag;

        public long timestamp;

        public Map<String, String> data;

        public Event() {
        }

        @Override
        public String toString() {
            return String.format("Event[tag=%s,timestamp=%d,data=%s]",
                    new Object[] { tag, timestamp, data.toString() });
        }
    }

    private MessagePack msgpack;

    private String tagPrefix;

    private SocketAddress server;

    private Socket socket;

    private String name;

    private int timeout;

    private BufferedOutputStream out;

    private ByteBuffer pendings;

    public Sender(String tag) {
        this(tag, "localhost", 24224);
    }

    public Sender(String tag, String host, int port) {
        this(tag, host, port, 3 * 1000, 8 * 1024 * 1024);
    }

    public Sender(String tagPrefix, String host, int port, int timeout, int bufferCapacity) {
        this.tagPrefix = tagPrefix;
        msgpack = new MessagePack();
        server = new InetSocketAddress(host, port);
        name = String.format("%s_%s_%d", new Object[] { tagPrefix, host, port });
        open();
        pendings = ByteBuffer.allocate(bufferCapacity);
    }

    public String getName() {
        return name;
    }

    private void open() {
        try {
            connect();
        } catch (IOException e) {
            LOG.error("Failed to connect fluentd: " + getName(), e);
            LOG.error("Connection will be retried");
            close();
        }
    }

    private void connect() throws IOException {
        socket = new Socket();
        socket.connect(server);
        // the timeout value to be used in milliseconds
        socket.setSoTimeout(timeout);
        out = new BufferedOutputStream(socket.getOutputStream());
    }

    private void reconnect() throws IOException {
        if (socket.isClosed() || (!socket.isConnected())) {
            close();
            connect();
        }
    }

    public void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) { // ignore
            } finally {
                out = null;
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) { // ignore
            } finally {
                socket = null;
            }
        }
    }

    public void emit(String label, Map<String, String> data) {
        String tag = tagPrefix + "." + label;
        long timestamp = System.currentTimeMillis();

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Create event=[tag=%s,curtime=%d,data=%s]",
                    new Object[] { tag, timestamp, data.toString() }));
        }

        Event event = null;
        byte[] bytes = null;
        try {
            // create event
            event = new Event();
            event.tag = tag;
            event.timestamp = timestamp;
            event.data = data;

            // serialize tag, timestamp and data
            bytes = msgpack.write(event);
        } catch (IOException e) {
            LOG.error("Cannot serialize data: " + event, e);
        }

        // send serialized data
        if (bytes != null) {
            send(bytes);
        }
    }

    private synchronized void send(byte[] bytes) {
        // check pending buffer
        int pos = pendings.position();
        if (pos > 0) {
            byte[] b = new byte[pos + bytes.length];
            pendings.get(b, 0, pos);
            System.arraycopy(b, pos, bytes, 0, bytes.length);
            bytes = b;
        }

        try {
            // check whether connection is established or not
            reconnect();

            out.write(bytes);
            out.flush();
            pendings.clear();
        } catch (IOException e) {
            // check overflow of pending buffer
            if (bytes.length > pendings.capacity()) {
                LOG.error("FluentLogger: Cannot send logs to " + getName(), e);
                pendings.clear();
            } else {
                pendings.clear();
                pendings.put(bytes);
            }

            // close socket
            close();
        }
    }

    // TODO: main method must be deleted later
    public static void main(String[] args) throws Exception {
        Sender sender = new Sender("tag", "localhost", 24224);
        Map<String, String> data = new HashMap<String, String>();
        data.put("t1k1", "t1v1");
        data.put("t1k2", "t1v2");
        sender.emit("label1", data);

        Map<String, String> data2 = new HashMap<String, String>();
        data2.put("t2k1", "t2v1");
        data2.put("t2k2", "t2v2");
        sender.emit("label2", data2);
    }
}
