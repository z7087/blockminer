package me.z7087.blockminer.util;

import me.z7087.blockminer.BlockMinerMod;

// 易变量不要常量化，编译也需要时间
public final class BlockBreakUtils {
    private BlockBreakUtils() {}

    public static BlockBreakUtils createInstance() {
        return new BlockBreakUtils();
    }

    private boolean breaking = false;

    public boolean isModBreakingBlock() {
        if (!BlockMinerMod.getInstance().getTaskManager().isEnabled()) {
            return false;
        }
        return breaking;
    }

    public void setBreaking(boolean breaking) {
        this.breaking = breaking;
    }
}
