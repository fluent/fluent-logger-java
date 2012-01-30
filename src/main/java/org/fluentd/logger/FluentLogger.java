//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 - 2012 FURUHASHI Sadayuki
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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
            Sender sender = null;
            Properties props = System.getProperties();
            if (!props.containsKey(Config.FLUENT_SENDER_CLASS)) { // create default sender object
                sender = new RawSocketSender(host, port, timeout, bufferCapacity);
            } else {
                String senderClassName = props.getProperty(Config.FLUENT_SENDER_CLASS);
                try {
                    sender = createSenderInstance(senderClassName,
                            new Object[] { host, port, timeout, bufferCapacity });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            FluentLogger logger = new FluentLogger(tag, sender);
            loggers.put(key, logger);
            return logger;
        }
    }

    private static Sender createSenderInstance(final String className, final Object[] params)
            throws ClassNotFoundException, SecurityException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Class<?> cl = FluentLogger.class.getClassLoader().loadClass(className);
        Constructor<?> cons = cl.getDeclaredConstructor(
                new Class[] { String.class, int.class, int.class, int.class });
        return (Sender) cons.newInstance(params);
    }

    public static synchronized void close() {
        for (FluentLogger logger : loggers.values()) {
            logger.close0();
        }
        loggers.clear();
    }

    protected String tagPrefix;

    protected Sender sender;

    protected FluentLogger() {
    }

    protected FluentLogger(String tag, Sender sender) {
        tagPrefix = tag;
        this.sender = sender;
    }

    public boolean log(String label, String key, Object value) {
        return log(label, key, value, 0);
    }

    public boolean log(String label, String key, Object value, long timestamp) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(key, value);
        return log(label, data, timestamp);
    }

    public boolean log(String label, Map<String, Object> data) {
        return log(label, data, 0);
    }

    public boolean log(String label, Map<String, Object> data, long timestamp) {
        if (timestamp != 0) {
            return sender.emit(tagPrefix + "." + label, timestamp, data);
        } else {
            return sender.emit(tagPrefix + "." + label, data);
        }
    }

    public void flush() throws IOException {
        sender.flush();
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
