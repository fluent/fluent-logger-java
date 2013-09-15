package org.fluentd.logger.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluentd.logger.sender.DefaultEventTemplate;
import org.fluentd.logger.sender.Event;
import org.msgpack.MessagePack;
import org.msgpack.template.Templates;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Unpacker;

public class MockFluentd extends Thread {

    public static interface MockProcess {
        public void process(MessagePack msgpack, Socket socket) throws IOException;
    }

    public static class MockEventTemplate extends DefaultEventTemplate {
        public static MockEventTemplate INSTANCE = new MockEventTemplate();
        public Event read(Unpacker u, Event to, boolean required) throws IOException {
            if (!required && u.trySkipNil()) {
                return null;
            }

            to = new Event();
            u.readArrayBegin();
            {
                to.tag = Templates.TString.read(u, null, required);
                to.timestamp = Templates.TLong.read(u, null, required);
                to.data = toObject(u, u.readValue());
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
                Map<String, Object> map = new LinkedHashMap<String, Object>();
                Value[] vals = v.asMapValue().asMapValue().getKeyValueArray();
                for(int i = 0; i < (vals.length / 2); i++){
                    String key = vals[i * 2].asRawValue().getString();
                    Object value = toObject(u, vals[i * 2 + 1]);
                    map.put(key, value);
                }
                return map;
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
