package me.z7087.blockminer.util.data;

import me.z7087.final2constant.Constant;
import me.z7087.final2constant.util.JavaHelper;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntFunction;

public abstract class UniqueObjectCollection<T> {
    protected UniqueObjectCollection() {
    }

    private static final MethodHandle CONSTRUCTOR;

    static {
        final String[] immutableNames, immutableDescriptors;
        try {
            final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                    MethodHandles.lookup(),
                    (Function<UniqueObjectCollection<?>, String> & Serializable) UniqueObjectCollection::name,
                    (Function<UniqueObjectCollection<?>, Object[]> & Serializable) UniqueObjectCollection::originalObjects,
                    (Function<UniqueObjectCollection<?>, UniqueObject<?>[]> & Serializable) UniqueObjectCollection::uniqueObjects
            );
            immutableNames = immutableNamesAndDescriptors[0];
            immutableDescriptors = immutableNamesAndDescriptors[1];
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        CONSTRUCTOR = Constant.factory.ofRecordConstructor(
                MethodHandles.lookup(),
                UniqueObjectCollection.class,
                false,
                immutableNames,
                immutableDescriptors,
                null,
                null,
                true,
                false
        );
    }

    public static <T> UniqueObjectCollection<T> createInstance(String name, IntFunction<T[]> arrayProvider, Collection<T> originalObjects) {
        final UniqueObjectPool<T> pool = name != null ? UniqueObjectPool.withSInt32Limit(name) : UniqueObjectPool.withSInt32Limit();
        final T[] originalObjectsArray = originalObjects.toArray(arrayProvider.apply(originalObjects.size()));
        final UniqueObject<T>[] uniqueObjects = UniqueObjectPool.createUniqueObjectArray(pool, originalObjects);
        try {
            @SuppressWarnings("unchecked") final UniqueObjectCollection<T> instance = (UniqueObjectCollection<T>) CONSTRUCTOR.invokeExact(
                    name,
                    originalObjectsArray,
                    uniqueObjects
            );
            return instance;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    abstract String name();

    abstract T[] originalObjects();

    abstract UniqueObject<T>[] uniqueObjects();

    public int size() {
        return uniqueObjects().length;
    }

    public T[] getOriginalObjects() {
        return originalObjects();
    }

    public UniqueObject<T>[] getUniqueObjects() {
        return uniqueObjects();
    }

    public String getName() {
        return name();
    }
}
