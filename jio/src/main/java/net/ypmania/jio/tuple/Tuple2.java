package net.ypmania.jio.tuple;

public record Tuple2<T1,T2>(T1 _1, T2 _2) {
    public <T3> Tuple3<T1,T2,T3> zip(T3 _3) {
        return new Tuple3<>(_1, _2, _3);
    }
}
