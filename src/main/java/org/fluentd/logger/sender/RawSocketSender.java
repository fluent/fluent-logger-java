//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 - 2013 Muga Nishizawa
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

import org.fluentd.logger.errorhandler.ErrorHandler;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

public class RawSocketSender implements Sender {

    private static final Logger LOG = LoggerFactory.getLogger(RawSocketSender.class);

    private static final ErrorHandler DEFAULT_ERROR_HANDLER = new ErrorHandler() {};

    private MessagePack msgpack;

    private SocketAddress server;

    private Socket socket;

    private int timeout;

    private BufferedOutputStream out;

    private ByteBuffer pendings;

    private Reconnector reconnector;

    private String name;

    private ErrorHandler errorHandler = DEFAULT_ERROR_HANDLER;

    public RawSocketSender() {
        this("localhost", 24224);
    }

    public RawSocketSender(String host, int port) {
        this(host, port, 3 * 1000, 8 * 1024 * 1024);
    }

    public RawSocketSender(String host, int port, int timeout, int bufferCapacity) {
        this(host, port, timeout, bufferCapacity, new ExponentialDelayReconnector());

    }

    public RawSocketSender(String host, int port, int timeout, int bufferCapacity, Reconnector reconnector) {
        msgpack = new MessagePack();
        msgpack.register(Event.class, Event.EventTemplate.INSTANCE);
        pendings = ByteBuffer.allocate(bufferCapacity);
        server = new InetSocketAddress(host, port);
        this.reconnector = reconnector;
        name = String.format("%s_%d_%d_%d", host, port, timeout, bufferCapacity);
        this.timeout = timeout;
    }

    private void connect() throws IOException {
        try {
            socket = new Socket();
            socket.connect(server, timeout);
            out = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
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

    @Override
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

    @Override
    public boolean emit(String tag, Map<String, Object> data) {
        return emit(tag, System.currentTimeMillis() / 1000, data);
    }

    @Override
    public boolean emit(String tag, long timestamp, Map<String, Object> data) {
        return emit(new Event(tag, timestamp, data));
    }

    protected boolean emit(Event event) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(String.format("Created %s", new Object[]{event}));
        }

        byte[] bytes = null;
        try {
            // serialize tag, timestamp and data
            bytes = msgpack.write(event);
        } catch (IOException e) {
            LOG.error("Cannot serialize event: " + event, e);
            return false;
        }

        // send serialized data
        return send(bytes);
    }

    private boolean flushBuffer() {
        if (reconnector.enableReconnection(System.currentTimeMillis())) {
            flush();
            if (pendings.position() == 0) {
                return true;
            }
        }

        return false;
    }

    private synchronized boolean send(byte[] bytes) {
        // buffering
        if (pendings.position() + bytes.length > pendings.capacity()) {
            if (!flushBuffer()) {
                LOG.error("Cannot send logs to " + server.toString());
                return false;
            }
            if (bytes.length > pendings.remaining()) {
                LOG.error("Log data {} larger than remaining buffer size {}", bytes.length, pendings.remaining());
                return false;
            }
        }
        pendings.put(bytes);

        // suppress reconnection burst
        if (!reconnector.enableReconnection(System.currentTimeMillis())) {
            return true;
        }

        // send pending data
        flush();

        return true;
    }

    @Override
    public synchronized void flush() {
        try {
            // check whether connection is established or not
            reconnect();
            // write data
            out.write(getBuffer());
            out.flush();
            clearBuffer();
            reconnector.clearErrorHistory();
        } catch (IOException e) {
            try {
                errorHandler.handleNetworkError(e);
            }
            catch (Exception handlerException) {
                LOG.warn("ErrorHandler.handleNetworkError failed", handlerException);
            }
            LOG.error(this.getClass().getName(), "flush", e);
            reconnector.addErrorHistory(System.currentTimeMillis());
            close();
        }
    }

    synchronized byte[] getBuffer() {
        int len = pendings.position();
        pendings.position(0);
        byte[] ret = new byte[len];
        pendings.get(ret, 0, len);
        return ret;
    }

    private void clearBuffer() {
        pendings.clear();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected() && !socket.isOutputShutdown();
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        if (errorHandler == null) {
            throw new IllegalArgumentException("errorHandler is null");
        }

        this.errorHandler = errorHandler;
    }

    @Override
    public void removeErrorHandler() {
        this.errorHandler = DEFAULT_ERROR_HANDLER;
    }
}
