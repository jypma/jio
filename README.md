# JIO: The Java wrapper for ZIO

# Building

To keep sanity intact, the project is split into two parts:

- `zio-java` contains a small wrapper for ZIO that makes it more accessible from Java (taking care of `R` needing a `Tag`, among other things). This is written in Scala using SBT.
- `jio` contains JIO itself, written in plain Java using Maven.

You should build it as follows:
```sh
cd zio-java
sbt publishM2
cd ..
cd jio
mvn test
```
