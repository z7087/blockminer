package me.z7087.blockminer.util;

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
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class InventoryUtils {
    private static final int HOTBAR_START =
            //#if MC >= 11700
            net.minecraft.screen.PlayerScreenHandler.HOTBAR_START
            //#else
            //$$ 36
            //#endif
    ;

    private InventoryUtils() {}

    public static DefaultedList<ItemStack> getMainStacks(PlayerInventory inventory) {
        //#if MC >= 12105
        return inventory.getMainStacks();
        //#else
        //$$ return inventory.main;
        //#endif
    }

    public static int getSelectedSlot(PlayerInventory inventory) {
        //#if MC >= 12105
        return inventory.getSelectedSlot();
        //#else
        //$$ return inventory.selectedSlot;
        //#endif
    }

    public static void setSelectedSlot(PlayerInventory inventory, int slot) {
        //#if MC >= 12105
        inventory.setSelectedSlot(slot);
        //#else
        //$$ inventory.selectedSlot = slot;
        //#endif
    }

    public static int findFirstItemInHotbar(PlayerInventory inventory, Item item) {
        DefaultedList<ItemStack> main = getMainStacks(inventory);
        for (int i = 0, size = PlayerInventory.getHotbarSize(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
    public static int findFirstItemInHotbar(PlayerInventory inventory, Predicate<ItemStack> predicate) {
        DefaultedList<ItemStack> main = getMainStacks(inventory);
        for (int i = 0, size = PlayerInventory.getHotbarSize(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }
    public static int findFirstItemInHotbar(PlayerInventory inventory, Item item, Predicate<ItemStack> predicate) {
        DefaultedList<ItemStack> main = getMainStacks(inventory);
        for (int i = 0, size = PlayerInventory.getHotbarSize(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == item && predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }
    public static int findFirstItem(PlayerInventory inventory, Item item) {
        DefaultedList<ItemStack> main = getMainStacks(inventory);
        for (int i = 0, size = main.size(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
    public static int findFirstItem(PlayerInventory inventory, Predicate<ItemStack> predicate) {
        DefaultedList<ItemStack> main = getMainStacks(inventory);
        for (int i = 0, size = main.size(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }
    public static int findFirstItem(PlayerInventory inventory, Item item, Predicate<ItemStack> predicate) {
        DefaultedList<ItemStack> main = getMainStacks(inventory);
        for (int i = 0, size = main.size(); i < size; ++i) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == item && predicate.test(stack)) {
                return i;
            }
        }
        return -1;
    }
    public static int findBestItemInHotbar(PlayerInventory inventory, Predicate<ItemStack> predicate, Comparator<ItemStack> comparator) {
        DefaultedList<ItemStack> main = getMainStacks(inventory);
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
        DefaultedList<ItemStack> main = getMainStacks(inventory);
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

    public static <T> T moveToOffHandDuring(ClientPlayerEntity player, int hotbarSlot, Supplier<T> supplier) {
        if (player.currentScreenHandler != player.playerScreenHandler)
            throw new IllegalStateException("player.currentScreenHandler != player.playerScreenHandler");
        if (hotbarSlot < 0 || hotbarSlot > 8)
            throw new IllegalArgumentException("hotbarSlot is not in 0~8 range");
        ClientPlayerInteractionManager interactionManager = Objects.requireNonNull(MinecraftClient.getInstance().interactionManager);
        interactionManager.clickSlot(0, hotbarSlot + HOTBAR_START, 40, SlotActionType.SWAP, player);
        T result = supplier.get();
        interactionManager.clickSlot(0, hotbarSlot + HOTBAR_START, 40, SlotActionType.SWAP, player);
        return result;
    }

    public static <T> T useEmptyMainHandIfSneakingDuring(ClientPlayerEntity player, PlayerInventory inventory, Supplier<T> supplier) {
        if (player.currentScreenHandler != player.playerScreenHandler)
            throw new IllegalStateException("player.currentScreenHandler != player.playerScreenHandler");
        if (!inventory.getSelectedStack().isEmpty() && player.isSneaking()) {
            int oldIndex = getSelectedSlot(inventory);
            int newIndex = findFirstItemInHotbar(inventory, ItemStack::isEmpty);
            if (newIndex == -1)
                throw new IllegalStateException("full hotbar");
            ClientPlayerInteractionManager interactionManager = Objects.requireNonNull(MinecraftClient.getInstance().interactionManager);
            interactionManager.clickSlot(0, oldIndex + HOTBAR_START, newIndex, SlotActionType.SWAP, player);
            T result = supplier.get();
            interactionManager.clickSlot(0, oldIndex + HOTBAR_START, newIndex, SlotActionType.SWAP, player);
            return result;
        }
        return supplier.get();
    }

    /**
     * 获取当前物品每tick能够破坏指定方块进度的百分比.
     *
     * @param blockState 要破坏的方块状态
     * @param itemStack  使用工具/物品破坏方块
     * @return 当前物品每tick破坏该方块的百分比
     */
    public static float calcBlockBreakingDelta(ClientPlayerEntity player, BlockState blockState, ItemStack itemStack) {
        // 硬编码的
        //#if MC <= 12104
        //$$ if (itemStack.getItem() instanceof net.minecraft.item.SwordItem &&
        //$$         (
        //$$                 blockState.getBlock() instanceof net.minecraft.block.BambooBlock
        //$$                         || blockState.getBlock() instanceof net.minecraft.block.BambooShootBlock
        //$$         )
        //$$ ) {
        //$$     return 1F;
        //$$ }
        //#endif
        float hardness = BlockUtils.getHardness(blockState);
        if (hardness < 0)
            return 0;
        final int i;
        if (!blockState.isToolRequired() || itemStack.isSuitableFor(blockState)) {
            i = 30;
        } else {
            i = 100;
        }
        float f = itemStack.getMiningSpeedMultiplier(blockState);  // 当前物品的破坏系数速度
        // 根据工具的"效率"附魔增加破坏速度
        if (f > 1.0F) {
            // 获取itemStack的附魔集合
            int toolLevel = -1;
            //#if MC >= 12100
            for (net.minecraft.registry.entry.RegistryEntry<Enchantment> enchantment : itemStack.getEnchantments().getEnchantments()) {
                //noinspection OptionalGetWithoutIsPresent
                if (enchantment.getKey().get() == Enchantments.EFFICIENCY) {
                    toolLevel = EnchantmentHelper.getLevel(enchantment, itemStack);
                    break;
                }
            }
            //#else
            //$$ toolLevel = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemStack);
            //#endif
            if (toolLevel > 0 && !itemStack.isEmpty()) {
                f += (float) (toolLevel * toolLevel + 1);
            }
        }
        // 根据玩家"急迫"状态效果增加破坏速度
        if (StatusEffectUtil.hasHaste(player)) {
            f *= 1.0F + (float) (StatusEffectUtil.getHasteAmplifier(player) + 1) * 0.2F;
        }

        // 根据玩家"挖掘疲劳"状态效果减缓破坏速度
        if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float f2;
            switch (Objects.requireNonNull(player.getStatusEffect(StatusEffects.MINING_FATIGUE)).getAmplifier()) {
                case 0: {
                    f2 = 0.3F;
                    break;
                }
                case 1: {
                    f2 = 0.09F;
                    break;
                }
                case 2: {
                    f2 = 0.0027F;
                    break;
                }
                default: {
                    f2 = 8.1E-4F;
                }
            }
            f *= f2;
        }
        //#if MC >= 12006
        f *= (float) player.getAttributeValue(EntityAttributes.BLOCK_BREAK_SPEED);
        //#endif
        // 如果玩家在水中并且没有"水下速掘"附魔，则减缓破坏速度
        if (player.isSubmergedIn(FluidTags.WATER)) {
            //#if MC >= 12100
            EntityAttributeInstance submergedMiningSpeed = player.getAttributeInstance(EntityAttributes.SUBMERGED_MINING_SPEED);
            if (submergedMiningSpeed != null) {
                f *= (float) submergedMiningSpeed.getValue();
            }
            //#else
            //$$ if (!EnchantmentHelper.hasAquaAffinity(player))
            //$$     f /= 5.0F;
            //#endif
        }
        // 如果玩家不在地面上，则减缓破坏速度
        if (!player.isOnGround()) {
            f /= 5.0F;
        }
        return f / hardness / (float) i;
    }

    public static boolean isPickaxe(ItemStack stack) {
        //#if MC >= 12104
        return stack.isIn(net.minecraft.registry.tag.ItemTags.PICKAXES);
        //#else
        //$$ return stack.getItem() instanceof net.minecraft.item.PickaxeItem;
        //#endif
    }
}
