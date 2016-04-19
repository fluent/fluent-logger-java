
package org.fluentd.logger.sender;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.fluentd.logger.errorhandler.ErrorHandler;
import org.fluentd.logger.sender.ExponentialDelayReconnector;
import org.fluentd.logger.sender.RawSocketSender;
import org.fluentd.logger.sender.Reconnector;
import org.fluentd.logger.sender.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An asynchronous wrapper around RawSocketSender
 *
 * @author mxk
 *
 */
public class AsyncRawSocketSender implements Sender {

    private final class EmitRunnable implements Runnable {
        private final String tag;
        private final Map<String, Object> data;
        private final RawSocketSender sender;
        private final long timestamp;

        private EmitRunnable(String tag, Map<String, Object> data,
                             RawSocketSender sender, long timestamp) {
            this.tag = tag;
            this.data = data;
            this.sender = sender;
            this.timestamp = timestamp;
        }

        @Override
        public void run() {
            sender.emit(tag, timestamp, data);
        }
    }

    private final class FlushRunnable implements Runnable {
        private final RawSocketSender sender;

        private FlushRunnable(RawSocketSender sender) {
            this.sender = sender;
        }

        @Override
        public void run() {
            sender.flush();
        }
    }

    private RawSocketSender sender;
    private Reconnector reconnector;

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(AsyncRawSocketSender.class);

    private final ExecutorService flusher = Executors.newSingleThreadExecutor();

    private static final ErrorHandler DEFAULT_ERROR_HANDLER = new ErrorHandler() {};

    private ErrorHandler errorHandler = DEFAULT_ERROR_HANDLER;

    public AsyncRawSocketSender() {
        this("localhost", 24224);
    }

    public AsyncRawSocketSender(String host, int port) {
        this(host, port, 3 * 1000, 8 * 1024 * 1024);
    }

    public AsyncRawSocketSender(String host, int port, int timeout,
                                int bufferCapacity) {
        this(host, port, timeout, bufferCapacity,
                new ExponentialDelayReconnector());
    }

    public AsyncRawSocketSender(String host, int port, int timeout,
                                int bufferCapacity, Reconnector reconnector) {
        this.reconnector = reconnector;
        this.sender = new RawSocketSender(host, port, timeout, bufferCapacity,
                reconnector);
    }

    @Override
    public synchronized void flush() {
        final RawSocketSender sender = this.sender;
        flusher.execute(new FlushRunnable(sender));
    }

    @Override
    public void close() {
        sender.close();
    }

    @Override
    public boolean emit(String tag, Map<String, Object> data) {
        return emit(tag, System.currentTimeMillis() / 1000, data);
    }

    @Override
    public boolean emit(final String tag, final long timestamp, final Map<String, Object> data) {
        final RawSocketSender sender = this.sender;
        flusher.execute(new EmitRunnable(tag, data, sender, timestamp));

        return sender.isConnected() || reconnector.enableReconnection(System.currentTimeMillis());
    }

    @Override
    public String getName() {
        return sender.getName();
    }

    @Override
    public boolean isConnected() {
        return sender.isConnected();
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