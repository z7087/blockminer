package me.z7087.blockminer.util;

import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public abstract class BlockBreakUtils {
    private BlockBreakUtils() {}

    private static final MethodHandle CONSTRUCTOR = Constant.factory.ofRecordConstructor(
            MethodHandles.lookup(),
            BlockBreakUtils.class,
            false,
            new String[] {
                    "breaking"
            },
            new String[] {
                    Type.getDescriptor(DynamicConstant.class)
            },
            null,
            null,
            true,
            false
    );

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
