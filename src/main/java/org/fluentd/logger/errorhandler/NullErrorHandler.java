package org.fluentd.logger.errorhandler;

import java.io.IOException;

public class NullErrorHandler implements ErrorHandler {
    @Override
    public void handleNetworkError(IOException ex) {
        // Do nothing
    }
}
