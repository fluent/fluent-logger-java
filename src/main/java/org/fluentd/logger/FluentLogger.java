//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 - 2013 FURUHASHI Sadayuki
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
package org.fluentd.logger;

import java.util.HashMap;
import java.util.Map;

import org.fluentd.logger.errorhandler.ErrorHandler;
import org.fluentd.logger.sender.Reconnector;
import org.fluentd.logger.sender.Sender;

public class FluentLogger {

    private static FluentLoggerFactory factory = new FluentLoggerFactory();

    public static FluentLogger getLogger(String tagPrefix) {
        return factory.getLogger(tagPrefix, "localhost", 24224);
    }

    public static FluentLogger getLogger(String tagPrefix, String host, int port) {
        return factory.getLogger(tagPrefix, host, port, 3 * 1000, 1 * 1024 * 1024);
    }

    public static synchronized FluentLogger getLogger(String tagPrefix, String host, int port, int timeout, int bufferCapacity) {
        return factory.getLogger(tagPrefix, host, port, timeout, bufferCapacity);
    }

    public static synchronized FluentLogger getLogger(String tagPrefix, String host, int port, int timeout,
            int bufferCapacity, Reconnector reconnector) {
        return factory.getLogger(tagPrefix, host, port, timeout, bufferCapacity, reconnector);
    }

    /**
     * the method is for testing
     */
    static Map<String, FluentLogger> getLoggers() {
        return factory.getLoggers();
    }

    public static synchronized void closeAll() {
        factory.closeAll();
    }

    public static synchronized void flushAll() {
        factory.flushAll();
    }

    protected String tagPrefix;

    protected Sender sender;

    protected FluentLogger() {
    }

    protected FluentLogger(String tagPrefix, Sender sender) {
        this.tagPrefix = tagPrefix;
        this.sender = sender;
    }

    public boolean log(String tag, String key, Object value) {
        return log(tag, key, value, 0);
    }

    public boolean log(String tag, String key, Object value, long timestamp) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(key, value);
        return log(tag, data, timestamp);
    }

    public boolean log(String tag, Map<String, Object> data) {
        return log(tag, data, 0);
    }

    public boolean log(String tag, Map<String, Object> data, long timestamp) {
        String concatTag = null;
        if (tagPrefix == null || tagPrefix.length() == 0) {
            concatTag = tag;
        }
        else {
            concatTag = tagPrefix + "." + tag;
        }

        if (timestamp != 0) {
            return sender.emit(concatTag, timestamp, data);
        } else {
            return sender.emit(concatTag, data);
        }
    }

    public void flush() {
        sender.flush();
    }

    public void close() {
        if (sender != null) {
            sender.flush();
            sender.close();
            sender = null;
        }
    }

    public String getName() {
        return String.format("%s_%s", tagPrefix, sender.getName());
    }

    @Override
    public String toString() {
        return String.format("%s{tagPrefix=%s,sender=%s}",
                new Object[] { this.getClass().getName(), tagPrefix, sender.toString() });
    }

    @Override
    public void finalize() {
        if (sender != null) {
            sender.close();
        }
    }

    public boolean isConnected() {
        return sender != null && sender.isConnected();
    }

    public synchronized void setErrorHandler(ErrorHandler errorHandler) {
        if (errorHandler == null) {
            throw new IllegalArgumentException("errorHandler is null");
        }

        if (sender != null) {
            sender.setErrorHandler(errorHandler);
        }
    }

    public synchronized void removeErrorHandler() {
        if (sender != null) {
            sender.removeErrorHandler();
        }
    }



}
