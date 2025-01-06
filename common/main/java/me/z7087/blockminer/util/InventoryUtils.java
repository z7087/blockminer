package me.z7087.blockminer.util;

import me.z7087.blockminer.multiversion.EntityAttributesImpl;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class InventoryUtils {
    private InventoryUtils() {}

    public static int findFirstItemInHotbar(PlayerInventory inventory, Item item) {
        DefaultedList<ItemStack> main = inventory.main;
        for (int i = 0, size = PlayerInventory.getHotbarSize(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
    public static int findFirstItemInHotbar(PlayerInventory inventory, Predicate<ItemStack> predicate) {
        DefaultedList<ItemStack> main = inventory.main;
        for (int i = 0, size = PlayerInventory.getHotbarSize(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }
    public static int findFirstItemInHotbar(PlayerInventory inventory, Item item, Predicate<ItemStack> predicate) {
        DefaultedList<ItemStack> main = inventory.main;
        for (int i = 0, size = PlayerInventory.getHotbarSize(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == item && predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }
    public static int findFirstItem(PlayerInventory inventory, Item item) {
        DefaultedList<ItemStack> main = inventory.main;
        for (int i = 0, size = main.size(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
    public static int findFirstItem(PlayerInventory inventory, Predicate<ItemStack> predicate) {
        DefaultedList<ItemStack> main = inventory.main;
        for (int i = 0, size = main.size(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }
    public static int findFirstItem(PlayerInventory inventory, Item item, Predicate<ItemStack> predicate) {
        DefaultedList<ItemStack> main = inventory.main;
        for (int i = 0, size = main.size(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == item && predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }
    public static int findBestItemInHotbar(PlayerInventory inventory, Predicate<ItemStack> predicate, Comparator<ItemStack> comparator) {
        DefaultedList<ItemStack> main = inventory.main;
        for (int i = 0, size = PlayerInventory.getHotbarSize(); i < size; ++i) {
            ItemStack bestStack = main.get(i);
            if (predicate.test(bestStack)) {
                int bestIndex = i++;
                for (; i < size; ++i) {
                    ItemStack stack = main.get(i);
                    if (predicate.test(stack) && comparator.compare(bestStack, stack) < 0) {
                        bestStack = stack;
                        bestIndex = i;
                    }
                }
                return bestIndex;
            }
        }
        return -1;
    }
    public static int findBestItem(PlayerInventory inventory, Predicate<ItemStack> predicate, Comparator<ItemStack> comparator) {
        DefaultedList<ItemStack> main = inventory.main;
        for (int i = 0, size = main.size(); i < size; ++i) {
            ItemStack bestStack = main.get(i);
            if (predicate.test(bestStack)) {
                int bestIndex = i++;
                for (; i < size; ++i) {
                    ItemStack stack = main.get(i);
                    if (predicate.test(stack) && comparator.compare(bestStack, stack) < 0) {
                        bestStack = stack;
                        bestIndex = i;
                    }
                }
                return bestIndex;
            }
        }
        return -1;
    }

    public static <T> T moveToOffhandDuring(ClientPlayerEntity player, int hotbarSlot, Supplier<T> supplier) {
        if (player.currentScreenHandler != player.playerScreenHandler)
            throw new IllegalStateException("player.currentScreenHandler != player.playerScreenHandler");
        if (hotbarSlot < 0 || hotbarSlot > 8)
            throw new IllegalArgumentException("hotbarSlot is not in 0~8 range");
        ClientPlayerInteractionManager interactionManager = Objects.requireNonNull(MinecraftClient.getInstance().interactionManager);
        interactionManager.clickSlot(0, hotbarSlot + PlayerScreenHandler.HOTBAR_START, 40, SlotActionType.SWAP, player);
        T result = supplier.get();
        interactionManager.clickSlot(0, hotbarSlot + PlayerScreenHandler.HOTBAR_START, 40, SlotActionType.SWAP, player);
        return result;
    }

    /**
     * 获取当前物品每tick能够破坏指定方块进度的百分比.
     *
     * @param blockState 要破坏的方块状态
     * @param itemStack  使用工具/物品破坏方块
     * @return 当前物品每tick破坏该方块的百分比
     */
    public static float calcBlockBreakingDelta(ClientPlayerEntity player, BlockState blockState, ItemStack itemStack) {
        float hardness = blockState.getBlock().getHardness();
        if (hardness < 0)
            return 0;
        final int i;
        if (!blockState.isToolRequired() || player.getInventory().getMainHandStack().isSuitableFor(blockState)) {
            i = 30;
        } else {
            i = 100;
        }
        float f = itemStack.getMiningSpeedMultiplier(blockState);  // 当前物品的破坏系数速度
        // 根据工具的"效率"附魔增加破坏速度
        if (f > 1.0F) {
            // 获取itemStack的附魔集合
            for (RegistryEntry<Enchantment> enchantment : itemStack.getEnchantments().getEnchantments()) {
                Optional<RegistryKey<Enchantment>> enchantmentKey = enchantment.getKey();
                if (enchantmentKey.isPresent()) {
                    // 获取效率附魔等级
                    if (enchantmentKey.get() == Enchantments.EFFICIENCY) {
                        int toolLevel = EnchantmentHelper.getLevel(enchantment, itemStack);
                        if (toolLevel > 0 && !itemStack.isEmpty()) {
                            f += (float) (toolLevel * toolLevel + 1);
                        }
                        break;
                    }
                }
            }
        }
        // 根据玩家"急迫"状态效果增加破坏速度
        if (StatusEffectUtil.hasHaste(player)) {
            f *= 1.0F + (float) (StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
        }

        // 根据玩家"挖掘疲劳"状态效果减缓破坏速度
        if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            switch (Objects.requireNonNull(player.getStatusEffect(StatusEffects.MINING_FATIGUE)).getAmplifier()) {
                case 0: {
                    f *= 0.3F;
                    break;
                }
                case 1: {
                    f *= 0.09F;
                    break;
                }
                case 2: {
                    f *= 0.0027F;
                    break;
                }
                default: {
                    f *= 8.1E-4F;
                }
            }
        }
        f *= (float) player.getAttributeValue(EntityAttributesImpl.BLOCK_BREAK_SPEED);
        // 如果玩家在水中并且没有"水下速掘"附魔，则减缓破坏速度
        if (player.isSubmergedIn(FluidTags.WATER)) {
            EntityAttributeInstance submergedMiningSpeed = player.getAttributeInstance(EntityAttributesImpl.SUBMERGED_MINING_SPEED);
            if (submergedMiningSpeed != null) {
                f *= (float) submergedMiningSpeed.getValue();
            }
        }
        // 如果玩家不在地面上，则减缓破坏速度
        if (!player.isOnGround()) {
            f /= 5.0F;
        }
        return f / hardness / (float) i;
    }
}
