package org.fluentd.logger.sender;

import java.io.IOException;

import org.msgpack.packer.Packer;

public class DefaultEventTemplate extends EventTemplate {
    @Override
    protected void doWriteData(Packer pk, Object data, boolean required) throws IOException {
        pk.write(data);
    }
}
