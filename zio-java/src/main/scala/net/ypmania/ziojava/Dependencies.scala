package net.ypmania.ziojava

import zio.ZIO
import zio.ZLayer
import zio.Schedule
import zio.ZEnvironment

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

  def wrap[I,O](zio: Schedule[Object, I, O]): Schedule[Dependencies, I, O] = zio

  def unsafeUnwrap[I,O](zio: Schedule[Dependencies, I, O]): Schedule[Object, I, O] = zio.provideEnvironment(ZEnvironment(new Dependencies(null)))

  def provide[R <: Object, I, O](zio: Schedule[Dependencies,I,O], dependencies: R): Schedule[Dependencies,I,O] = {
    zio.provideEnvironment(ZEnvironment(Dependencies(dependencies)))
  }

  def discard[I,O](zio: Schedule[Dependencies,I,O]): Schedule[Any,I,O] = {
    zio.provideEnvironment(ZEnvironment(Dependencies(new Object)))
  }
}
