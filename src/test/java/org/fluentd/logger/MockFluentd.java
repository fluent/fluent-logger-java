package org.fluentd.logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.fluentd.logger.sender.Event;
import org.fluentd.logger.sender.EventTemplate;
import org.msgpack.MessagePack;
import org.msgpack.template.Templates;
import org.msgpack.unpacker.Unpacker;

public class MockFluentd extends Thread {

    public static interface MockProcess {
        public void process(MessagePack msgpack, Socket socket) throws IOException;
    }

    public static class MockStringEventTemplate extends EventTemplate {
        public static MockStringEventTemplate INSTANCE = new MockStringEventTemplate();

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
                        String key = Templates.TString.read(u, null, required);
                        String value = Templates.TString.read(u, null, required);
                        to.data.put(key, value);
                    }
                }
                u.readMapEnd();
            }
            u.readArrayEnd();
            return to;
        }
    }

    private MessagePack msgpack;

    private ServerSocket serverSocket;

    private MockProcess process;

    public MockFluentd(int port, MockProcess mockProcess) throws IOException {
        msgpack = new MessagePack();
        msgpack.register(Event.class, MockStringEventTemplate.INSTANCE);
        serverSocket = new ServerSocket(port);
        process = mockProcess;
    }

    public void run() {
        try {
            final Socket socket = serverSocket.accept();
            Thread th = new Thread() {
                public void run() {
                    try {
                        process.process(msgpack, socket);
                    } catch (IOException e) { // ignore
                    }
                }
            };
            th.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
