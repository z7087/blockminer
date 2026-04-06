package me.z7087.blockminer.util;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.mixin.minecraft.client.network.ClientPlayerEntityAccessor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public final class UncertainManager {
    public enum YawDirection {
        NORTH,
        SOUTH,
        WEST,
        EAST;

        public static byte yaw2Id(float yaw) {
            float normalizedYaw = yaw - (MathHelper.floor(yaw / 360f) * 360f);
            if (normalizedYaw <= 45f) {
                return 1;
            } else if (normalizedYaw < 135f) {
                return 2;
            } else if (normalizedYaw <= 225f) {
                return 0;
            } else if (normalizedYaw < 315f) {
                return 3;
            } else {
                return 1;
            }
        }
        public static YawDirection id2Direction(byte yawId) {
            switch (yawId) {
                case 0:
                    return NORTH;
                case 1:
                    return SOUTH;
                case 2:
                    return WEST;
                case 3:
                    return EAST;
                default:
                    throw new IllegalArgumentException("Invalid yaw id " + yawId);
            }
        }
        public static Direction id2MCDirection(byte yawId) {
            switch (yawId) {
                case 0:
                    return Direction.NORTH;
                case 1:
                    return Direction.SOUTH;
                case 2:
                    return Direction.WEST;
                case 3:
                    return Direction.EAST;
                default:
                    throw new IllegalArgumentException("Invalid yaw id " + yawId);
            }
        }

        public static float id2Yaw(byte yawId) {
            switch (yawId) {
                case 0:
                    return 180f;
                case 1:
                    return 0f;
                case 2:
                    return 90f;
                case 3:
                    return 270f;
                default:
                    throw new IllegalArgumentException("Invalid yaw id " + yawId);
            }
        }
    }

    // all tick counts here are unsigned

    private byte hotbarSlot;
    private int hotbarSlotKeepingTicks;
    private ItemStack mainHandItem;
    private int mainHandItemKeepingTicks;

    // id of YawDirections
    private byte yawDirectionId;
    private int yawDirectionKeepingTicks;
    // there's no pitch uncertain

    public void tick() {
        hotbarSlotKeepingTicks = upSafe(hotbarSlotKeepingTicks);
        mainHandItemKeepingTicks = upSafe(mainHandItemKeepingTicks);
        yawDirectionKeepingTicks = upSafe(yawDirectionKeepingTicks);
    }

    // yaw will be updated via mixin
    public void check(int hotbarSlot, ItemStack mainHandItem) {
        onHotbarSlotUpdate(hotbarSlot);
        onMainHandItemUpdate(mainHandItem);
    }

    public void tickAndCheck() {
        tick();
        final BlockMinerMod.TicklyUpdateConstants ticklyUpdateConstants = BlockMinerMod.getInstance().ticklyUpdateConstants();
        check(
                InventoryUtils.getSelectedSlot(ticklyUpdateConstants.inventory()),
                ticklyUpdateConstants.inventory().getSelectedStack()
        );
    }

    public boolean onHotbarSlotUpdate(int hotbarSlot) {
        final byte slot = (byte) hotbarSlot;
        if (this.hotbarSlot != slot) {
            this.hotbarSlot = slot;
            hotbarSlotKeepingTicks = 0;
            return true;
        }
        return false;
    }

    public boolean onMainHandItemUpdate(ItemStack mainHandItem) {
        if (!ItemStack.areItemsEqual(this.mainHandItem, mainHandItem)) {
            this.mainHandItem = mainHandItem;
            mainHandItemKeepingTicks = 0;
            return true;
        }
        return false;
    }

    public boolean onYawDirectionUpdate(float yaw) {
        final byte yawId = YawDirection.yaw2Id(yaw);
        if (this.yawDirectionId != yawId) {
            this.yawDirectionId = yawId;
            yawDirectionKeepingTicks = 0;
            return true;
        }
        return false;
    }

    public void startup() {
        reset();
        final BlockMinerMod.TicklyUpdateConstants ticklyUpdateConstants = BlockMinerMod.getInstance().ticklyUpdateConstants();
        hotbarSlot = (byte) InventoryUtils.getSelectedSlot(ticklyUpdateConstants.inventory());
        mainHandItem = ticklyUpdateConstants.inventory().getSelectedStack();
        yawDirectionId = YawDirection.yaw2Id(((ClientPlayerEntityAccessor) ticklyUpdateConstants.player()).getLastYaw());
    }

    public void reset() {
        hotbarSlot = 0;
        hotbarSlotKeepingTicks = 0;
        mainHandItem = null;
        mainHandItemKeepingTicks = 0;
        yawDirectionId = 0;
        yawDirectionKeepingTicks = 0;
    }

    public boolean isHotbarSlotKeeping(int unsignedTicks) {
        return Integer.compareUnsigned(hotbarSlotKeepingTicks, unsignedTicks) >= 0;
    }

    // 主手效率持续检测尚未实现
    // 在每tick调用的tickAndCheck中检查的mainHandItem变化或许已经足够了？（主要是监控物品栏好麻烦
    public boolean isMainHandItemKeeping(int unsignedTicks) {
        return Integer.compareUnsigned(mainHandItemKeepingTicks, unsignedTicks) >= 0;
    }

    public boolean isYawDirectionKeeping(int unsignedTicks) {
        return Integer.compareUnsigned(yawDirectionKeepingTicks, unsignedTicks) >= 0;
    }

    private static int upSafe(int n) {
        // increment from 0 up to -1
        int m = n + 1;
        if (m == 0) m = n;
        return m;
    }

}
