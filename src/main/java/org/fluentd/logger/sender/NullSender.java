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


public class NullSender implements Sender {

    public NullSender(String host, int port, int timeout, int bufferCapacity) {
    }

    @Override
    public boolean emit(String tag, Object data) {
        return emit(tag, System.currentTimeMillis() / 1000, data);
    }

    @Override
    public boolean emit(String tag, long timestamp, Object data) {
        return true;
    }

    @Override
    public void flush() {
    }

    @Override
    public byte[] getBuffer() {
        return new byte[0];
    }

    @Override
    public void close() {
    }

    public String getName() {
        return "null";
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
