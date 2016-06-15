package org.fluentd.logger.sender;

import org.fluentd.logger.errorhandler.ErrorHandler;

import java.util.Map;

public class AFUNIXSocketSender implements Sender {


    @Override
    public boolean emit(String tag, Map<String, Object> data) {
        return false;
    }

    @Override
    public boolean emit(String tag, long timestamp, Map<String, Object> data) {
        return false;
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {

    }

    @Override
    public void removeErrorHandler() {

    }
}
