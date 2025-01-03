package me.z7087.blockminer.util;

public final class BlockBreakUtils {
    private boolean breaking = false;
    public boolean isModBreakingBlock() {
        return breaking;
    }

    public void setBreaking(boolean breaking) {
        this.breaking = breaking;
    }
}
