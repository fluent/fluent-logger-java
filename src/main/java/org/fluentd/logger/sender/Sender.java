//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 - 2013 FURUHASHI Sadayuki
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

import org.fluentd.logger.errorhandler.ErrorHandler;

import java.util.Map;

public interface Sender {
    boolean emit(String tag, Map<String, Object> data);

    boolean emit(String tag, long timestamp, Map<String, Object> data);

    void flush();

    void close();

    String getName();

    boolean isConnected();

    void setErrorHandler(ErrorHandler errorHandler);

    void removeErrorHandler();
}
