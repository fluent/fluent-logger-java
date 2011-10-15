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
import java.util.LinkedList;
import java.util.Map;

import org.msgpack.MessagePack;
import org.msgpack.annotation.Message;
import org.slf4j.LoggerFactory;


class Sender {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Sender.class);

    @Message
    public static class Event {
        public String tag;

        public long timestamp;

        public Map<String, String> data;

        public Event() {
        }

        public Event(String tag, long timestamp, Map<String, String> data) {
            this.tag = tag;
            this.timestamp = timestamp;
            this.data = data;
        }

        @Override
        public String toString() {
            return String.format("Event[tag=%s,timestamp=%d,data=%s]",
                    new Object[] { tag, timestamp, data.toString() });
        }
    }

    /**
     * Calcurate exponential delay for reconnecting
     */
    private static class ExponentialDelayReconnection {
        private double wait = 0.5;

        private double waitIncrRate = 1.5;

        private double waitMax = 60;

        private int waitMaxCount;

        private LinkedList<Long> errorHist;

        public ExponentialDelayReconnection() {
            waitMaxCount = getWaitMaxCount();
            errorHist = new LinkedList<Long>();
        }

        private int getWaitMaxCount() {
            double r = waitMax / wait;
            for (int j = 1; j <= 100; j++) {
                if (r < waitIncrRate) {
                    return j + 1;
                }
                r = r / waitIncrRate;
            }
            return 100;
        }

        public void addErrorHistory(long timestamp) {
            errorHist.addLast(timestamp);
            if (errorHist.size() > waitMaxCount) {
                errorHist.removeFirst();
            }
        }

        public void clearErrorHistory() {
            errorHist.clear();
        }

        public boolean enableReconnection(long timestamp) {
            int size = errorHist.size();
            if (size == 0) {
                return true;
            }

            double suppressSec;
            if (size < waitMaxCount) {
                suppressSec = wait * Math.pow(waitIncrRate, size - 1);
            } else {
                suppressSec = waitMax;
            }

            return (! (timestamp - errorHist.getLast() < suppressSec));
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

    private ExponentialDelayReconnection reconnector;

    public Sender(String tag) {
        this(tag, "localhost", 24224);
    }

    public Sender(String tag, String host, int port) {
        this(tag, host, port, 3 * 1000, 8 * 1024 * 1024);
    }

    public Sender(String tagPrefix, String host, int port, int timeout, int bufferCapacity) {
        this.tagPrefix = tagPrefix;
        msgpack = new MessagePack();
        pendings = ByteBuffer.allocate(bufferCapacity);
        server = new InetSocketAddress(host, port);
        name = String.format("%s_%s_%d_%d_%d", new Object[] { tagPrefix, host, port, timeout, bufferCapacity });
        reconnector = new ExponentialDelayReconnection();
        open();
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
        try {
            socket = new Socket();
            socket.connect(server);
            // the timeout value to be used in milliseconds
            socket.setSoTimeout(timeout);
            out = new BufferedOutputStream(socket.getOutputStream());
            reconnector.clearErrorHistory();
        } catch (IOException e) {
            reconnector.addErrorHistory(System.currentTimeMillis());
            throw e;
        }
    }

    private void reconnect() throws IOException {
        if (socket.isClosed() || (!socket.isConnected())) {
            close();
            connect();
        }
    }

    public void close() {
        int pos = pendings.position();
        if (pos > 0) {
            byte[] b = new byte[pos];
            pendings.get(b, 0, pos);
            send(b);
        }

        // close output stream
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) { // ignore
            } finally {
                out = null;
            }
        }

        // close socket
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
        // create event
        Event event = new Event(tagPrefix + "." + label, System.currentTimeMillis(), data);

        if (LOG.isDebugEnabled()) { // for debug
            LOG.debug(String.format("Created %s", new Object[] { event }));
        }

        byte[] bytes = null;
        try {
            // serialize tag, timestamp and data
            bytes = msgpack.write(event);
        } catch (IOException e) {
            LOG.error("Cannot serialize event: " + event, e);
        }

        // send serialized data
        if (bytes != null) {
            send(bytes);
        }
    }

    private synchronized void send(byte[] bytes) {
        // buffering
        appendBuffer(bytes);

        try {
            // suppress reconnection burst
            if (!reconnector.enableReconnection(System.currentTimeMillis())) {
                return;
            }

            // check whether connection is established or not
            reconnect();

            // write data
            out.write(getBuffer());
            out.flush();

            clearBuffer();
        } catch (IOException e) {
            // close socket
            close();
        }
    }

    private void appendBuffer(byte[] bytes) {
        if (pendings.position() + bytes.length > pendings.capacity()) {
            LOG.error("FluentLogger: Cannot send logs to " + getName());
            pendings.clear();
        }
        pendings.put(bytes);
    }

    private byte[] getBuffer() {
        int len = pendings.position();
        pendings.position(0);
        byte[] ret = new byte[len];
        pendings.get(ret, 0, len);
        return ret;
    }

    private void clearBuffer() {
        pendings.clear();
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
