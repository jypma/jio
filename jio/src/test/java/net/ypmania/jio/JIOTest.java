package net.ypmania.jio;

import de.tobiasroeser.lambdatest.junit5.FreeSpec;
import net.ypmania.jio.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public final class JIOTest extends FreeSpec {
    interface HasString {
        String string();
    }
    interface HasInteger {
        Integer integer();
    }

    private <T extends HasString & HasInteger> UJIO<T,T> dependencies() {
        return JIO.<T> environment();
    }

    private <T extends HasString & Scope.Has> UJIO<T,T> dependenciesWithScope() {
        return JIO.<T> environment();
    }

    record Dependencies(String string, Integer integer) implements HasString, HasInteger {}
    record DependenciesWithScope(String string, Scope scope) implements HasString, Scope.Has {}

    private int failIntWithIOException() throws IOException {
        throw new IOException("simulated failure");
    }
    private void failVoidWithIOException() throws IOException {
        throw new IOException("simulated failure");
    }

    {
        section("flatMap", () -> {
            test("should infer types from lambdas", () -> {
                var res = JIO.succeed("15").flatMapU(s -> JIO.succeed(Integer.parseInt(s)));
                assertThat(Runtime.runtime.unsafeRun(res).get(), equalTo(15));
            });

            test("should (can) generalize error type", () -> {
                JIO<Object, Integer, String> fail1 = JIO.fail(42);
                JIO<Object, Long, String> fail2 = JIO.fail(42L);

                // Unfortunately, Java cannot seem to symmetrically infer the highest subtype of two types,
                // unless we move flatMap to a static method.
                @SuppressWarnings("unused")
                JIO<Object, Number, String> compileCheck = JIO.<Object,Number,String>cast(fail1).flatMap(s -> fail2);

                @SuppressWarnings("unused")
                JIO<Object, Number, String> compileCheck2 = JIO.flatMap(fail1, s -> fail2);
            });
        });

        section("provide", () -> {
            test("should fulfill dependencies", () -> {
                var service = JIO.<String>environment().map(s -> Integer.parseInt(s));
                var res = service.provide("15");
                assertThat(Runtime.runtime.unsafeRun(res).get(), equalTo(15));
            });

            test("should fulfill multiple dependencies as intersection type", () -> {
                var service = dependencies().map(d -> d.string() + " / " + d.integer());
                var res = service.provide(new Dependencies("hello", 15));
                assertThat(Runtime.runtime.unsafeRun(res).get(), equalTo("hello / 15"));
            });

            test("should map from another type into the environment", () -> {
                var service = JIO.<String>environment().map(s -> Integer.parseInt(s));
                var withDeps = service.<Dependencies>provideFrom(d -> d.string());
                var res = withDeps.provide(new Dependencies("15", 42));
                assertThat(Runtime.runtime.unsafeRun(res).get(), equalTo(15));
            });

            test("should map from a JIO, then depending on its dependencies", () -> {
                var service1 = JIO.<String>environment().map(s -> Integer.parseInt(s));
                var service2 = JIO.<Integer>environment().map(i -> i * 2);
                var withDeps = service2.provideFrom(service1);
                var res = withDeps.provide("15");
                assertThat(Runtime.runtime.unsafeRun(res).get(), equalTo(30));
            });
        });

        section("zip", () -> {
            test("should create tuple", () -> {
                var effect = JIO.succeed(42).zip(JIO.succeed("hello"));
                assertThat(Runtime.runtime.unsafeRun(effect).get(),
                    equalTo(Tuple.of(42, "hello")));
            });
        });

        section("attempt", () -> {
            test("should wrap a checked exception from a function", () -> {
                @SuppressWarnings("unused")
                JIO<Object,IOException,Integer> jio = JIO.attempt(() -> failIntWithIOException());
            });

            test("should wrap a checked exception from a runnable", () -> {
                @SuppressWarnings("unused")
                JIO<Object,IOException,Object> jio = JIO.attempt(() -> failVoidWithIOException());
            });
        });

        section("catchAllU", () -> {
            test("should handle all errors", () -> {
                JIO<Object, String, Integer> failure = JIO.fail("42");
                var res = failure.catchAllU(s -> JIO.succeed(Integer.parseInt(s)));
                assertThat(Runtime.runtime.unsafeRun(res).get(), equalTo(42));
            });
        });

        section("mapError", () -> {
            test("should change error type with type inference", () -> {
                JIO<Object, String, Void> service = JIO.fail("42");
                var res = service.mapError(s -> Integer.parseInt(s));
                assertThat(Runtime.runtime.unsafeRun(res.flip().catchAllU(o -> JIO.succeed(0))).get(), equalTo(42));
            });
        });

        section("flatMapError", () -> {
            test("should change error type with type inference", () -> {
                JIO<Object, String, Void> service = JIO.fail("42");
                var res = service.flatMapError(s -> JIO.succeed(Integer.parseInt(s)));
                assertThat(Runtime.runtime.unsafeRun(res.flip().catchAllU(o -> JIO.succeed(0))).get(), equalTo(42));
            });
        });

        section("scope", () -> {
            test("acquireRelease should release at end of scope", () -> {
                var counter = new AtomicInteger();
                var service = JIO.acquireReleaseU(JIO.succeedWith(() -> {
                    counter.incrementAndGet();
                    return counter;
                }), c -> JIO.succeedWith(() -> c.decrementAndGet()));
                var result = new int[1];
                var res = JIO.scoped(
                    service.map(c -> {
                        result[0] = c.get();
                        return c;
                    })
                );
                Runtime.runtime.unsafeRun(res).get();
                assertThat(result[0], equalTo(1));
            });

            test("scoped should combine with other service dependencies", () -> {
                var scopedService = dependenciesWithScope().map(d -> d.string());
                var res = JIO.scoped(
                    scopedService, (String s, Scope c) -> new DependenciesWithScope(s, c)
                ).provide("Hello");
                assertThat(Runtime.runtime.unsafeRun(res).get(), equalTo("Hello"));
            });
        });

        section("schedule", () -> {
            test("repeat should repeat", () -> {
                var counter = new AtomicInteger();
                var effect = JIO.succeedWith(() -> counter.incrementAndGet()).repeat(Schedule.recurs(2));
                Runtime.runtime.unsafeRun(effect).get();
                // Effect runs once on its own, twice from the recurs.
                assertThat(counter.get(), equalTo(3));
            });

            test("retry should retry", () -> {
                var counter = new AtomicInteger();
                var effect = JIO
                    .succeedWith(() -> counter.incrementAndGet())
                    .flatMap(d -> JIO.fail(42))
                    .retry(Schedule.recurs(2))
                    .catchAllU(i -> JIO.empty());
                Runtime.runtime.unsafeRun(effect).get();
                // Effect runs once on its own, twice from the recurs.
                assertThat(counter.get(), equalTo(3));
            });

        });
    }
}
