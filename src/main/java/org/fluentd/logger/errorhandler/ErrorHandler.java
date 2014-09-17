package org.fluentd.logger.errorhandler;

import java.io.IOException;

public abstract class ErrorHandler {
    public void handleNetworkError(IOException ex) {};
}
