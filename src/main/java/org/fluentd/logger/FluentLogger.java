//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 Muga Nishizawa
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

import java.util.Map;
import java.util.WeakHashMap;


public class FluentLogger {

    private static Map<String, FluentLogger> loggers = new WeakHashMap<String, FluentLogger>();

    public static FluentLogger getLogger(String tag) {
        return getLogger(tag, "localhost", 24224);
    }

    public static FluentLogger getLogger(String tag, String host, int port) {
        return getLogger(tag, host, port, 3 * 1000, 1 * 1024 * 1024);
    }

    public static synchronized FluentLogger getLogger(String tag, String host, int port, int timeout, int bufferCapacity) {
        String key = String.format("%s_%s_%d_%d_%d", new Object[] { tag, host, port, timeout, bufferCapacity });
        if (loggers.containsKey(key)) {
            return loggers.get(key);
        } else {
            FluentLogger logger = new FluentLogger(tag, host, port, timeout, bufferCapacity);
            loggers.put(key, logger);
            return logger;
        }
    }

    private Sender sender;

    private FluentLogger(String tag, String host, int port, int timeout, int bufferCapacity) {
        sender = new Sender(tag, host, port, timeout, bufferCapacity);
    }

    public void log(String label, Map<String, String> data) {
        sender.emit(label, data);
    }
}
