//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 FURUHASHI Sadayuki
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
import java.util.WeakHashMap;

import org.fluentd.logger.sender.RawSocketSender;
import org.fluentd.logger.sender.Sender;

public class FluentLogger {

    private static Map<String, FluentLogger> loggers = new WeakHashMap<String, FluentLogger>();

    public static FluentLogger getLogger(String tag) {
        return getLogger(tag, "localhost", 24224);
    }

    public static FluentLogger getLogger(String tag, String host, int port) {
        return getLogger(tag, host, port, 3 * 1000, 1 * 1024 * 1024);
    }

    public static synchronized FluentLogger getLogger(
            String tag, String host, int port, int timeout, int bufferCapacity) {
        String key = String.format("%s_%s_%d_%d_%d",
                new Object[] { tag, host, port, timeout, bufferCapacity });
        if (loggers.containsKey(key)) {
            return loggers.get(key);
        } else {
            FluentLogger logger =
                new FluentLogger(tag, host, port, timeout, bufferCapacity);
            loggers.put(key, logger);
            return logger;
        }
    }

    public static synchronized void close() {
        for (FluentLogger logger : loggers.values()) {
            logger.close0();
        }
    }

    protected String tagPrefix;

    protected Sender sender;

    protected FluentLogger() {
    }

    protected FluentLogger(String tag, String host, int port, int timeout, int bufferCapacity) {
        tagPrefix = tag;
        sender = new RawSocketSender(host, port, timeout, bufferCapacity);
    }

    public void log(String label, String key, String value) {
        log(label, key, value, 0);
    }

    public void log(String label, String key, String value, long timestamp) {
        Map<String, String> data = new HashMap<String, String>();
        data.put(key, value);
        log(label, data, timestamp);
    }

    public void log(String label, Map<String, String> data) {
        log(label, data, 0);
    }

    public void log(String label, Map<String, String> data, long timestamp) {
        if (timestamp != 0) {
            sender.emit(tagPrefix + "." + label, timestamp, data);
        } else {
            sender.emit(tagPrefix + "." + label, data);
        }
    }

    protected void close0() {
        if (sender != null) {
            sender.close();
            sender = null;
        }
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
}
