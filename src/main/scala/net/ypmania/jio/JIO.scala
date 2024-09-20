package net.ypmania.jio

import zio.ZIO
import zio.ZLayer
import zio.ZEnvironment
import io.vavr.CheckedFunction1
import JIO._
import zio.Cause

class UJIO[R <: Object, A <: Object](private[jio] val zio: ZIO[Dependencies, Nothing, A]) {
  def provide(deps: R): UJIO[Object, A] = {
    new UJIO(zio.provideLayer(ZLayer.succeed(Dependencies(deps))))
  }

  def provideFrom[R1 <: Object](fn: CheckedFunction1[R1,R]): UJIO[R1, A] = {
    service[R1]().map(fn).flatMapU(r => provide(r))
  }

  def flatMapU[U <: Object, U1 <: U, R1 >: R <: Object](f: CheckedFunction1[A, UJIO[R1,U1]]): UJIO[R,U] = {
    new UJIO(zio.flatMap { a =>
      f.apply(a).zio
    })
  }

  def flatMap[U <: Object, E, U1 <: U, R1 >: R <: Object](f: CheckedFunction1[A, JIO[R1,E,U1]]): JIO[R,E,U] = {
    new JIO(zio.flatMap { a =>
      f.apply(a).zio
    })
  }

  def map[U <: Object, U1 <: U](f: CheckedFunction1[A, U1]): UJIO[R,U] = {
    flatMapU(a => succeed(f.apply(a)))
  }
}

class JIO[R <: Object, E, A <: Object](private[jio] val zio: ZIO[Dependencies, E, A]) {
  def provide(deps: R): JIO[Object, E, A] = {
    new JIO(zio.provideLayer(ZLayer.succeed(Dependencies(deps))))
  }

  def provideFrom[R1 <: Object](fn: CheckedFunction1[R1,R]): JIO[R1, E, A] = {
    service[R1]().map(fn).flatMap(r => provide(r))
  }

  def flatMap[U <: Object, E1 >: E, U1 <: U, R1 >: R <: Object](f: CheckedFunction1[A, JIO[R1,? <: E1,U1]]): JIO[R,E1,U] = {
    new JIO(zio.flatMap { a =>
      f.apply(a).zio
    })
  }

  def flatMapU[U <: Object, U1 <: U, R1 >: R <: Object](f: CheckedFunction1[A, UJIO[R1, U1]]): JIO[R,E,U] = {
    new JIO(zio.flatMap { a =>
      f.apply(a).zio
    })
  }

  def map[U <: Object, U1 <: U](f: CheckedFunction1[A, U1]): JIO[R,E,U] = {
    flatMapU(a => succeed(f.apply(a)))
  }

  def catchAllU[U >: A <: Object](f: CheckedFunction1[E, UJIO[R,U]]): UJIO[R,U] = {
    new UJIO(zio.catchAll(e => f.apply(e).zio))
  }
}

object JIO {
  private[jio] case class Dependencies(content: Object)

  def service[R <: Object](): UJIO[R,  R] = new UJIO(ZIO.service[Dependencies].map(_.content.asInstanceOf[R]))

  def succeed[R <: Object, A <: Object](value: A): UJIO[R,A] = new UJIO(ZIO.succeed(value))

  def fail[R <: Object, E, A <: Object](failure: E): JIO[R,E,A] = new JIO(ZIO.fail(failure))
}
