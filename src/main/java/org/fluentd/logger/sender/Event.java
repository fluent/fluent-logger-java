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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Templates;
import org.msgpack.unpacker.Unpacker;

public class Event {
    public String tag;

    public long timestamp;

    public Object data;

    public Event() {
    }

    public Event(String tag, Object data) {
        this(tag, System.currentTimeMillis() / 1000, data);
    }

    public Event(String tag, long timestamp, Object data) {
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
                if(v.data instanceof Map){
                    writeMap(pk, (Map<?, ?>)v.data, required);
                } else{
                    try{
                        pk.write(v.data);
                    } catch (MessageTypeException e) {
                        writeObj(pk, v.data, required);
                    }
                }
            }
            pk.writeArrayEnd();
        }

        public Event read(Unpacker u, Event to, boolean required) throws IOException {
            throw new UnsupportedOperationException("Don't need the operation");
        }

        private void writeObj(Packer pk, Object data, boolean required) throws IOException {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            Class<?> clazz = data.getClass();
            while(!clazz.equals(Object.class)){
                for(Method m : clazz.getDeclaredMethods()){
                    if(m.getDeclaringClass().equals(Object.class)) continue;
                    if(m.getParameterTypes().length != 0) continue;
                    String name = null;
                    if(m.getName().startsWith("get")){
                        name = m.getName().substring(3);
                    } else if(m.getName().startsWith("is") && m.getReturnType().equals(boolean.class)){
                        name = m.getName().substring(2);
                    } else{
                        continue;
                    }
                    if(name.length() == 0) continue;
                    name = name.substring(0, 1).toLowerCase() + (name.length() == 1 ? "" : name.substring(1));
                    try {
                        map.put(name, m.invoke(data));
                    } catch (IllegalArgumentException e) {
                    } catch (IllegalAccessException e) {
                    } catch (InvocationTargetException e) {
                    }
                }
                clazz = clazz.getSuperclass();
            }
            writeMap(pk, map, required);
        }

        private <K, V> void writeMap(Packer pk, Map<K, V> map, boolean required) throws IOException {
            pk.writeMapBegin(map.size());
            {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Templates.TString.write(pk, entry.getKey().toString(), required);
                    Object value = entry.getValue();
                    if(value instanceof Map<?, ?>){
                        writeMap(pk, (Map<?, ?>)value, required);
                    } else{
                        try {
                            pk.write(entry.getValue());
                        } catch (MessageTypeException e) {
                            writeObj(pk, entry.getValue(), required);
                        }
                    }
                }
            }
            pk.writeMapEnd();
        }
    }
}
