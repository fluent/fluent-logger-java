package org.adsnative.fluentd;

import org.fluentd.logger.FluentLogger;

import java.util.HashMap;
import java.util.Map;

public class Logger {

    private static FluentLogger LOG = FluentLogger.getLogger("app");

    public void doApplicationLogic() {

        System.out.println("init -> application logic");

        // ...
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("from", "userA");
        data.put("to", "userB");

        LOG.log("follow", data);
        // ...

        System.out.println("Done.");
    }

    public static void main(String[] args) {
        Logger tf = new Logger();

        tf.doApplicationLogic();
    }



}
