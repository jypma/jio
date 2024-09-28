package net.ypmania.jio;

import java.util.concurrent.CompletableFuture;

import net.ypmania.ziojava.JavaRuntime;

public class Runtime<R> {
    public static final Runtime<Object> runtime = new Runtime<>(JavaRuntime.defaultRuntime());

    private JavaRuntime<R> r;

    public Runtime(JavaRuntime<R> r) {
        this.r = r;
    }

    public <A> CompletableFuture<A> unsafeRun(UJIO<Object,A> jio) {
        return r.unsafeRun(JIO.unwrap(jio));
    }
}
