package net.ypmania.jio;

import java.time.Duration;

import net.ypmania.ziojava.Dependencies;
import zio.Trace;

@SuppressWarnings("unchecked")
public class Schedule<R,I,O> {
    public static Schedule<Object,Object,Long> recurs(long n) {
        return wrap(zio.Schedule.recurs(n, Trace.empty()));
    }

    public static Schedule<Object,Object,Long> once() {
        return wrap(zio.Schedule.once(Trace.empty()));
    }

    public static Schedule<Object,Object,Object> stop() {
        return wrap(zio.Schedule.stop(Trace.empty()));
    }

    public static Schedule<Object,Object,Long> spaced(Duration duration) {
        return wrap(zio.Schedule.spaced(duration, Trace.empty()));
    }

    public static <I,O> Schedule<Object,I,O> wrap(zio.Schedule<Object, ? super I, ? extends O> s) {
        return new Schedule<Object,I,O>(Dependencies.wrap(s));
    }

    public static <R,I,O> Schedule<R,I,O> cast(Schedule<? super R, ? super I, ? extends O> s) {
        return (Schedule<R, I, O>) s;
    }

    zio.Schedule<Dependencies,I,O> schedule;

    Schedule(zio.Schedule<Dependencies, ? super I, ? extends O> schedule) {
        this.schedule = (zio.Schedule<Dependencies, I, O>) schedule;
    }
}
