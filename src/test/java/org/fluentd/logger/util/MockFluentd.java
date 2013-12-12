package org.fluentd.logger.util;

import org.fluentd.logger.sender.Event;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.template.Templates;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MockFluentd extends Thread {

    public static interface MockProcess {
        public void process(MessagePack msgpack, Socket socket) throws IOException;
    }

    public static class MockEventTemplate extends Event.EventTemplate {
        public static MockEventTemplate INSTANCE = new MockEventTemplate();

        public void write(Packer pk, Event v, boolean required) throws IOException {
            throw new UnsupportedOperationException("don't need operation");
        }

        public Event read(Unpacker u, Event to, boolean required) throws IOException {
            if (!required && u.trySkipNil()) {
                return null;
            }

            to = new Event();
            u.readArrayBegin();
            {
                to.tag = Templates.TString.read(u, null, required);
                to.timestamp = Templates.TLong.read(u, null, required);
                int size = u.readMapBegin();
                to.data = new HashMap<String, Object>(size);
                {
                    for (int i = 0; i < size; i++) {
                        String key = (String) toObject(u, u.readValue());
                        Object value = toObject(u, u.readValue());
                        to.data.put(key, value);
                    }
                }
                u.readMapEnd();
            }
            u.readArrayEnd();
            return to;
        }

        private static Object toObject(Unpacker u, Value v) {
            if (v.isNilValue()) {
                v.asNilValue();
                return null;
            } else if (v.isRawValue()) {
                return v.asRawValue().getString(); // String only
            } else if (v.isBooleanValue()) {
                return v.asBooleanValue().getBoolean();
            } else if (v.isFloatValue()) {
                return v.asFloatValue().getDouble(); // double only
            } else if (v.isIntegerValue()) {
                return v.asIntegerValue().getLong(); // long only
            } else if (v.isMapValue()) {
                throw new UnsupportedOperationException();
            } else if (v.isArrayValue()) {
                throw new UnsupportedOperationException();
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private ServerSocket serverSocket;

    private MockProcess process;

    private AtomicBoolean finished = new AtomicBoolean(false);

    public MockFluentd(int port, MockProcess mockProcess) throws IOException {
        serverSocket = new ServerSocket(port);
        process = mockProcess;
    }

    /**
     * Return an available port in the system
     * @return port number
     * @throws IOException
     */
    public static int randomPort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();
        return port;
    }

    public void run() {
        while (!finished.get()) {
            try {
                final Socket socket = serverSocket.accept();
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            MessagePack msgpack = new MessagePack();
                            msgpack.register(Event.class, MockEventTemplate.INSTANCE);
                            process.process(msgpack, socket);
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                };
                new Thread(r).start();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public void close() throws IOException {
        finished.set(true);
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
