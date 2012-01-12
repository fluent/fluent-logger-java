//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 - 2012 Muga Nishizawa
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
package org.fluentd.logger.sender;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

import org.msgpack.MessagePack;

public class RawSocketSender implements Sender {
    private static final java.util.logging.Logger LOG =
        java.util.logging.Logger.getLogger(RawSocketSender.class.getName());

    /**
     * Calcurate exponential delay for reconnecting
     */
    private static class ExponentialDelayReconnector {
        private double wait = 0.5;

        private double waitIncrRate = 1.5;

        private double waitMax = 60;

        private int waitMaxCount;

        private LinkedList<Long> errorHistory;

        public ExponentialDelayReconnector() {
            waitMaxCount = getWaitMaxCount();
            errorHistory = new LinkedList<Long>();
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
            errorHistory.addLast(timestamp);
            if (errorHistory.size() > waitMaxCount) {
                errorHistory.removeFirst();
            }
        }

        public void clearErrorHistory() {
            errorHistory.clear();
        }

        public boolean enableReconnection(long timestamp) {
            int size = errorHistory.size();
            if (size == 0) {
                return true;
            }

            double suppressSec;
            if (size < waitMaxCount) {
                suppressSec = wait * Math.pow(waitIncrRate, size - 1);
            } else {
                suppressSec = waitMax;
            }

            return (!(timestamp - errorHistory.getLast() < suppressSec));
        }
    }

    private MessagePack msgpack;

    private SocketAddress server;

    private Socket socket;

    private String name;

    private int timeout;

    private BufferedOutputStream out;

    private ByteBuffer pendings;

    private ExponentialDelayReconnector reconnector;

    public RawSocketSender() {
        this("localhost", 24224);
    }

    public RawSocketSender(String host, int port) {
        this(host, port, 3 * 1000, 8 * 1024 * 1024);
    }

    public RawSocketSender(String host, int port, int timeout, int bufferCapacity) {
        msgpack = new MessagePack();
        msgpack.register(Event.class, EventTemplate.INSTANCE);
        pendings = ByteBuffer.allocate(bufferCapacity);
        server = new InetSocketAddress(host, port);
        name = String.format("%s{host=%s,port=%d,timeout=%d,bufCap=%d}",
                new Object[] { this.getClass().getName(), host, port, timeout,
                        bufferCapacity });
        reconnector = new ExponentialDelayReconnector();
        open();
    }

    private void open() {
        try {
            connect();
        } catch (IOException e) {
            LOG.severe("Failed to connect fluentd: " + server.toString());
            LOG.severe("Connection will be retried");
            e.printStackTrace();
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
        if (socket == null) {
            connect();
        } else if (socket.isClosed() || (!socket.isConnected())) {
            close();
            connect();
        }
    }

    public void close() {
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

    public boolean emit(String tag, Map<String, Object> data) {
        return emit(tag, System.currentTimeMillis() / 1000, data);
    }

    public boolean emit(String tag, long timestamp, Map<String, Object> data) {
        return emit(new Event(tag, timestamp, data));
    }

    protected boolean emit(Event event) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(String.format("Created %s", new Object[] { event }));
        }

        byte[] bytes = null;
        try {
            // serialize tag, timestamp and data
            bytes = msgpack.write(event);
        } catch (IOException e) {
            LOG.severe("Cannot serialize event: " + event);
            e.printStackTrace();
            return false;
        }

        // send serialized data
        return send(bytes);
    }

    private synchronized boolean send(byte[] bytes) {
        // buffering
        if (pendings.position() + bytes.length > pendings.capacity()) {
            LOG.severe("Cannot send logs to " + server.toString());
            return false;
        }
        pendings.put(bytes);

        try {
            // suppress reconnection burst
            if (!reconnector.enableReconnection(System.currentTimeMillis())) {
                return true;
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
        return true;
    }

    public byte[] getBuffer() {
        int len = pendings.position();
        pendings.position(0);
        byte[] ret = new byte[len];
        pendings.get(ret, 0, len);
        return ret;
    }

    void clearBuffer() {
        pendings.clear();
    }

    @Override
    public String toString() {
        return name;
    }
}
