//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 Muga Nishizawa
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

import java.util.Map;

public class Event {
    public String tag;

    public long timestamp;

    public Map<String, Object> data;

    public Event() {
    }

    public Event(String tag, long timestamp, Map<String, Object> data) {
        this.tag = tag;
        this.timestamp = timestamp;
        this.data = data;
    }

    @Override
    public String toString() {
        return String.format("Event { tag=%s, timestamp=%d, data=%s }",
                new Object[] { tag, timestamp, data.toString() });
    }
}
