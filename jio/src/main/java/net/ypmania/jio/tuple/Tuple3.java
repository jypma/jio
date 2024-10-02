package net.ypmania.jio.tuple;

public record Tuple3<T1,T2,T3>(T1 _1, T2 _2, T3 _3) {
    public <T4> Tuple4<T1,T2,T3,T4> zip(T4 _4) {
        return new Tuple4<>(_1, _2, _3, _4);
    }

}
