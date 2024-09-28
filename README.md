# JIO: The Java wrapper for ZIO

The excellent [zio](https://zio.dev/) library provides a powerful functional effect system for the Scala language. It uses Scala constructs like implicit arguments and macros to provide powerful high-level APIs around a simple conceptual type. The `JIO` project attempts to expose as much as possible of `ZIO` to Java developers.

`JIO` is a Java type. An instance of `JIO` (just like an instance of `ZIO`) is a *description of a program* that might give a successful result, or an error, when ran. This is also sometimes called an *effect*. Programs always run asynchronously, and only run when actually started: if you have an instance of a `JIO`, nothing is running yet. You have to hand a `JIO` to a `Runtime` to actually run it.

The main type of `JIO` is as follows.

```java
/** An program (also called an effect) that can succeed or fail.
    @param R The program's environment (dependencies it needs), or Object if no dependencies are needed.
    @param E The result of the program failing.
    @param A The result of the program succeeding.
*/
public class JIO<R,E,A> { ... }
```

From the previous description, you can think of a `JIO<R,E,A>` a little bit like a `Function<R,CompletionStage<Either<E,A>>>`, with "either" being a type that will either have an `E` or an `A` value. Think of it like `Optional`, except that instead of *nothing* or `T`, it has `E` or `A`.

## Why?

Encapsulating behavior through `JIO` value has a lot of advantages, since values can be modified. For example (not all of these are available in Java yet):

- Compile-time differentiating between programs that expect to fail (e.g. through I/O), from those that expect not to fail (doing computions only). Think of it like checked exceptions, but done right.
- Built-in retry: `jio.retry(3)` will return a new effect that retries the underlying effect 3 times. Much more complicated schedules are available.
- Compile-time guaranteed resource safety: The `Scope` type guarantees that a resource (e.g. a database connection) CAN only be used in a way that guarantees clean-up. Think of it as `try / finally`, but with the impossibility of using your resource without cleaning up.
- Predictable handling of real time, randomness and other external dependencies.

Look at [zio](https://zio.dev/)'s own site for more examples. Most of the advantages hold for Java as well.

# Examples

For the moment, look at [JIOTest.java](src/test/java/net/ypmania/jio/JIOTest.java) for some example Java client code.

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
