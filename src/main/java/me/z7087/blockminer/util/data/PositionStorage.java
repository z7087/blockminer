package me.z7087.blockminer.util.data;

import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import me.z7087.final2constant.Constant;
import me.z7087.final2constant.util.JavaHelper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

public abstract class PositionStorage implements LongIterable {
    private PositionStorage() {}

    private static final MethodHandle CONSTRUCTOR;
    static {
        final String[] immutableNames, immutableDescriptors;
        try {
            PositionStorage positionStorageEmptyImpl = Constant.factory.ofEmptyAbstractImplInstance(
                    MethodHandles.lookup(),
                    PositionStorage.class
            );
            final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                    MethodHandles.lookup(),
                    (Supplier<LongLinkedOpenHashSet> & Serializable) positionStorageEmptyImpl::positionSet
            );
            immutableNames = immutableNamesAndDescriptors[0];
            immutableDescriptors = immutableNamesAndDescriptors[1];
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        CONSTRUCTOR = Constant.factory.ofRecordConstructor(
                MethodHandles.lookup(),
                PositionStorage.class,
                false,
                immutableNames,
                immutableDescriptors,
                null,
                null,
                true,
                false
        );
    }

    public static PositionStorage createInstance() {
        final LongLinkedOpenHashSet positionSet = new LongLinkedOpenHashSet();
        try {
            return (PositionStorage) CONSTRUCTOR.invokeExact(
                    positionSet
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    abstract LongLinkedOpenHashSet positionSet();

    public final void clear() {
        positionSet().clear();
    }

    public final boolean hasPos(BlockPos pos) {
        return positionSet().contains(pos.asLong());
    }

    public final void registerPos(BlockPos pos) {
        positionSet().add(pos.asLong());
    }

    public final @NotNull LongListIterator iterator() {
        return positionSet().iterator();
    }
}
