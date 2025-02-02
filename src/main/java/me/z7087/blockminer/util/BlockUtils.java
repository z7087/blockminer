package me.z7087.blockminer.util;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.util.enums.DistanceCalculationMode;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

public final class BlockUtils {
    private BlockUtils() {}

    public static int getDistance(BlockPos pos1, BlockPos pos2) {
        BlockPos pos3 = pos1.subtract(pos2);
        return Math.abs(pos3.getX()) + Math.abs(pos3.getY()) + Math.abs(pos3.getZ());
    }

    public static BlockPos clampToValidPos(BlockPos pos, World world) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        final double minX, maxX, minZ, maxZ;
        {
            final WorldBorder worldBorder = world.getWorldBorder();
            minX = worldBorder.getBoundWest();
            maxX = worldBorder.getBoundEast();
            minZ = worldBorder.getBoundNorth();
            maxZ = worldBorder.getBoundSouth();
        }
        final int minY, maxY;
        minY =
                //#if MC >= 11700
                world.getBottomY()
                //#else
                //$$ 0
                //#endif
        ;
        maxY = minY + world.getHeight();
        if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ)
            return pos;

        if (x < minX)
            x = MathHelper.ceil(minX);
        else if (x > maxX)
            x = MathHelper.floor(maxX);

        if (y < minY)
            y = MathHelper.ceil(minY);
        else if (y > maxY)
            y = MathHelper.floor(maxY);

        if (z < minZ)
            z = MathHelper.ceil(minZ);
        else if (z > maxZ)
            z = MathHelper.floor(maxZ);

        return new BlockPos(x, y, z);
    }

    public static ActionResult interactBlock(ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player, ClientWorld ignoredWorld, Hand hand, BlockHitResult hitResult) {
        //#if MC >= 11900
        return interactionManager.interactBlock(player, hand, hitResult);
        //#else
        //$$ return interactionManager.interactBlock(player, ignoredWorld, hand, hitResult);
        //#endif
    }

    public static boolean isReplaceable(BlockState blockState) {
        //#if MC >= 11903
        return blockState.isReplaceable();
        //#else
        //$$ return blockState.getMaterial().isReplaceable();
        //#endif
    }

    public static float getHardness(BlockState blockState) {
        return blockState.getHardness(null, null);
    }

    private static Vec3d getEyePos(PlayerEntity player) {
        //#if MC >= 11700
        return player.getEyePos();
        //#else
        //$$ return new Vec3d(player.getX(), player.getEyeY(), player.getZ());
        //#endif
    }

    public static boolean playerCanTouchServerside(PlayerEntity player,
                                                   BlockPos blockPos,
                                                   double additionalRange) {
        // isMine在为true时拥有较小的容错
        return playerCanTouchServerside(player, blockPos, additionalRange, true);
    }

    public static boolean playerCanTouchServerside(PlayerEntity player,
                                                   BlockPos blockPos,
                                                   double additionalRange,
                                                   boolean isMine) {
        return playerCanTouchServerside(player, blockPos, additionalRange, BlockMinerMod.INSTANCE.config.distanceCalculationMode, isMine);
    }

    public static boolean playerCanTouchServerside(PlayerEntity player,
                                                   BlockPos blockPos,
                                                   double additionalRange,
                                                   DistanceCalculationMode mode,
                                                   boolean isMine) {
        double blockPosX = blockPos.getX();
        double blockPosY = blockPos.getY();
        double blockPosZ = blockPos.getZ();
        // <和<=号是源码里写的 尽量不要混用
        switch (mode) {
            case V1_20_6: {
                double distance = 4.5 + additionalRange;
                Vec3d eyePos = getEyePos(player);
                double eyePosX = eyePos.getX();
                double eyePosY = eyePos.getY();
                double eyePosZ = eyePos.getZ();
                double xOffset = Math.max(Math.max(blockPosX - eyePosX, eyePosX - (blockPosX + 1)), 0);
                double yOffset = Math.max(Math.max(blockPosY - eyePosY, eyePosY - (blockPosY + 1)), 0);
                double zOffset = Math.max(Math.max(blockPosZ - eyePosZ, eyePosZ - (blockPosZ + 1)), 0);
                return ((xOffset * xOffset) + (yOffset * yOffset) + (zOffset * zOffset)) < (distance * distance);
            }
            case V1_19: {
                double distance = 5 + additionalRange;
                Vec3d eyePos = getEyePos(player);
                double xOffset = eyePos.getX() - (blockPosX + 0.5);
                double yOffset = eyePos.getY() - (blockPosY + 0.5);
                double zOffset = eyePos.getZ() - (blockPosZ + 0.5);
                return ((xOffset * xOffset) + (yOffset * yOffset) + (zOffset * zOffset)) <= (distance * distance);
            }
            default: {
                if (isMine) {
                    double distance = 5 + additionalRange;
                    double xOffset = player.getX() - (blockPosX + 0.5);
                    double yOffset = player.getY() - (blockPosY + 0.5) + 1.5;
                    double zOffset = player.getZ() - (blockPosZ + 0.5);
                    return ((xOffset * xOffset) + (yOffset * yOffset) + (zOffset * zOffset)) <= (distance * distance);
                } else {
                    // 对于1.18.2及以前的版本，放置方块有一个超高的threshold
                    double distance = 7 + additionalRange;
                    double xOffset = player.getX() - (blockPosX + 0.5);
                    double yOffset = player.getY() - (blockPosY + 0.5);
                    double zOffset = player.getZ() - (blockPosZ + 0.5);
                    return ((xOffset * xOffset) + (yOffset * yOffset) + (zOffset * zOffset)) < (distance * distance);
                }
            }
        }
    }
}
