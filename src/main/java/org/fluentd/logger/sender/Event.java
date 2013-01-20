//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 - 2013 Muga Nishizawa
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
package org.fluentd.logger.sender;

import java.io.IOException;
import java.util.Map;

import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Templates;
import org.msgpack.unpacker.Unpacker;

public class Event {
    public String tag;

    public long timestamp;

    public Map<String, Object> data;

    public Event() {
    }

    public Event(String tag, Map<String, Object> data) {
        this(tag, System.currentTimeMillis() / 1000, data);
    }

    public Event(String tag, long timestamp, Map<String, Object> data) {
        this.tag = tag;
        this.timestamp = timestamp;
        this.data = data;
    }

    @Override
    public String toString() {
        return String.format("Event{tag=%s,timestamp=%d,data=%s}",
                tag, timestamp, data.toString());
    }

    public static class EventTemplate extends AbstractTemplate<Event> {
        public static EventTemplate INSTANCE = new EventTemplate();

        public void write(Packer pk, Event v, boolean required) throws IOException {
            if (v == null) {
                if (required) {
                    throw new MessageTypeException("Attempted to write null");
                }
                pk.writeNil();
                return;
            }

            pk.writeArrayBegin(3);
            {
                Templates.TString.write(pk, v.tag, required);
                Templates.TLong.write(pk, v.timestamp, required);
                pk.writeMapBegin(v.data.size());
                {
                    for (Map.Entry<String, Object> entry : v.data.entrySet()) {
                        Templates.TString.write(pk, entry.getKey(), required);
                        try {
                            pk.write(entry.getValue());
                        } catch (MessageTypeException e) {
                            String val = entry.getValue().toString();
                            Templates.TString.write(pk, val, required);
                        }
                    }
                }
                pk.writeMapEnd();
            }
            pk.writeArrayEnd();
        }

        public Event read(Unpacker u, Event to, boolean required) throws IOException {
            throw new UnsupportedOperationException("Don't need the operation");
        }
    }
}
