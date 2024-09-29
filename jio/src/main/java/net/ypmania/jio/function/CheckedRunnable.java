package net.ypmania.jio.function;

public interface CheckedRunnable<X extends Throwable> {
    public void run() throws X;

    /**
     * Returns an unchecked function that will <em>sneaky throw</em> if an exceptions occurs when applying the function.
     */
    default Runnable unchecked() {
        return () -> {
            try {
                run();
            } catch (Throwable t) {
                SneakyThrow.sneakyThrow(t);
            }
        };
    }
}
