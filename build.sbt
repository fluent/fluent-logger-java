organization := "org.fluentd"

description := "Fluent Logger for Java"

name := "fluent-logger"

libraryDependencies ++= Seq(
    "org.msgpack" % "msgpack" % "0.6.8",
    "org.slf4j" % "slf4j-api" % "1.7.6",
    "ch.qos.logback" % "logback-classic" % "1.1.1" % "test",
    "com.novocode" % "junit-interface" % "0.10" % "test"
)

logBuffered in Test := false

parallelExecution in Test := false
    
autoScalaLibrary := false

scalaVersion in Global := "2.10.3"

crossPaths := false

incOptions := incOptions.value.withNameHashing(true)

javacOptions in Compile ++= Seq("-Xlint:unchecked")
