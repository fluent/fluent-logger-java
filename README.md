# Fluent Logger for Java

[![Build Status](https://travis-ci.org/fluent/fluent-logger-java.svg?branch=master)](https://travis-ci.org/fluent/fluent-logger-java)

## Overview

Many web/mobile applications generate huge amount of event logs (c,f. login,
logout, purchase, follow, etc).  Analyzing these event logs can be quite
valuable for improving services.  However, collecting these logs easily and 
reliably is a challenging task.

Fluentd solves the problem by having: easy installation, small footprint, plugins,
reliable buffering, log forwarding, etc.

  * Fluentd website: [http://github.com/fluent/fluentd](http://github.com/fluent/fluentd)

**fluent-logger-java** is a Java library, to record events via Fluentd, from Java application.

## Requirements

Java >= 1.6

## Install

### Install with all-in-one jar file

You can download all-in-one jar file for Fluent Logger for Java.

```bash
wget http://central.maven.org/maven2/org/fluentd/fluent-logger/${logger.version}/fluent-logger-${logger.version}-jar-with-dependencies.jar   
```

To use Fluent Logger for Java, set the above jar file to your classpath.

### Install from Maven2 repository

Fluent Logger for Java is released on Fluent Maven2 repository.  You can 
configure your pom.xml or build.gradle as follows to use it:

**Maven:**

```xml
<dependencies>
  ...
  <dependency>
    <groupId>org.fluentd</groupId>
    <artifactId>fluent-logger</artifactId>
    <version>${logger.version}</version>
  </dependency>
  ...
</dependencies>
```

**Gradle:**

```gradle
dependencies {
    compile 'org.fluentd:fluent-logger:'+loggerVersion
}
```

### Install from Github repository

You can get latest source code using git.

```bash
git clone git@github.com:fluent/fluent-logger-java.git
cd fluent-logger-java
mvn assembly:assembly
```

You will get the fluent logger jar file in fluent-logger-java/target 
directory.  File name will be fluent-logger-${logger.version}-jar-with-dependencies.jar.
For more detail, see pom.xml.

**Replace `${logger.version}` or `loggerVersion` with the current version of Fluent Logger for Java.**

## Quickstart

The following program is a small example of Fluent Logger for Java.

```java
import java.util.HashMap;
import java.util.Map;
import org.fluentd.logger.FluentLogger;

public class Main {
    private static FluentLogger LOG = FluentLogger.getLogger("app");

    public void doApplicationLogic() {
        // ...
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("from", "userA");
        data.put("to", "userB");
        LOG.log("follow", data);
        // ...
    }
}
```

To create Fluent Logger instances, users need to invoke getLogger method in 
FluentLogger class like org.slf4j, org.log4j logging libraries.  The method 
should be called only once.  By default, the logger assumes fluent daemon is 
launched locally.  You can also specify remote logger by passing the following 
options.  

```java
// for remote fluentd
private static FluentLogger LOG = FluentLogger.getLogger("app", "remotehost", port);
```

Then, please create the events like this.  This will send the event to fluentd, 
with tag 'app.follow' and the attributes 'from' and 'to'.

Close method in FluentLogger class should be called explicitly when application 
is finished.  The method closes socket connection with the fluentd.

```java
FluentLogger.close();
```

## License

Apache License, Version 2.0
