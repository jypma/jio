package net.ypmania.jio;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import net.ypmania.ziojava.Dependencies;
import scala.runtime.Nothing$;
import zio.Trace;
import zio.ZIO;

/** An program that can succeed or fail.
    @param R The program's environment (dependencies it needs), or Object if no dependencies are needed.
    @param E The result of the program failing.
    @param A The result of the program succeeding.
*/
public class JIO<R,E,A> {
    public static <R> UJIO<R,R> environment() {
        return new UJIO<R,R>(Dependencies.<R> make());
    }

    public static <A> UJIO<Object, A> succeed(A value) {
        return wrapU(ZIO.succeedNow(value));
    }

    public static <A> UJIO<Object, A> succeedWith(Supplier<A> fn) {
        return wrapU(ZIO.succeed(u -> fn.get(), Trace.empty()));
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

    @SuppressWarnings("unchecked")
    public static <R,E,A> JIO<R,E,A> cast(JIO<? super R, ? extends E, ? extends A> jio) {
        return (JIO<R, E, A>) jio;
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

    /** Casts to <U>, which must be a supertype of <A>. Unsafe since <U super A> is not possible to declare in Java. */
    @SuppressWarnings("unchecked")
    <U> JIO<R,E,U> unsafeCast() {
        return (JIO<R,E,U>) this;
    }

    /// ------ only for JIO --------

    // TODO: Test
    public <E1> JIO<R,E1,A> mapError(Function<? super E, ? extends E1> fn) {
        return new JIO<>(zio.mapError(e -> fn.apply(e), null, Trace.empty()));
    }

    // TODO: Test
    public <E1> JIO<R,E1,A> flatMapError(Function<? super E, UJIO<? super R, ? extends E1>> fn) {
        return new JIO<>(zio.flatMapError(e -> fn.apply(e).<E1>unsafeCast().zio, null, Trace.empty()));
    }

    public <U> UJIO<R,U> catchAllU(Function<? super E, UJIO<? super R, ? extends U>> fn) {
        return new UJIO<>(zio.catchAll(e -> fn.apply(e).<U>unsafeCast().zio, null, Trace.empty()));
    }
}
