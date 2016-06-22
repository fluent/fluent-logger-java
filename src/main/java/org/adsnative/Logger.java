package org.adsnative;

import org.fluentd.logger.Config;
import org.fluentd.logger.FluentLogger;
import org.fluentd.logger.sender.AFUNIXSocketSender;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Logger {

    private static FluentLogger LOG = FluentLogger.getUnixLogger("raw", "/etc/socketname");
//    private static FluentLogger LOG = FluentLogger.getUnixLogger("raw", "/tmp/bevo/socket.sockd", 0);
//    private static FluentLogger LOG = FluentLogger.getLogger("app");

    public void doApplicationLogic() {

        Map<String, Object> data = getSomeObjectMap();
        LOG.log("follow", data);

        System.out.println("Done!");
    }

    public static void main(String[] args) {
        Logger tf = new Logger();

        tf.doApplicationLogic();
    }

    private Map<String, Object> getSomeObjectMap() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("from", "userA");
        data.put("to", "userB");
        return data;
    }
}
