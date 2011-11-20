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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Templates;
import org.msgpack.unpacker.Unpacker;

public class EventTemplate extends AbstractTemplate<Event> {
    public static EventTemplate INSTANCE = new EventTemplate();

    public void write(Packer pk, Event v, boolean required)
            throws IOException {
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
                for (Map.Entry<String, String> e : v.data.entrySet()) {
                    Templates.TString.write(pk, e.getKey(), required);
                    Templates.TString.write(pk, e.getValue(), required);
                }
            }
            pk.writeMapEnd();
        }
        pk.writeArrayEnd();
    }

    public Event read(Unpacker u, Event to, boolean required)
            throws IOException {
        if (!required && u.trySkipNil()) {
            return null;
        }

        to = new Event();
        u.readArrayBegin();
        {
            to.tag = Templates.TString.read(u, null, required);
            to.timestamp = Templates.TLong.read(u, null, required);
            int size = u.readMapBegin();
            to.data = new HashMap<String, String>(size);
            {
                for (int i = 0; i < size; i++) {
                    String key = Templates.TString.read(u, null, required);
                    String value = Templates.TString.read(u, null, required);
                    to.data.put(key, value);
                }
            }
            u.readMapEnd();
        }
        u.readArrayEnd();
        return to;
    }
}