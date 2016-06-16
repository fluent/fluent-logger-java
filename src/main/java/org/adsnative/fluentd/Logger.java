package org.adsnative.fluentd;

import org.fluentd.logger.Config;
import org.fluentd.logger.FluentLogger;
import org.fluentd.logger.sender.AFUNIXSocketSender;
import org.newsclub.net.unix.AFUNIXSocket;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Logger {

    private static FluentLogger LOG = FluentLogger.getLogger("app");
    private static FluentLogger xLOG = FluentLogger.getUnixLogger("app");


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
//        tf.checkSystemProperties();
//        tf.testAFUNIXSocket();
    }

    private void testAFUNIXSocket() {

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("from", "userA");
        data.put("to", "userB");

        File socketFile = new File("/etc/socket-test");
        AFUNIXSocketSender sender = new AFUNIXSocketSender(socketFile, 1045);

        sender.emit("af-app", data);

        System.out.println("Done sender !");
    }

    private void checkSystemProperties() {
        Properties props = System.getProperties();
        System.out.println(props.size());
        System.out.println(props.toString());
        if (!props.containsKey(Config.FLUENT_SENDER_CLASS)) {
            System.out.println("props does not contain fluent_sender_class");
        }
    }
}
