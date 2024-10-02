package net.ypmania.jio;

import java.util.function.BiFunction;
import java.util.function.Function;

import net.ypmania.jio.tuple.Tuple;
import net.ypmania.jio.tuple.Tuple2;
import net.ypmania.ziojava.Dependencies;
import scala.runtime.Nothing$;
import zio.Trace;
import zio.ZIO;

/** An program that does not fail.
    @param R The program's environment (dependencies it needs), or Object if no dependencies are needed.
    @param A The result of running the program.
*/
public class UJIO<R,A> {
    final ZIO<Dependencies, Nothing$, A> zio;

    @SuppressWarnings("unchecked")
    UJIO(ZIO<Dependencies, Nothing$, ? extends A> zio) {
        this.zio = (ZIO<Dependencies, Nothing$, A>) zio;
    }

    public UJIO<Object, A> provide(R environment) {
        return new UJIO<Object, A>(Dependencies.provide(zio, environment));
    }

    public <R1> UJIO<R1, A> provideFrom(Function<? super R1, ? extends R> fn) {
        return JIO.<R1>environment().map(fn).flatMapU(this::provide);
    }

    public <R1> UJIO<R1, A> provideFrom(UJIO<R1, R> jio) {
        return jio.flatMapU(this::provide);
    }

    /** Executes the given effect after this one. The effect must have a compatible environment. If not, you can align both
     * effects' environments to the same type by using .provideFrom() on both before flatMap(). */
    public <U> UJIO<R, U> flatMapU(Function<? super A, UJIO<? super R, ? extends U>> fn) {
        return new UJIO<>(zio.flatMap(a -> fn.apply(a).zio, Trace.empty()));
    }

    /** Executes the given effect after this one. The effect must have a compatible environment. If not, you can align both
     * effects' environments to the same type by using .provideFrom() on both before flatMap(). */
    public <U, E> JIO<R, E, U> flatMap(Function<? super A, JIO<? super R, ? extends E, ? extends U>> fn) {
        return new JIO<>(zio.flatMap(a -> fn.apply(a).zio, Trace.empty()));
    }

    public <U> UJIO<R, U> map(Function<? super A, ? extends U> fn) {
        return flatMapU(a -> JIO.succeed(fn.apply(a)));
    }

    public <U> UJIO<R, U> as(U value) {
        return map(a -> value);
    }

    /** Casts to <U>, which must be a supertype of <A>. Unsafe since <U super A> is not possible to declare in Java. */
    @SuppressWarnings("unchecked")
    <U> UJIO<R,U> unsafeCast() {
        return (UJIO<R,U>) this;
    }

    public <B> UJIO<R,B> repeat(Schedule<? super R, ? super A, ? extends B> schedule) {
        return new UJIO<>(zio.repeat(() -> Schedule.<R,A,B>cast(schedule).schedule, Trace.empty()));
    }

    public <B> UJIO<R,Tuple2<A,B>> zip(UJIO<? super R, ? extends B> that) {
        return zipWith(that, Tuple::of);
    }

    public <B,O> UJIO<R,O> zipWith(UJIO<? super R, ? extends B> that, BiFunction<A,B,O> fn) {
        return new UJIO<R,Object>(zio.zip(() -> that.zio, (a,b) -> fn.apply(a,b), Trace.empty())).<O>unsafeCast();
    }

    /// ------ only for UJIO --------

    @SuppressWarnings("unchecked")
    public <E> JIO<R,E,A> toJIO() {
        return new JIO<R,E,A>((ZIO<Dependencies,E,A>) zio);
    }
}
