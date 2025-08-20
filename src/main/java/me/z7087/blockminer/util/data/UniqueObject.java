package me.z7087.blockminer.util.data;

public final class UniqueObject<T> implements Comparable<UniqueObject<T>> {
    private final long id;
    private final T o;

    UniqueObject(long id, T o) {
        this.id = id;
        this.o = o;
    }

    public T get() {
        return o;
    }

    public long id() {
        return id;
    }

    @Override
    public int hashCode() {
        return (int) id;
    }

    @Override
    public int compareTo(UniqueObject<T> other) {
        return Long.compare(this.id, other.id);
    }

    @Override
    public String toString() {
        return "UniqueObject{" +
                "id=" + id +
                ", o=" + o +
                '}';
    }
}
