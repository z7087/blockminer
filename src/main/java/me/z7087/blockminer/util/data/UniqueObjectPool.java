package me.z7087.blockminer.util.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

// 对在一个集合内唯一的对象的包装。
// 对于哈希表中的键/集合中的元素，可以用于优化hashCode/equals计算速度、减少哈希冲突，也能在哈希表/集合过大时允许哈希表/集合对未实现Comparable的对象做红黑树。
// 由于id连续递增，应该也可以将id和对象对应上后存储在BitSet里。
// 取原始元素需要额外的时间。
public final class UniqueObjectPool<T> {
    private final AtomicLong counter = new AtomicLong();
    private final String name;
    private final long limit;

    public UniqueObjectPool() {
        this.name = null;
        this.limit = -1;
    }

    public UniqueObjectPool(String name) {
        this.name = Objects.requireNonNull(name);
        this.limit = -1;
    }

    public UniqueObjectPool(long limit) {
        this.name = null;
        this.limit = limit;
    }

    public UniqueObjectPool(String name, long limit) {
        this.name = Objects.requireNonNull(name);
        this.limit = limit;
    }

    private static final long SIGNED_INT32_MAX_VALUE = ((long) Integer.MAX_VALUE) & 0xFFFFFFFFL;
    private static final long UNSIGNED_INT32_MAX_VALUE = ((((long) Integer.MAX_VALUE) & 0xFFFFFFFFL) | (((long) Integer.MIN_VALUE) & 0xFFFFFFFFL));

    public static <F> UniqueObjectPool<F> withSInt32Limit() {
        return new UniqueObjectPool<>(SIGNED_INT32_MAX_VALUE);
    }

    public static <F> UniqueObjectPool<F> withSInt32Limit(String name) {
        return new UniqueObjectPool<>(name, SIGNED_INT32_MAX_VALUE);
    }

    public static <F> UniqueObjectPool<F> withUInt32Limit() {
        return new UniqueObjectPool<>(UNSIGNED_INT32_MAX_VALUE);
    }

    public static <F> UniqueObjectPool<F> withUInt32Limit(String name) {
        return new UniqueObjectPool<>(name, UNSIGNED_INT32_MAX_VALUE);
    }

    public UniqueObject<T> ofUnique(T o) {
        long id;
        do {
            id = counter.get();
            if (id == limit) {
                throw new IllegalStateException(name != null ? "Too many unique objects in pool '" + name + "' !" : "Too many unique objects in one pool!");
            }
        } while (!counter.compareAndSet(id, id + 1));
        return new UniqueObject<>(id, o);
    }

    public long nextId() {
        return counter.get();
    }

    @Override
    public String toString() {
        return "UniqueObjectPool(" + (name != null ? ('"' + name + "\", ") : "") + counter.get() + ')';
    }

    public static <T> UniqueObject<T>[] createUniqueObjectArray(UniqueObjectPool<T> pool, T[] originalObjects) {
        return createUniqueObjectArray(pool, originalObjects, 0);
    }

    public static <T> UniqueObject<T>[] createUniqueObjectArray(UniqueObjectPool<T> pool, Collection<T> originalObjects) {
        return createUniqueObjectArray(pool, originalObjects, 0);
    }

    public static <T> UniqueObject<T>[] createUniqueObjectArray(UniqueObjectPool<T> pool, T[] originalObjects, int poolStartIndex) {
        final int size = originalObjects.length;
        @SuppressWarnings("unchecked") final UniqueObject<T>[] uniqueObjectArray = (UniqueObject<T>[]) new UniqueObject[size];
        for (int i = 0; i < size; ++i, ++poolStartIndex) {
            long gotId;
            if (pool.nextId() == poolStartIndex) {
                final UniqueObject<T> uniqueObject = pool.ofUnique(originalObjects[i]);
                if (uniqueObject.id() == poolStartIndex) {
                    uniqueObjectArray[i] = uniqueObject;
                    continue;
                } else {
                    gotId = uniqueObject.id();
                }
            } else {
                gotId = pool.nextId();
            }
            throw new IllegalStateException("Expected index " + poolStartIndex + ", but got " + gotId + "!");
        }
        return uniqueObjectArray;
    }

    public static <T> UniqueObject<T>[] createUniqueObjectArray(UniqueObjectPool<T> pool, Collection<T> originalObjects, int poolStartIndex) {
        final int size = originalObjects.size();
        @SuppressWarnings("unchecked") final UniqueObject<T>[] uniqueObjectArray = (UniqueObject<T>[]) new UniqueObject[size];
        final Iterator<T> iterator = originalObjects.iterator();
        for (int i = 0; i < size; ++i, ++poolStartIndex) {
            if (!iterator.hasNext()) {
                throw new IllegalStateException("List iterator terminated unexpectedly");
            }
            long gotId;
            if (pool.nextId() == poolStartIndex) {
                final UniqueObject<T> uniqueObject = pool.ofUnique(iterator.next());
                if (uniqueObject.id() == poolStartIndex) {
                    uniqueObjectArray[i] = uniqueObject;
                    continue;
                } else {
                    gotId = uniqueObject.id();
                }
            } else {
                gotId = pool.nextId();
            }
            throw new IllegalStateException("Expected index " + poolStartIndex + ", but got " + gotId + "!");
        }
        return uniqueObjectArray;
    }
}
