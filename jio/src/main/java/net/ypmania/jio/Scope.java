package net.ypmania.jio;

import zio.Trace;


public class Scope {
    public static UJIO<Object, Scope> make() {
        return JIO.wrapU(zio.Scope.make(Trace.empty())).map(Scope::new);
    }

    private final zio.Scope zioScope;

    public Scope(zio.Scope zioScope) {
        this.zioScope = zioScope;
    }

    public UJIO<Object, Object> addFinalizer(UJIO<Object,?> run) {
        return JIO.wrapU(zioScope.addFinalizer(() -> JIO.unwrap(run.<Object>unsafeCast()), Trace.empty()));
    }

    interface Has {
        Scope scope();
    }
}
