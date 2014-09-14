package org.fluentd.logger;

import java.io.IOException;

public interface ServerErrorHandler {
    void handle(IOException ex);
}
