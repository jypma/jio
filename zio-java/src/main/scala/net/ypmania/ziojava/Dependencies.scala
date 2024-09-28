package net.ypmania.ziojava

import zio.ZIO
import zio.ZLayer

case class Dependencies(content: Object)
object Dependencies {
  def provide[R <: Object, E, A <: Object](zio: ZIO[Dependencies,E,A], dependencies: R): ZIO[Dependencies,E,A] = {
    zio.provideLayer(ZLayer.succeed(Dependencies(dependencies)))
  }

  def discard[E, A <: Object](zio: ZIO[Dependencies,E,A]): ZIO[Any, E, A] = {
    zio.provideLayer(ZLayer.succeed(Dependencies(new Object)))
  }

  def make[R <: Object](): ZIO[Dependencies,Nothing,R] = ZIO.service[Dependencies].map(_.content.asInstanceOf[R])

  def wrap[A,E](zio: ZIO[Object, E, A]): ZIO[Dependencies, E, A] = zio

  def unsafeUnwrap[A,E](zio: ZIO[Dependencies, E, A]): ZIO[Object, E, A] = zio.provideLayer(ZLayer.succeed(new Dependencies(null)))
}
