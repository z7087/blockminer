package me.z7087.blockminer.util;

import net.minecraft.util.math.BlockPos;

public final class BlockUtils {
    private BlockUtils() {}

    public static int getDistance(BlockPos pos1, BlockPos pos2) {
        BlockPos pos3 = pos1.subtract(pos2);
        return Math.abs(pos3.getX()) + Math.abs(pos3.getY()) + Math.abs(pos3.getZ());
    }
}
