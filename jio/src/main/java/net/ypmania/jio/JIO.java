package net.ypmania.jio;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import net.ypmania.jio.function.CheckedFunction0;
import net.ypmania.jio.function.CheckedRunnable;
import net.ypmania.jio.tuple.Tuple;
import net.ypmania.jio.tuple.Tuple2;
import net.ypmania.ziojava.Dependencies;
import scala.runtime.Nothing$;
import zio.Trace;
import zio.ZIO;

/** An program (also called an effect) that can succeed or fail.
    @param R The program's environment (dependencies it needs), or Object if no dependencies are needed.
    @param E The result of the program failing.
    @param A The result of the program succeeding.
*/
public class JIO<R,E,A> {
    public static UJIO<Object,Object> empty() {
        return succeed(null);
    }

    public static <R> UJIO<R,R> environment() {
        return new UJIO<R,R>(Dependencies.<R> make());
    }

    public static <A> UJIO<Object, A> succeed(A value) {
        return wrapU(ZIO.succeedNow(value));
    }

    public static <A> UJIO<Object, A> succeedWith(Supplier<A> fn) {
        return wrapU(ZIO.succeed(u -> fn.get(), Trace.empty()));
    }

    public static UJIO<Object, Object> succeedWith(Runnable fn) {
        return wrapU(ZIO.succeed(u -> {
            fn.run();
            return null;
        }, Trace.empty()));
    }

    public static <E,A> JIO<Object,E,A> fail(E failure) {
        return wrap(ZIO.fail(() -> failure, Trace.empty())).<A>unsafeCast();
    }

    public static <E,A> JIO<Object,E,A> failWith(Supplier<E> failure) {
        return wrap(ZIO.fail(() -> failure.get(), Trace.empty())).<A>unsafeCast();
    }

    public static <A> UJIO<Object, A> wrapU(ZIO<Object, Nothing$, ? extends A> zio) {
        return new UJIO<Object, A>(Dependencies.wrap(zio));
    }

    public static <E,A> JIO<Object, E, A> wrap(ZIO<Object, ? extends E, ? extends A> zio) {
        return new JIO<Object, E, A>(Dependencies.wrap(zio));
    }

    public static <A> ZIO<Object,Nothing$,A> unwrap(UJIO<Object,A> jio) {
        return Dependencies.unsafeUnwrap(jio.zio);
    }

    public static <A,E> ZIO<Object,E,A> unwrap(JIO<Object,E,A> jio) {
        return Dependencies.unsafeUnwrap(jio.zio);
    }

    public static <A, X extends Throwable> JIO<Object, X, A> attempt(CheckedFunction0<? extends A, ? extends X> fn) {
        return wrap(ZIO.attempt(u -> fn.unchecked().get(), Trace.empty())).<A>unsafeCast().<X>unsafeCastError();
    }

    public static <X extends Throwable> JIO<Object, X, Object> attempt(CheckedRunnable<? extends X> fn) {
        return wrap(ZIO.attempt(u -> {
            fn.unchecked().run();
            return null;
        }, Trace.empty())).<Object>unsafeCast().<X>unsafeCastError();
    }

    /** Casts the given JIO to a less-specific generic type. */
    @SuppressWarnings("unchecked")
    public static <R,E,A> JIO<R,E,A> cast(JIO<? super R, ? extends E, ? extends A> jio) {
        return (JIO<R, E, A>) jio;
    }

    /** Casts the given JIO to a less-specific generic type. */
    @SuppressWarnings("unchecked")
    public static <R,A> UJIO<R,A> cast(UJIO<? super R, ? extends A> jio) {
        return (UJIO<R,
            A>) jio;
    }

    /** Runs another effect with the result of this one. This is functionally equivalent to this.flatMap(fn), but the static
     * variant has a more flexible combination of error and environment types in the Java language. */
    public static <R,E,A,I> JIO<R,E,A> flatMap(JIO<? super R, ? extends E, I> jio, Function<? super I, JIO<? super R, ? extends E, ? extends A>> fn) {
        return new JIO<>(jio.zio.flatMap(a -> fn.apply(a).zio, Trace.empty()));
    }

    /** Returns a JIO managing a resource that requires cleanup, requiring an environment.
        @param R environment for the JIO that acquires and releases the resource (which must provide access to a Scope instance).
        @param E error for the JIO that acquires and releases the resource.
        @param A type of the managed resource
        @param getScope Returns the Scope from an R
    */
    public static <R,E,A> JIO<R, E, A> acquireReleaseR(JIO<? super R, ? extends E, ? extends A> acquire, Function<? super A, UJIO<? super R, ?>> release, Function<? super R, Scope> getScope) {
        return JIO.<R>environment().flatMap(env ->
            acquire.flatMapU(a ->
                getScope.apply(env).addFinalizer(release.apply(a).provide(env)).as(a)
            )
        );
    }

    /** Returns a JIO managing a resource that requires cleanup.
        @param E error for the JIO that acquires and releases the resource.
        @param A type of the managed resource
    */
    public static <E,A> JIO<Scope, E, A> acquireRelease(JIO<Object, ? extends E, ? extends A> acquire, Function<? super A, UJIO<Object, ?>> release) {
        return JIO.<Scope>environment().flatMap(scope ->
            acquire.flatMapU(a ->
                scope.addFinalizer(release.apply(a).provide(scope)).as(a)
            )
        );
    }

    /** Returns a JIO managing a resource that requires cleanup, without failing, requiring an environment.
        @param R environment for the JIO that acquires and releases the resource (which must provide access to a Scope instance).
        @param A type of the managed resource
        @param getScope Returns the Scope from an R
    */
    public static <A,R> UJIO<R, A> acquireReleaseUR(UJIO<? super R, ? extends A> acquire, Function<? super A, UJIO<? super R, ?>> release, Function<? super R, Scope> getScope) {
        return JIO.<R>environment().flatMapU(env ->
            acquire.flatMapU(a ->
                getScope.apply(env).addFinalizer(release.apply(a).provide(env)).as(a)
            )
        );
    }

    /** Returns a JIO managing a resource that requires cleanup, without failing.
        @param A type of the managed resource
    */
    public static <A> UJIO<Scope, A> acquireReleaseU(UJIO<Object, ? extends A> acquire, Function<? super A, UJIO<Object, ?>> release) {
        return JIO.<Scope>environment().flatMapU(scope ->
            acquire.flatMapU(a ->
                scope.addFinalizer(release.apply(a).provide(scope)).as(a)
            )
        );
    }

  /** Maintains a Scope while executing an effect, closing the scope after it finishes. The Scope instance is
    * made available to the given function. */
    public static <R,A> UJIO<R,A> scopedWithU(Function<Scope, UJIO<? super R, ? extends A>> fn) {
        return new UJIO<>(ZIO.scopedWith(s -> fn.apply(new Scope(s)).zio, Trace.empty()));
    }

  /** Maintains a Scope while executing an effect, closing the scope after it finishes. The Scope instance is
    * made available to the given function. */
    public static <R,E,A> JIO<R,E,A> scopedWith(Function<Scope, JIO<? super R, ? extends E, ? extends A>> fn) {
        return new JIO<>(ZIO.scopedWith(s -> fn.apply(new Scope(s)).zio, Trace.empty()));
    }

  /** Maintains a Scope while executing an effect, closing the scope after it finishes. The Scope instance is
   * made available as environment to the given JIO. */
    public static <A> UJIO<Object, A> scoped(UJIO<? super Scope, ? extends A> jio) {
        return scopedWithU(s -> jio.provide(s));
    }

  /** Maintains a Scope while executing an effect, closing the scope after it finishes. The Scope instance is
   * made available as environment to the given JIO. */
    public static <E,A> JIO<Object, E, A> scoped(JIO<? super Scope, ? extends E, ? extends A> jio) {
        return scopedWith(s -> jio.provide(s));
    }

  /** Maintains a Scope while executing an effect requiring an environment, closing the scope after it finishes. The Scope
   * instance is made available to [combine], to be passed as combined environment to the given JIO.
   * @param RI The environment the inner JIO requires (presumably including a Scope, potentially through Scope.Has)
   * @param RO The environment the result of this method requires (presumably RI minus the Scope)
   * @param combine Function combining an RO and a Scope into an RI
   */
    public static <RO, RI, A> UJIO<RO, A> scoped(UJIO<? super RI, ? extends A> jio, BiFunction<RO, Scope, RI> combine) {
        return scopedWithU(scope -> JIO.<RO> environment().flatMapU(env -> jio.provide(combine.apply(env, scope))));
    }

  /** Maintains a Scope while executing an effect requiring an environment, closing the scope after it finishes. The Scope
   * instance is made available to [combine], to be passed as combined environment to the given JIO.
   * @param RI The environment the inner JIO requires (presumably including a Scope, potentially through Scope.Has)
   * @param RO The environment the result of this method requires (presumably RI minus the Scope)
   * @param combine Function combining an RO and a Scope into an RI
   */
    public static <RO, RI, E, A> JIO<RO, E, A> scoped(JIO<? super RI, ? extends E, ? extends A> jio, BiFunction<RO, Scope, RI> combine) {
        return scopedWith(scope -> JIO.<RO> environment().flatMap(env -> jio.provide(combine.apply(env, scope))));
    }

    final ZIO<Dependencies, E, A> zio;

    @SuppressWarnings("unchecked")
    JIO(ZIO<Dependencies, ? extends E, ? extends A> zio) {
        this.zio = (ZIO<Dependencies, E, A>) zio;
    }

    public JIO<Object, E, A> provide(R environment) {
        return new JIO<Object, E, A>(Dependencies.provide(zio, environment));
    }

    public <R1> JIO<R1, E, A> provideFrom(Function<? super R1, ? extends R> fn) {
        return JIO.<R1>environment().map(fn).flatMap(this::provide);
    }

    public <R1> JIO<R1, E, A> provideFrom(UJIO<R1, R> jio) {
        return jio.flatMap(this::provide);
    }

    /** Executes the given effect after this one. The effect must have a compatible environment. If not, you can align both
     * effects' environments to the same type by using .provideFrom() on both before flatMap(). */
    public <U> JIO<R, E, U> flatMapU(Function<? super A, UJIO<? super R, ? extends U>> fn) {
        return new JIO<>(zio.flatMap(a -> fn.apply(a).<E>toJIO().zio, Trace.empty()));
    }

    /** Executes the given effect after this one. The effect must have a compatible environment and error. If not, you can align
     * both effects' environments to the same type by using .provideFrom() on both before flatMap(), or align the error type using
     * mapError. */
    public <U> JIO<R, E, U> flatMap(Function<? super A, JIO<? super R, ? extends E, ? extends U>> fn) {
        return new JIO<>(zio.flatMap(a -> fn.apply(a).zio, Trace.empty()));
    }

    public <U> JIO<R, E, U> map(Function<? super A, ? extends U> fn) {
        return flatMapU(a -> succeed(fn.apply(a)));
    }

    public <U> JIO<R, E, U> as(U value) {
        return map(a -> value);
    }

    /** Returns a JIO that swaps the error / success cases */
    public JIO<R, A, E> flip() {
        return new JIO<>(zio.flip(Trace.empty()));
    }

    @SuppressWarnings("unchecked")
    <U> JIO<R,E,U> unsafeCast() {
        return (JIO<R,E,U>) this;
    }

    @SuppressWarnings("unchecked")
    <E1> JIO<R,E1,A> unsafeCastError() {
        return (JIO<R,E1,A>) this;
    }

    public <B> JIO<R,E,B> repeat(Schedule<? super R, ? super A, ? extends B> schedule) {
        return new JIO<>(zio.repeat(() -> Schedule.<R,A,B>cast(schedule).schedule, Trace.empty()));
    }

    public <B> JIO<R,E,A> retry(Schedule<? super R, ? super E, ? extends B> schedule) {
        return new JIO<>(zio.retry(() -> Schedule.<R,E,B>cast(schedule).schedule, null, Trace.empty()));
    }

    public <B> JIO<R,E,Tuple2<A,B>> zip(JIO<? super R, ? extends E, ? extends B> that) {
        return zipWith(that, Tuple::of);
    }

    public <B,O> JIO<R,E,O> zipWith(JIO<? super R, ? extends E, ? extends B> that, BiFunction<A,B,O> fn) {
        return new JIO<R,E,Object>(zio.zip(() -> that.zio, (a,b) -> fn.apply(a,b), Trace.empty())).<O>unsafeCast();
    }

    /// ------ only for JIO --------

    public <E1> JIO<R,E1,A> mapError(Function<? super E, ? extends E1> fn) {
        return new JIO<>(zio.mapError(e -> fn.apply(e), null, Trace.empty()));
    }

    public <E1> JIO<R,E1,A> flatMapError(Function<? super E, UJIO<? super R, ? extends E1>> fn) {
        return new JIO<>(zio.flatMapError(e -> fn.apply(e).<E1>unsafeCast().zio, null, Trace.empty()));
    }

    public <U> UJIO<R,U> catchAllU(Function<? super E, UJIO<? super R, ? extends U>> fn) {
        return new UJIO<>(zio.catchAll(e -> fn.apply(e).<U>unsafeCast().zio, null, Trace.empty()));
    }
}
