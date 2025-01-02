package me.z7087.blockminer.util.data;

import java.util.Objects;

public final class Pair4<F, S, T, FO> {
    public final F first;
    public final S second;
    public final T third;
    public final FO fourth;

    public Pair4(F first, S second, T third, FO fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    public static <F, S, T, FO> Pair4<F, S, T, FO> of(F first, S second, T third, FO fourth) {
        return new Pair4<>(first, second, third, fourth);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pair4) {
            Pair4<?, ?, ?, ?> pair4 = (Pair4<?, ?, ?, ?>) o;
            return Objects.equals(first, pair4.first) && Objects.equals(second, pair4.second) && Objects.equals(third, pair4.third) && Objects.equals(fourth, pair4.fourth);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(first);
        result = 31 * result + Objects.hashCode(second);
        result = 31 * result + Objects.hashCode(third);
        result = 31 * result + Objects.hashCode(fourth);
        return result;
    }

    @Override
    public String toString() {
        return "Pair4(" + first +
                ", " + second +
                ", " + third +
                ", " + fourth +
                ')';
    }
}