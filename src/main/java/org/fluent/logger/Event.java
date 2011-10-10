package org.fluent.logger;

import java.util.Map;

import org.msgpack.annotation.Message;

@Message
public class Event {
    public String tagName;

    public long currentTime;

    public Map<String, String> data;

    public Event() {
    }
}
