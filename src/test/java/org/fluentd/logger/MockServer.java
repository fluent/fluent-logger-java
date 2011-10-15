package org.fluentd.logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.msgpack.MessagePack;


public class MockServer extends Thread {

    public static interface MockProcess {
	public void process(MessagePack msgpack, Socket socket) throws IOException;
    }

    private MessagePack msgpack;

    private ServerSocket serverSocket;

    private MockProcess process;

    public MockServer(int port, MockProcess mockProcess) throws IOException {
	msgpack = new MessagePack();
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
