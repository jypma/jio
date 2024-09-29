package net.ypmania.jio.function;

class SneakyThrow {
    /** Throws the given exception without it being visible in the type signature. We need this because Scala does not expose
     * checked exceptions. */
    @SuppressWarnings("unchecked")
    static <T extends Throwable, R> R sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
