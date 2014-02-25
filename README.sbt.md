
## Development with sbt

sbt is a build tool for Scala, but it can be used for pure-java projects. To install sbt (in Mac OS X), do 

```
$ brew install sbt
```

Alternatively, you can download a convenient sbt script <https://raw.github.com/paulp/sbt-extras/master/sbt> that retrieves the latest sbt.jar to a local directory.

## Using sbt

To enter the sbt console, run sbt command:
```
$ sbt
>
```

* Running tests
```
> test
```

* Running tests again if some source code has changed
```
> ~test 
```   

* Running specific tests in a test case class
```
> ~test-only org.fluentd.logger.TestFluentLogger -- --tests=testNormal01
```

* Publishing locally
```
> pulbishLocal
```

* Publishing to local maven repository
```
> publishM2
```

* Publishing to Maven central
See the instruction of [sbt-sonatype](https://github.com/xerial/sbt-sonatype) plugin to set Sonatype account information. After that, you can publish GPG signed artifacts to Maven central:
```
> publishSigned    # Deploy to sonatype repository
> sonatypeRelease  # Close, release and drop the deployed artifacts
```

