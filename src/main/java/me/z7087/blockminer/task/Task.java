package me.z7087.blockminer.task;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.mixin.minecraft.client.network.ClientPlayerInteractionManagerAccessor;
import me.z7087.blockminer.util.BlockUtils;
import me.z7087.blockminer.util.InventoryUtils;
import me.z7087.blockminer.util.RotationUtils;
import me.z7087.blockminer.util.data.BlockBreakStructureFull;
import me.z7087.blockminer.util.enums.PowerBlockType;
import me.z7087.blockminer.util.enums.TaskState;
import me.z7087.blockminer.util.finder.BlockFinder;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

// TODO 史山，等待重构
public class Task implements Comparable<Task> {
    public final BlockPos targetPos;
    private int pistonIndex, dependBlockIndex;
    private int redstoneTorchIndex, leverIndex;
    private int pickaxeIndex;
    private BlockBreakStructureFull structure;
    private int waitTicks = 0;
    private float blockBreakingDelta;
    private boolean isMining = false;
    public TaskState state = TaskState.Start;

    public Task(BlockPos targetPos) {
        this.targetPos = Objects.requireNonNull(targetPos);
    }

    private int getWaitTicksAfterDecrement() {
        return --waitTicks;
    }

    private void setWaitTicks(int ticks) {
        if (this.waitTicks > 0) {
            throw new IllegalStateException("still waiting");
        }
        this.waitTicks = ticks + BlockMinerMod.getInstance().getConfig().getPingSpikeThreshold();
    }

    public boolean tick() {
        final MinecraftClient client = MinecraftClient.getInstance();
        final ClientPlayerEntity player = client.player;
        final ClientWorld world = client.world;
        if (player == null || world == null)
            return false;
        final ClientPlayerInteractionManager interactionManager = Objects.requireNonNull(client.interactionManager);
        final RotationUtils rotationUtils = BlockMinerMod.getInstance().getRotationUtils();
        final PlayerInventory inventory;
        //#if MC >= 11700
        inventory = player.getInventory();
        //#else
        //$$ inventory = player.inventory;
        //#endif

        loop:
        while (true) {
            switch (state) {
                case Start: {
                    if (player.currentScreenHandler != player.playerScreenHandler) {
                        break loop;
                    }
                    if (!BlockUtils.playerCanTouchServerside(player, targetPos, 0)) {
                        break loop;
                    }
                    pistonIndex = InventoryUtils.findFirstItemInHotbar(inventory, Items.PISTON);
                    if (pistonIndex == -1 && BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode()) {
                        pistonIndex = InventoryUtils.findFirstItemInHotbar(inventory, Items.STICKY_PISTON);
                    }
                    if (pistonIndex == -1) {
                        break loop;
                    } else {
                        dependBlockIndex = InventoryUtils.findFirstItemInHotbar(inventory, (stack) -> BlockMinerMod.getInstance().getConfig().dependBlockWhitelistContains(stack.getItem()));
                        PowerBlockType powerBlockUsage = BlockMinerMod.getInstance().getConfig().getPowerBlockUsage();
                        pickaxeIndex = InventoryUtils.findBestItemInHotbar(inventory,
                                (stack ->
                                        InventoryUtils.isPickaxe(stack)
                                                && (stack.getMaxDamage() - stack.getDamage() >= 5)
                                ),
                                ((stack1, stack2) -> {
                                    BlockState pistonDefaultState = Blocks.PISTON.getDefaultState();
                                    return Float.compare(
                                            InventoryUtils.calcBlockBreakingDelta(player, pistonDefaultState, stack1),
                                            InventoryUtils.calcBlockBreakingDelta(player, pistonDefaultState, stack2)
                                    );
                                }));
                        boolean canInstantMinePiston = pickaxeIndex != -1 && InventoryUtils.calcBlockBreakingDelta(player, Blocks.PISTON.getDefaultState(), inventory.getStack(pickaxeIndex)) >= 0.7;
                        redstoneTorchIndex = powerBlockUsage.isRedstoneTorch() && canInstantMinePiston
                                ? InventoryUtils.findFirstItemInHotbar(inventory, Items.REDSTONE_TORCH)
                                : -1;
                        leverIndex = powerBlockUsage.isLever() && !canSwitchHandDenyUse(player, inventory)
                                ? InventoryUtils.findFirstItemInHotbar(inventory, Items.LEVER)
                                : -1;
                    }
                    PowerBlockType powerBlockUsage = PowerBlockType.of(redstoneTorchIndex != -1, leverIndex != -1);
                    if (powerBlockUsage == null)
                        break loop;
                    //ArrayList<Pair<BlockPos, Direction>> pistonList = new ArrayList<>();
                    //BlockFinder.findStablePistons(world, targetPos, pistonList);
                    //ArrayList<PistonPowerInfo> pistonPowerInfos = new ArrayList<>();
                    //BlockFinder.findPowerBlockForPiston(world, targetPos, powerBlockUsage, pistonList, pistonPowerInfos, dependBlockIndex != -1);
                    for (BlockBreakStructureFull structure : (Iterable<? extends BlockBreakStructureFull>) BlockMinerMod.getInstance().getConfig().getSearchMode().findPossibleStructures(world, targetPos, powerBlockUsage, dependBlockIndex != -1)::iterator) {
                        this.structure = structure;
                        if (BlockUtils.playerCanTouchServerside(player, structure.getPistonPos(), 1, false)
                                && BlockUtils.playerCanTouchServerside(player, structure.getPowerBlockPos(), 1, false)
                                && BlockUtils.playerCanTouchServerside(player, structure.getDependBlockPos(), 1, false)
                                && world.canPlace(Blocks.STONE.getDefaultState(), structure.getPistonPos(), ShapeContext.absent())
                                // 当依赖方块是目标方块时，无法创建无头活塞
                                && (!BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode() || !structure.getDependBlockPos().equals(targetPos))) {
                            // 朝上下的活塞的朝向可以立即到位，其他方向的不行
                            switch (structure.getPistonFace()) {
                                case UP:
                                case DOWN: {
                                    // 如果活塞朝上，面向下，否则面向上
                                    float pitch = structure.getPistonFace() == Direction.UP ? 90F : -90F;
                                    if (rotationUtils.canSetPitch(pitch)) {
                                        assertTrue(rotationUtils.trySetPitch(pitch));
                                        rotationUtils.updateLocation(player);
                                        state = TaskState.PlaceBlocksWithoutChecks;
                                        // 继续循环
                                        continue loop;
                                    }
                                    break loop;
                                }
                                default: {
                                    float pitch = 0;
                                    float yaw;
                                    // 假定玩家面向正南时 yaw = 0
                                    switch (structure.getPistonFace()) {
                                        case SOUTH: {
                                            // 朝北
                                            yaw = 180F;
                                            break;
                                        }
                                        case WEST: {
                                            // 朝东
                                            yaw = -90F;
                                            break;
                                        }
                                        case NORTH: {
                                            // 朝南
                                            yaw = 0F;
                                            break;
                                        }
                                        case EAST:
                                        default: {
                                            // 朝西
                                            yaw = 90F;
                                        }
                                    }
                                    if (rotationUtils.canSetRotation(yaw, pitch)) {
                                        assertTrue(rotationUtils.trySetRotation(yaw, pitch));
                                        rotationUtils.markKeepRotation();
                                        state = TaskState.WaitForPistonPlaceRotate;
                                        setWaitTicks(1);
                                    }
                                    break loop;
                                }
                            }
                        }
                    }
                    break loop;
                }
                case WaitForPistonPlaceRotate: {
                    if (getWaitTicksAfterDecrement() > 0) {
                        rotationUtils.markKeepRotation();
                        break loop;
                    }
                    state = TaskState.PlaceBlocksWithChecks;
                    // fall down
                }
                case PlaceBlocksWithChecks: {
                    final BlockState dependBlockState = world.getBlockState(structure.getDependBlockPos());
                    if (inventory.getStack(pistonIndex).getItem() != Items.PISTON
                            || (dependBlockIndex != -1 && !BlockMinerMod.getInstance().getConfig().dependBlockWhitelistContains(inventory.getStack(dependBlockIndex).getItem()))
                            || (redstoneTorchIndex != -1 && inventory.getStack(redstoneTorchIndex).getItem() != Items.REDSTONE_TORCH)
                            || (leverIndex != -1 && inventory.getStack(leverIndex).getItem() != Items.LEVER)
                            || !BlockUtils.isReplaceable(world.getBlockState(structure.getPistonPos()))
                            || !world.canPlace(Blocks.STONE.getDefaultState(), structure.getPistonPos(), ShapeContext.absent())
                            || !BlockUtils.isReplaceable(world.getBlockState(structure.getPowerBlockPos()))
                            || (
                            !BlockUtils.isReplaceable(world.getBlockState(structure.getDependBlockPos()))
                                    && !(
                                    dependBlockState.isSolidBlock(world, structure.getDependBlockPos())
                                            && dependBlockState.isSideSolidFullSquare(world, structure.getDependBlockPos(), structure.getPowerBlockFace())
                                            && !(dependBlockState.getBlock() instanceof PistonBlock)
                            )
                    )
                    ) {
                        // 检查失败，重新找
                        retry();
                        break loop;
                    }
                    if (!BlockUtils.playerCanTouchServerside(player, structure.getPistonPos(), 1, false)
                            || !BlockUtils.playerCanTouchServerside(player, structure.getPowerBlockPos(), 1, false)
                            || !BlockUtils.playerCanTouchServerside(player, structure.getDependBlockPos(), 1, false)) {
                        // 距离不够，重新找
                        retry();
                        break loop;
                    }
                    // fall down
                }
                case PlaceBlocksWithoutChecks: {
                    if (player.currentScreenHandler != player.playerScreenHandler) {
                        retry();
                        break loop;
                    }
                    if (redstoneTorchIndex == -1 || !structure.getPowerBlockType().isRedstoneTorch()) {
                        // 对于拉杆放置，检查是否无法使用拉杆
                        if (canSwitchHandDenyUse(player, inventory)) {
                            retry();
                            break loop;
                        }
                    }
                    {
                        // 凭空放置
                        ActionResult result = InventoryUtils.moveToOffHandDuring(player,
                                pistonIndex,
                                () -> rotationUtils.useServerSideRotationDuring(
                                        player,
                                        () -> BlockUtils.interactBlock(interactionManager,
                                                player,
                                                world,
                                                Hand.OFF_HAND,
                                                new BlockHitResult(
                                                        Vec3d.of(structure.getPistonPos()),
                                                        Direction.DOWN,
                                                        structure.getPistonPos(),
                                                        false
                                                )
                                        )
                                )
                        );
                        // 放不了，怎么回事呢？重来一遍
                        if (!result.isAccepted()) {
                            retry();
                            break loop;
                        }
                    }
                    if (BlockUtils.isReplaceable(world.getBlockState(structure.getDependBlockPos()))) {
                        if (dependBlockIndex == -1) {
                            // 本来那有个方块但消失了，手里又没有粘液块，回去重找
                            retry();
                            break loop;
                        }
                        // 凭空放置
                        ActionResult result = InventoryUtils.moveToOffHandDuring(player,
                                dependBlockIndex,
                                () -> BlockUtils.interactBlock(interactionManager,
                                        player,
                                        world,
                                        Hand.OFF_HAND,
                                        new BlockHitResult(
                                                Vec3d.of(structure.getDependBlockPos()),
                                                Direction.DOWN,
                                                structure.getDependBlockPos(),
                                                false
                                        )
                                )
                        );
                        // 这个必须得放得了，之前都检查过了
                        assertTrue(result.isAccepted());
                    }
                    // 优先红石火把
                    if (redstoneTorchIndex != -1 && structure.getPowerBlockType().isRedstoneTorch()) {
                        leverIndex = -1;
                        // 放在那个方块上
                        ActionResult result = InventoryUtils.moveToOffHandDuring(player,
                                redstoneTorchIndex,
                                () -> BlockUtils.interactBlock(interactionManager,
                                        player,
                                        world,
                                        Hand.OFF_HAND,
                                        new BlockHitResult(
                                                Vec3d.of(structure.getDependBlockPos()),
                                                structure.getPowerBlockFace(),
                                                structure.getDependBlockPos(),
                                                false
                                        )
                                )
                        );
                        assertTrue(result.isAccepted());
                    } else if (leverIndex != -1 && structure.getPowerBlockType().isLever()) {
                        redstoneTorchIndex = -1;
                        // 放在那个方块上
                        ActionResult result = InventoryUtils.moveToOffHandDuring(player,
                                leverIndex,
                                () -> BlockUtils.interactBlock(interactionManager,
                                        player,
                                        world,
                                        Hand.OFF_HAND,
                                        new BlockHitResult(
                                                Vec3d.of(structure.getDependBlockPos()),
                                                structure.getPowerBlockFace(),
                                                structure.getDependBlockPos(),
                                                false
                                        )
                                )
                        );
                        assertTrue(result.isAccepted());
                        // 拉拉杆
                        result = InventoryUtils.useEmptyMainHandIfSneakingDuring(player,
                                inventory,
                                () -> BlockUtils.interactBlock(interactionManager,
                                        player,
                                        world,
                                        Hand.MAIN_HAND,
                                        new BlockHitResult(
                                                Vec3d.of(structure.getPowerBlockPos()),
                                                structure.getPowerBlockFace(),
                                                structure.getPowerBlockPos(),
                                                false
                                        )
                                )
                        );
                        if (!result.isAccepted())
                            assertTrue(result.isAccepted());
                    } else {
                        shouldNotReachHere();
                    }
                    state = TaskState.SelectPickaxeAndReadyMine;
                    // fall down
                }
                case SelectPickaxeAndReadyMine: {
                    if (pickaxeIndex != -1) {
                        InventoryUtils.setSelectedSlot(inventory, pickaxeIndex);
                        ((ClientPlayerInteractionManagerAccessor) interactionManager).invokeSyncSelectedSlot();
                    }
                    Direction pistonToTargetBlockFace = null;
                    for (Direction face : BlockFinder.DIRECTIONS) {
                        if (structure.getPistonPos().offset(face).equals(targetPos)) {
                            pistonToTargetBlockFace = face;
                            break;
                        }
                    }
                    assertTrue(pistonToTargetBlockFace != null);
                    switch (pistonToTargetBlockFace) {
                        case UP:
                        case DOWN: {
                            if (rotationUtils.trySetPitch(pistonToTargetBlockFace == Direction.UP ? 90F : -90F)) {
                                break;
                            }
                            break loop;
                        }
                        default: {
                            float pitch = 0;
                            float yaw;
                            // 假定玩家面向正南时 yaw = 0
                            switch (pistonToTargetBlockFace) {
                                case SOUTH: {
                                    // 朝北
                                    yaw = 180F;
                                    break;
                                }
                                case WEST: {
                                    // 朝东
                                    yaw = -90F;
                                    break;
                                }
                                case NORTH: {
                                    // 朝南
                                    yaw = 0F;
                                    break;
                                }
                                case EAST:
                                default: {
                                    // 朝西
                                    yaw = 90F;
                                }
                            }
                            if (rotationUtils.trySetRotation(yaw, pitch)) {
                                break;
                            }
                            break loop;
                        }
                    }
                    blockBreakingDelta = InventoryUtils.calcBlockBreakingDelta(player, Blocks.PISTON.getDefaultState(), inventory.getSelectedStack());
                    if (blockBreakingDelta < 1) {
                        if (blockBreakingDelta < 0.7 && redstoneTorchIndex != -1) {
                            // 挖得太慢了，破不了，回去重试
                            retry();
                            break loop;
                        }
                        if (BlockUtils.playerCanTouchServerside(player, structure.getPistonPos(), 1, true) && !BlockMinerMod.getInstance().getBlockBreakUtils().isModBreakingBlock()) {
                            BlockMinerMod.getInstance().getBlockBreakUtils().setBreaking(true);
                            isMining = true;
                            interactionManager.cancelBlockBreaking();
                            interactionManager.attackBlock(structure.getPistonPos(), Direction.DOWN);
                        } else {
                            // 有别的任务在占用挖掘或者挖不到方块，一会再检查一遍
                            break loop;
                        }
                    }
                    rotationUtils.markKeepRotation();
                    rotationUtils.updateLocation(player);
                    // 等待活塞进入正在展开状态，不需要完全展开
                    state = TaskState.WaitForPistonExtend;
                    setWaitTicks(blockBreakingDelta >= 0.7 ? 1 : (int) Math.ceil(0.7 / blockBreakingDelta) + 1);
                    break loop;
                }
                case WaitForPistonExtend: {
                    if (getWaitTicksAfterDecrement() > 0) {
                        rotationUtils.markKeepRotation();
                        if (pickaxeIndex != -1) {
                            InventoryUtils.setSelectedSlot(inventory, pickaxeIndex);
                            ((ClientPlayerInteractionManagerAccessor) interactionManager).invokeSyncSelectedSlot();
                        }
                        break loop;
                    }
                    state = TaskState.Execute;
                    // fall down
                }
                case Execute: {
                    if (pickaxeIndex != -1) {
                        InventoryUtils.setSelectedSlot(inventory, pickaxeIndex);
                        ((ClientPlayerInteractionManagerAccessor) interactionManager).invokeSyncSelectedSlot();
                    }
                    if (!BlockUtils.playerCanTouchServerside(player, structure.getPistonPos(), 1, true)) {
                        rotationUtils.markKeepRotation();
                        // 太远挖不到活塞，延后
                        break loop;
                    }
                    if (!BlockUtils.playerCanTouchServerside(player, structure.getPowerBlockPos(), 1, true)) {
                        rotationUtils.markKeepRotation();
                        // 太远碰不到能源方块，延后
                        break loop;
                    }
                    if (redstoneTorchIndex != -1 && BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode() && !BlockUtils.playerCanTouchServerside(player, structure.getDependBlockPos(), 1, false)) {
                        rotationUtils.markKeepRotation();
                        // 如果是无头活塞模式，且此task使用红石火把，且太远碰不到红石火把依附的方块，延后
                        break loop;
                    }
                    if (leverIndex != -1 && canSwitchHandDenyUse(player, inventory)) {
                        // 如果是拉杆模式且无法使用拉杆，延后
                        rotationUtils.markKeepRotation();
                        break loop;
                    }
                    if (redstoneTorchIndex != -1) {
                        // 打红石火把
                        interactionManager.attackBlock(structure.getPowerBlockPos(), Direction.DOWN);
                    } else {
                        // 拉拉杆
                        ActionResult result = InventoryUtils.useEmptyMainHandIfSneakingDuring(player,
                                inventory,
                                () -> BlockUtils.interactBlock(interactionManager,
                                        player,
                                        world,
                                        Hand.MAIN_HAND,
                                        new BlockHitResult(
                                                Vec3d.of(structure.getPowerBlockPos()),
                                                structure.getPowerBlockFace(),
                                                structure.getPowerBlockPos(),
                                                false
                                        )
                                )
                        );
                        if (!result.isAccepted()) {
                            // 为什么失败了？
                            retry();
                            break loop;
                        }
                    }
                    if (blockBreakingDelta >= 1) {
                        float blockBreakingDeltaNow = InventoryUtils.calcBlockBreakingDelta(player, Blocks.PISTON.getDefaultState(), player.getMainHandStack());
                        if (blockBreakingDeltaNow < 1) {
                            // 之前能秒破活塞但现在不能了，回去
                            retry();
                            break loop;
                        }
                        interactionManager.attackBlock(structure.getPistonPos(), Direction.DOWN);
                        if (!world.getBlockState(structure.getPistonPos()).isAir())
                            world.setBlockState(structure.getPistonPos(), Blocks.AIR.getDefaultState());
                    } else {
                        if (BlockUtils.getHardness(world.getBlockState(structure.getPistonPos())) < 0) {
                            // 怎么回事？byd活塞变基岩了？
                            retry();
                            break loop;
                        }
                        ClientPlayerInteractionManagerAccessor interactionManagerAccessor = (ClientPlayerInteractionManagerAccessor) interactionManager;
                        while (interactionManager.isBreakingBlock() && interactionManagerAccessor.invokeIsCurrentlyBreaking(structure.getPistonPos()))
                            interactionManager.updateBlockBreakingProgress(structure.getPistonPos(), Direction.DOWN);
                        BlockMinerMod.getInstance().getBlockBreakUtils().setBreaking(false);
                        isMining = false;
                    }
                    if (BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode()) {
                        // 无头活塞模式下的重新放置阶段，先重新激活信号源或先放置活塞都是可以的
                        // 但先放置活塞的话目标方块可能被意外破掉，所以这里选择先重新激活信号源
                        ActionResult result;
                        if (redstoneTorchIndex != -1) {
                            // 重新放置红石火把
                            result = InventoryUtils.moveToOffHandDuring(player,
                                    redstoneTorchIndex,
                                    () -> BlockUtils.interactBlock(interactionManager,
                                            player,
                                            world,
                                            Hand.OFF_HAND,
                                            new BlockHitResult(
                                                    Vec3d.of(structure.getDependBlockPos()),
                                                    structure.getPowerBlockFace(),
                                                    structure.getDependBlockPos(),
                                                    false
                                            )
                                    )
                            );
                        } else {
                            // 拉拉杆
                            result = InventoryUtils.useEmptyMainHandIfSneakingDuring(player,
                                    inventory,
                                    () -> BlockUtils.interactBlock(interactionManager,
                                            player,
                                            world,
                                            Hand.MAIN_HAND,
                                            new BlockHitResult(
                                                    Vec3d.of(structure.getPowerBlockPos()),
                                                    structure.getPowerBlockFace(),
                                                    structure.getPowerBlockPos(),
                                                    false
                                            )
                                    )
                            );
                        }
                        if (!result.isAccepted()) {
                            // 重新激活信号源失败了，回去重试
                            retry();
                            break loop;
                        }
                    }
                    // 重新凭空放置活塞
                    ActionResult result = InventoryUtils.moveToOffHandDuring(
                            player,
                            pistonIndex,
                            () -> rotationUtils.useServerSideRotationDuring(
                                    player,
                                    () -> BlockUtils.interactBlock(interactionManager,
                                            player,
                                            world,
                                            Hand.OFF_HAND,
                                            new BlockHitResult(
                                                    Vec3d.of(structure.getPistonPos()),
                                                    Direction.DOWN,
                                                    structure.getPistonPos(),
                                                    false
                                            )
                                    )
                            )
                    );
                    if (!result.isAccepted()) {
                        // ?????
                        retry();
                        break loop;
                    }
                    state = TaskState.Finished;
                    break loop;
                }
                case Finished:
                default: {
                    break loop;
                }
            }
        }
        return false;
    }

    private void retry() {
        state = TaskState.Start;
        if (isMining) {
            BlockMinerMod.getInstance().getBlockBreakUtils().setBreaking(false);
            Objects.requireNonNull(MinecraftClient.getInstance().interactionManager).cancelBlockBreaking();
            isMining = false;
        }
    }

    private static boolean canDenyUse(ClientPlayerEntity player) {
        return player.isSneaking() && (!player.getMainHandStack().isEmpty() || !player.getOffHandStack().isEmpty());
    }

    private static boolean canSwitchHandDenyUse(ClientPlayerEntity player, PlayerInventory inventory) {
        if (player.isSneaking()) {
            if (!player.getOffHandStack().isEmpty())
                return true;
            return InventoryUtils.findFirstItemInHotbar(inventory, ItemStack::isEmpty) == -1;
        }
        return false;
    }

    private static void assertTrue(boolean result) {
        if (!result)
            throw new AssertionError();
    }

    private static void shouldNotReachHere() {
        throw new AssertionError();
    }

    public static Task of(BlockPos targetPos) {
        return new Task(targetPos);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Task && targetPos.equals(((Task) o).targetPos);
    }

    @Override
    public int hashCode() {
        return targetPos.hashCode();
    }

    @Override
    public int compareTo(Task o) {
        return targetPos.compareTo(o.targetPos);
    }
}
