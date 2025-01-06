package me.z7087.blockminer.util.data;

import java.util.Objects;

public final class Pair3<F, S, T> {
    public final F first;
    public final S second;
    public final T third;

    public Pair3(F first, S second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public static <F, S, T> Pair3<F, S, T> of(F first, S second, T third) {
        return new Pair3<>(first, second, third);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pair3) {
            Pair3<?, ?, ?> pair3 = (Pair3<?, ?, ?>) o;
            return Objects.equals(first, pair3.first) && Objects.equals(second, pair3.second) && Objects.equals(third, pair3.third);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(first);
        result = 31 * result + Objects.hashCode(second);
        result = 31 * result + Objects.hashCode(third);
        return result;
    }

    @Override
    public String toString() {
        return "Pair3(" + first +
                ", " + second +
                ", " + third +
                ')';
    }
}