package org.fluentd.logger.sender;

import java.io.IOException;

import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.template.Templates;
import org.msgpack.unpacker.Unpacker;

public abstract class EventTemplate extends AbstractTemplate<Event> {
    public static EventTemplate INSTANCE = new DefaultEventTemplate();

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
            doWriteData(pk, v.data, required);
        }
        pk.writeArrayEnd();
    }

    protected abstract void doWriteData(Packer pk, Object data, boolean required)
    throws IOException;

    public Event read(Unpacker u, Event to, boolean required) throws IOException {
        throw new UnsupportedOperationException("Don't need the operation");
    }
}
