package net.ypmania.jio;

import de.tobiasroeser.lambdatest.junit5.FreeSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class JIOTest extends FreeSpec {
    interface HasString {
        String string();
    }
    interface HasInteger {
        Integer integer();
    }

    private <T extends HasString & HasInteger> UJIO<T,T> dependencies() {
        return JIO.<T> service();
    }

    record Dependencies(String string, Integer integer) implements HasString, HasInteger {}

    {
        section("flatMap", () -> {
            test("should infer types from lambdas", () -> {
                var res = JIO.succeed("15").flatMapU(s -> JIO.succeed(Integer.parseInt(s)));
                assertThat(Runtime.getDefault().unsafeGet(res).get(), equalTo(15));
            });

            test("should generalize  error type", () -> {
                JIO<Object, Integer, String> fail1 = JIO.fail(42);
                JIO<Object, Long, String> fail2 = JIO.fail(42L);
                JIO<Object, Number, String> check = fail1.flatMap(s -> fail2);
            });
        });

        section("provide", () -> {
            test("should fulfill dependencies", () -> {
                var service = JIO.<String> service().map(s -> Integer.parseInt(s));
                var res = service.provide("15");
                assertThat(Runtime.getDefault().unsafeGet(res).get(), equalTo(15));
            });

            test("should fulfill multiple dependencies as intersection type", () -> {
                var service = dependencies().map(d -> d.string() + " / " + d.integer());
                var res = service.provide(new Dependencies("hello", 15));
                assertThat(Runtime.getDefault().unsafeGet(res).get(), equalTo("hello / 15"));
            });

            test("should map from another type into the environment", () -> {
                var service = JIO.<String> service().map(s -> Integer.parseInt(s));
                var withDeps = service.<Dependencies>provideFrom(d -> d.string());
                var res = withDeps.provide(new Dependencies("15", 42));
                assertThat(Runtime.getDefault().unsafeGet(res).get(), equalTo(15));
            });

            test("should map from a JIO, then depending on its dependencies", () -> {
                var service1 = JIO.<String> service().map(s -> Integer.parseInt(s));
                var service2 = JIO.<Integer> service().map(i -> i * 2);
                var withDeps = service2.provideFrom(service1);
                var res = withDeps.provide("15");
                assertThat(Runtime.getDefault().unsafeGet(res).get(), equalTo(30));
            });
        });
        
        section("catchAllU", () -> {
            test("should handle all errors", () -> {
                JIO<Object, String, Integer> failure = JIO.fail("42");
                var res = failure.catchAllU(s -> JIO.succeed(Integer.parseInt(s)));
                assertThat(Runtime.getDefault().unsafeGet(res).get(), equalTo(42));
            });
        });
    }
}
