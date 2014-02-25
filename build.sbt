import AssemblyKeys._

assemblySettings

sonatypeSettings

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

javacOptions in (Compile, doc) <<= (baseDirectory, version) map { (bd, v) => Seq(
    "-locale", "en_US",
    "-sourcepath", bd.getAbsolutePath,
    "-doctitle", s"Fluent Logger for Java ${v} API"
)}

pomExtra := {
  <url>https://github.com/fluent/fluent-logger-java</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:git://github.com/fluent/fluent-logger-java.git</connection>
    <developerConnection>scm:git:git@github.com:fluent/fluent-logger-java.git</developerConnection>
    <url>scm:git:git://github.com/fluent/fluent-logger-java.git</url>
  </scm>
  <developers>
    <developer>
      <id>muga</id>
      <name>Muga Nishizawa</name>
      <email>muga.nishizawa@gmail.com</email>
    </developer>
  </developers>
}


// Publish fluent-logger-(version)-assembly.jar 
artifact in (Compile, assembly) ~= { art =>
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)
