package me.z7087.blockminer.util;

import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;
import me.z7087.final2constant.util.JavaHelper;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

public abstract class BlockBreakUtils {
    private BlockBreakUtils() {}

    private static final MethodHandle CONSTRUCTOR;
    static {
        final String[] immutableNames, immutableDescriptors;
        try {
            BlockBreakUtils blockBreakUtilsEmptyImpl = Constant.factory.ofEmptyAbstractImplInstance(
                    MethodHandles.lookup(),
                    BlockBreakUtils.class
            );
            final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                    MethodHandles.lookup(),
                    (Supplier<DynamicConstant<Boolean>> & Serializable) blockBreakUtilsEmptyImpl::breaking
            );
            immutableNames = immutableNamesAndDescriptors[0];
            immutableDescriptors = immutableNamesAndDescriptors[1];
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        CONSTRUCTOR = Constant.factory.ofRecordConstructor(
                MethodHandles.lookup(),
                BlockBreakUtils.class,
                false,
                immutableNames,
                immutableDescriptors,
                null,
                null,
                true,
                false
        );
    }

    public static BlockBreakUtils createInstance() {
        try {
            return (BlockBreakUtils) CONSTRUCTOR.invokeExact(
                    Constant.factory.ofMutable(Boolean.FALSE)
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    abstract DynamicConstant<Boolean> breaking();

    public boolean isModBreakingBlock() {
        return breaking().orElseThrow();
    }

    public void setBreaking(boolean breaking) {
        breaking().set(breaking);
    }
}
