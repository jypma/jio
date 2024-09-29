package net.ypmania.jio.function;

import java.util.function.Function;

public interface CheckedFunction1<T1,R,X extends Throwable> {
    public R apply(T1 t1) throws X;

    /**
     * Returns an unchecked function that will <em>sneaky throw</em> if an exceptions occurs when applying the function.
     */
    default Function<T1,R> unchecked() {
        return (t1) -> {
            try {
                return apply(t1);
            } catch (Throwable t) {
                return SneakyThrow.sneakyThrow(t);
            }
        };
    }
}
