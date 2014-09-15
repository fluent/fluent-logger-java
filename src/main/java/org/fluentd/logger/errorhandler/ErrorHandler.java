package org.fluentd.logger.errorhandler;

import java.io.IOException;

public interface ErrorHandler {
    void handleNetworkError(IOException ex);
}
