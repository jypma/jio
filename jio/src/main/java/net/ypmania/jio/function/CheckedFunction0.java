package net.ypmania.jio.function;

import java.util.function.Supplier;

public interface CheckedFunction0<R, X extends Throwable> {
    public R apply() throws X;

    /**
     * Returns an unchecked function that will <em>sneaky throw</em> if an exceptions occurs when applying the function.
     */
    default Supplier<R> unchecked() {
        return () -> {
            try {
                return apply();
            } catch (Throwable t) {
                return SneakyThrow.sneakyThrow(t);
            }
        };
    }
}
