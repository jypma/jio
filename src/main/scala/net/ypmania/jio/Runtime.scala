package net.ypmania.jio

import io.vavr.concurrent.Future
import scala.jdk.javaapi.FutureConverters._

class Runtime {
  def unsafeGet[T <: Object](jio: JIO[Object, Object, T]): Future[T] = {
    zio.Unsafe.unsafe { implicit unsafe =>
      Future.fromCompletableFuture(
        asJava(zio.Runtime.default.unsafe.runToFuture(
          jio.zio
            .provideLayer(zio.ZLayer.succeed(new JIO.Dependencies(new Object)))
            .mapError {
              case x:Throwable => x
              case other => new IllegalStateException("JIO failed unexpectedly with " + other)
            }
        ).future).toCompletableFuture()
      )
    }
  }

  def unsafeGet[T <: Object](jio: UJIO[Object, T]): Future[T] = {
    zio.Unsafe.unsafe { implicit unsafe =>
      Future.fromCompletableFuture(
        asJava(zio.Runtime.default.unsafe.runToFuture(
          JIO.unwrap(jio)
        ).future).toCompletableFuture()
      )
    }
  }
}

object Runtime {
  private val instance = new Runtime()

  def getDefault(): Runtime = instance
}
