package org.fluent.logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.msgpack.MessagePack;
import org.slf4j.LoggerFactory;


public class Sender {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Sender.class);

    private MessagePack msgpack;

    private String tag;

    private SocketAddress server;

    private Socket socket;

    private int timeout;

    private BufferedOutputStream out;

    private ByteBuffer pendingBuffer;

    public Sender(String tag) throws IOException {
	this(tag, "localhost", 24224);
    }
    public Sender(String tag, String host, int port) throws IOException {
	this(tag, host, port, 3 * 1000, 1 * 1024 * 1024);
    }

    public Sender(String tag, String host, int port, int timeout, int bufferCapacity) {
	this.tag = tag;
	msgpack = new MessagePack();
	server = new InetSocketAddress(host, port);
	try {
	    connect();
	} catch (IOException e) {
	    close();
	}
	pendingBuffer = ByteBuffer.allocate(bufferCapacity);
    }

    private void connect() throws IOException {
	socket = new Socket();
	socket.connect(server, timeout); // the timeout value to be used in milliseconds
	out = new BufferedOutputStream(socket.getOutputStream());
    }

    private void reconnect() throws IOException {
	boolean b = false;
	if (socket.isClosed()) {
	    socket = null;
	    b = true;
	} else if (! socket.isConnected()) {
	    close();
	    b = true;
	}
	if (b) {
	    try {
		Thread.sleep(3 * 1000);
	    } catch (InterruptedException e) { // ignore
	    }
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

    public void emit(String label, Map<String, String> data) throws IOException {
	String tagName = tag + "." + label;
	long currentTime = System.currentTimeMillis();

	if (LOG.isDebugEnabled()) {
	    LOG.debug(String.format("Create event=[tag=%s,curtime=%d,data=%s]",
		    new Object[] { tagName, currentTime, data.toString() }));
	}

	// create event
	Event event = new Event();
	event.tagName = tagName;
	event.currentTime = currentTime;
	event.data = data;

	// serialize tagName, currentTime and data
        byte[] bytes = msgpack.write(event);

        // send serialized data
        send(bytes);
    }

    private synchronized void send(byte[] bytes) throws IOException {
	// check pending buffer
	try {
	    pendingBuffer.put(bytes);
	} catch (BufferOverflowException e) {
	    pendingBuffer.clear();
	    pendingBuffer.put(bytes);
	}

	// check whether connection is established or not
	reconnect();

	try {
	    pendingBuffer.flip();
	    int len = pendingBuffer.remaining();
	    byte[] b = new byte[len];
	    pendingBuffer.get(b, 0, b.length);
	    out.write(b);
	    out.flush();
	    pendingBuffer.clear();
	} catch (IOException e) {
	    close();
	}
    }
}
