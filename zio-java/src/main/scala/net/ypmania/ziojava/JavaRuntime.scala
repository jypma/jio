package net.ypmania.ziojava

import java.util.concurrent.CompletableFuture
import scala.jdk.javaapi.FutureConverters._
import zio.ZIO

case class JavaRuntime[R](runtime: zio.Runtime[R]) {
  // [Any] will compile down to Object anyway.
  def unsafeRun[E,A](z: ZIO[Any,E,A]): CompletableFuture[A] = {
    zio.Unsafe.unsafe { implicit unsafe =>
      asJava(zio.Runtime.default.unsafe.runToFuture(
        z.mapError {
          case x:Throwable => x
          case other => new IllegalStateException("JIO failed unexpectedly with " + other)
        }
      ).future).toCompletableFuture()
    }
  }
}

object JavaRuntime {
  val defaultRuntime = JavaRuntime(zio.Runtime.default)
}
