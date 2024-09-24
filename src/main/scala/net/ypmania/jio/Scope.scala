package net.ypmania.jio

class Scope (private val zioScope: zio.Scope) {
  def addFinalizer(run: UJIO[Object, ?]): UJIO[Object, Object] = {
    JIO.wrapU(zioScope.addFinalizer(JIO.unwrap(run)).as(new Object))
  }
}

object Scope {
  def make(): UJIO[Object, Scope] = JIO.wrapU(zio.Scope.make.map(s => new Scope(s)))

  trait Has {
    def scope(): Scope
  }
}
