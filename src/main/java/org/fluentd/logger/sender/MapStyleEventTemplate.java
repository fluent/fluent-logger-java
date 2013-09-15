package org.fluentd.logger.sender;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.Templates;

public class MapStyleEventTemplate extends EventTemplate {
    @Override
    protected void doWriteData(Packer pk, Object data, boolean required) throws IOException {
        if(data instanceof Map){
            writeMap(pk, (Map<?, ?>)data, required);
        } else{
            try{
                pk.write(data);
            } catch (MessageTypeException e) {
                writeObj(pk, data, required);
            }
        }
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
}
