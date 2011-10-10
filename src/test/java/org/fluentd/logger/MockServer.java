package org.fluentd.logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MockServer {

    private ServerSocket serverSock;

    public MockServer(int port) throws IOException {
	serverSock = new ServerSocket(port);
    }

    public void run() throws IOException {
	Socket socket = serverSock.accept();
	BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
	// TODO
    }
}
