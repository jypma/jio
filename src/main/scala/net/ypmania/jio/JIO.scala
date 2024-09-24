package net.ypmania.jio

import zio.ZIO
import zio.ZLayer
import zio.ZEnvironment
import io.vavr.CheckedFunction1
import io.vavr.CheckedFunction0
import JIO._
import zio.Cause
import java.util.function.Supplier
import java.util.function.BiFunction

class UJIO[R <: Object, A <: Object](private[jio] val zio: ZIO[Dependencies, Nothing, A]) {
  def provide(deps: R): UJIO[Object, A] = {
    new UJIO(zio.provideLayer(ZLayer.succeed(Dependencies(deps))))
  }

  def provideFrom[R1 <: Object](fn: CheckedFunction1[R1,R]): UJIO[R1, A] = {
    service[R1]().map(fn).flatMapU(r => provide(r))
  }

  def provideFrom[R1 <: Object, E](jio: JIO[R1, E, R]): JIO[R1,E,A] = {
    jio.flatMapU(r => provide(r))
  }

  def provideFrom[R1 <: Object](jio: UJIO[R1, R]): UJIO[R1,A] = {
    jio.flatMapU(r => provide(r))
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

  def as[U <: Object](value: U): UJIO[R,U] = map(a => value)

  private[jio] def unwrapZIO(implicit ev: Object =:= R): ZIO[Any, Nothing, A] = zio.provideLayer(ZLayer.succeed(new JIO.Dependencies(new Object)))
}

class JIO[R <: Object, E, A <: Object](private[jio] val zio: ZIO[Dependencies, E, A]) {
  def provide(deps: R): JIO[Object, E, A] = {
    new JIO(zio.provideLayer(ZLayer.succeed(Dependencies(deps))))
  }

  def provideFrom[R1 <: Object](fn: CheckedFunction1[R1,R]): JIO[R1, E, A] = {
    service[R1]().map(fn).flatMap(r => provide(r))
  }

  def provideFrom[R1 <: Object, E1 <: E](jio: JIO[R1, E1, R]): JIO[R1,E,A] = {
    jio.flatMap(r => provide(r))
  }

  def provideFrom[R1 <: Object](jio: UJIO[R1, R]): JIO[R1,E,A] = {
    jio.flatMap(r => provide(r))
  }

  def flatMap[U <: Object, E1 >: E, U1 <: U, R1 >: R <: Object](f: CheckedFunction1[A, JIO[R1,? <: E1,U1]]): JIO[R,E1,U] = {
    new JIO(zio.flatMap { a => f.apply(a).zio })
  }

  def flatMapU[U <: Object, U1 <: U, R1 >: R <: Object](f: CheckedFunction1[A, UJIO[R1, U1]]): JIO[R,E,U] = {
    new JIO(zio.flatMap { a => f.apply(a).zio })
  }

  def map[U <: Object, U1 <: U](f: CheckedFunction1[A, U1]): JIO[R,E,U] = {
    flatMapU(a => succeed(f.apply(a)))
  }

  def catchAllU[U >: A <: Object](f: CheckedFunction1[E, UJIO[R,U]]): UJIO[R,U] = {
    new UJIO(zio.catchAll(e => f.apply(e).zio))
  }

  def as[U <: Object](value: U): JIO[R,E,U] = map(a => value)

  private[jio] def unwrapZIO(implicit ev: Object =:= R): ZIO[Any, E, A] = zio.provideLayer(ZLayer.succeed(new JIO.Dependencies(new Object)))
}

object JIO {
  private[jio] case class Dependencies(content: Object)

  private[jio] def wrapU[A <: Object](zio: ZIO[Any, Nothing, A]) = new UJIO(zio)

  def service[R <: Object](): UJIO[R, R] = new UJIO(ZIO.service[Dependencies].map(_.content.asInstanceOf[R]))

  def succeed[R <: Object, A <: Object](value: A): UJIO[R,A] = new UJIO(ZIO.succeed(value))

  def run[R <: Object, A <: Object](fn: Supplier[A]): UJIO[R,A] = new UJIO(ZIO.succeed(fn.get()))

  def tryRun[R <: Object, A <: Object](fn: CheckedFunction0[A]): JIO[R,Throwable,A] = new JIO(ZIO.attempt(fn.apply()))

  def fail[R <: Object, E, A <: Object](failure: E): JIO[R,E,A] = new JIO(ZIO.fail(failure))

  def acquireRelease[R >: Scope <: Object, E, A <: Object](acquire: JIO[? >: R, E, A], release: Function[A, UJIO[? >: R, ?]]): JIO[Scope, E, A] = {
    service[Scope]().flatMap { scope =>
      acquire.flatMapU { a =>
        scope.addFinalizer(release.apply(a).provide(scope)).as(a)
      }
    }
  }

  def acquireRelease[R >: Scope <: Object, A <: Object](acquire: UJIO[? >: R, A], release: Function[A, UJIO[? >: R, ?]]): UJIO[Scope, A] = {
    service[Scope]().flatMapU { scope =>
      acquire.flatMapU { a =>
        scope.addFinalizer(release.apply(a).provide(scope)).as(a)
      }
    }
  }

  /** Maintains a Scope while executing an effect, closing the scope after it finishes. The Scope instance is
    * made available to the given function. */
  def scopedWithU[R <: Object, A <: Object](fn: Scope => UJIO[R, A]): UJIO[R, A] = {
    new UJIO[R,A](ZIO.scopedWith { s =>
      fn.apply(new Scope(s)).zio
    })
  }

  /** Maintains a Scope while executing an effect, closing the scope after it finishes. The Scope instance is
    * made available to the given function. */
  def scopedWith[R <: Object, A <: Object, E](fn: Scope => JIO[R, E, A]): JIO[R, E, A] = {
    new JIO[R,E,A](ZIO.scopedWith { s =>
      fn.apply(new Scope(s)).zio
    })
  }

  /** Maintains a Scope while executing an effect, closing the scope after it finishes. The effect is expected
    * to only have Scope as its dependency. */
  def scoped[R >: Scope <: Object, A <: Object](jio: UJIO[R, A]): UJIO[Object, A] = {
    scopedWithU { s =>
      jio.provide(s)
    }
  }

  /** Maintains a Scope while executing an effect, closing the scope after it finishes. The effect is expected
    * to depend on Scope and other dependencies. The [combine] function should be implement to comine the new
    * returned dependencies [R], and a Scope, into the [R1] which the effect expects. */
  def scoped[R <: Object, R1 <: Object & Scope.Has, A <: Object](jio: UJIO[R1, A], combine: BiFunction[R, Scope, ? <: R1]): UJIO[R, A] = {
    scopedWithU { s =>
      service[R]().flatMapU { r =>
        jio.provide(combine.apply(r, s))
      }
    }
  }

  /** Maintains a Scope while executing an effect, closing the scope after it finishes. The effect is expected
    * to only have Scope as its dependency. */
  def scoped[R >: Scope <: Object, E, A <: Object](jio: JIO[R, E, A]): JIO[Object, E, A] = {
    scopedWith { s =>
      jio.provide(s)
    }
  }

  /** Maintains a Scope while executing an effect, closing the scope after it finishes. The effect is expected
    * to depend on Scope and other dependencies. The [combine] function should be implement to comine the new
    * returned dependencies [R], and a Scope, into the [R1] which the effect expects. */
  def scoped[R <: Object, R1 <: Object & Scope.Has, E, A <: Object](jio: JIO[R1, E, A], combine: BiFunction[R, Scope, ? <: R1]): JIO[R, E, A] = {
    scopedWith { s =>
      service[R]().flatMap { r =>
        jio.provide(combine.apply(r, s))
      }
    }
  }
}
