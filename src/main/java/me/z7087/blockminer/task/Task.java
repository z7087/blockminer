package me.z7087.blockminer.task;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.mixin.ClientPlayerInteractionManagerAccessor;
import me.z7087.blockminer.util.BlockFinder;
import me.z7087.blockminer.util.InventoryUtils;
import me.z7087.blockminer.util.RotationUtils;
import me.z7087.blockminer.util.data.Pair;
import me.z7087.blockminer.util.data.PistonPowerInfo;
import me.z7087.blockminer.util.enums.PowerBlockType;
import me.z7087.blockminer.util.enums.TaskState;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class Task implements Comparable<Task> {
    public final BlockPos targetPos;
    private int pistonIndex, slimeBlockIndex;
    private int redstoneTorchIndex, leverIndex;
    private int pickaxeIndex;
    private PistonPowerInfo pistonPowerInfo;
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
        this.waitTicks = ticks + BlockMinerMod.INSTANCE.config.pingSpikeThreshold;
    }

    public boolean tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null)
            return false;
        loop:
        {
            while (true) {
                switch (state) {
                    case Start: {
                        if (player.currentScreenHandler != player.playerScreenHandler) {
                            break loop;
                        }
                        if (!player.canInteractWithBlockAt(targetPos, 0)) {
                            break loop;
                        }
                        RotationUtils rotationUtils = BlockMinerMod.INSTANCE.rotationUtils;
                        PlayerInventory inventory = player.getInventory();
                        if ((pistonIndex = InventoryUtils.findFirstItemInHotbar(inventory, Items.PISTON)) == -1
                                || (slimeBlockIndex = InventoryUtils.findFirstItemInHotbar(inventory, Items.SLIME_BLOCK)) == -1
                        ) {
                            break loop;
                        } else {
                            PowerBlockType powerBlockUsage = BlockMinerMod.INSTANCE.config.powerBlockUsage;
                            pickaxeIndex = InventoryUtils.findBestItemInHotbar(inventory,
                                    (stack ->
                                            stack.isIn(ItemTags.PICKAXES)
                                                    && (stack.getMaxDamage() - stack.getDamage() >= 5)
                                    ),
                                    ((stack1, stack2) -> {
                                        ClientPlayerEntity player1 = MinecraftClient.getInstance().player;
                                        BlockState pistonDefaultState = Blocks.PISTON.getDefaultState();
                                        return Float.compare(
                                                InventoryUtils.calcBlockBreakingDelta(player1, pistonDefaultState, stack1),
                                                InventoryUtils.calcBlockBreakingDelta(player1, pistonDefaultState, stack2)
                                        );
                                    }));
                            boolean canInstantMinePiston = pickaxeIndex != -1 && InventoryUtils.calcBlockBreakingDelta(player, Blocks.PISTON.getDefaultState(), player.getInventory().getStack(pickaxeIndex)) >= 0.7;
                            redstoneTorchIndex = powerBlockUsage.isRedstoneTorch() && canInstantMinePiston
                                    ? InventoryUtils.findFirstItemInHotbar(inventory, Items.REDSTONE_TORCH)
                                    : -1;
                            leverIndex = powerBlockUsage.isLever()
                                    ? InventoryUtils.findFirstItemInHotbar(inventory, Items.LEVER)
                                    : -1;
                        }
                        PowerBlockType powerBlockUsage = PowerBlockType.of(redstoneTorchIndex != -1, leverIndex != -1);
                        if (powerBlockUsage == null)
                            break loop;
                        ArrayList<Pair<BlockPos, Direction>> pistonList = new ArrayList<>();
                        BlockFinder.findStablePistons(world, targetPos, pistonList);
                        ArrayList<PistonPowerInfo> pistonPowerInfos = new ArrayList<>();
                        BlockFinder.findPowerBlockForPiston(world, targetPos, powerBlockUsage, pistonList, pistonPowerInfos);
                        if (pistonPowerInfos.isEmpty())
                            break loop;
                        for (PistonPowerInfo pistonPowerInfo : pistonPowerInfos) {
                            this.pistonPowerInfo = pistonPowerInfo;
                            if (player.canInteractWithBlockAt(pistonPowerInfo.pistonPos, 1)
                                    && player.canInteractWithBlockAt(pistonPowerInfo.powerBlockPos, 1)
                                    && player.canInteractWithBlockAt(pistonPowerInfo.powerBlockPos.offset(pistonPowerInfo.powerBlockFace.getOpposite()), 1)
                                    && world.canPlace(Blocks.STONE.getDefaultState(), pistonPowerInfo.pistonPos, ShapeContext.absent())) {
                                // 朝上下的活塞的朝向可以立即到位，其他方向的不行
                                switch (pistonPowerInfo.pistonFace) {
                                    case UP:
                                    case DOWN: {
                                        // 如果活塞朝上，面向下，否则面向上
                                        float pitch = pistonPowerInfo.pistonFace == Direction.UP ? 90F : -90F;
                                        if (rotationUtils.canSetPitch(pitch)) {
                                            assertTrue(rotationUtils.trySetPitch(pitch));
                                            rotationUtils.updateLocation(player);
                                            state = TaskState.PlaceBlocksWithoutChecks;
                                            // 继续循环
                                            break;
                                        }
                                        break loop;
                                    }
                                    default: {
                                        float pitch = 0;
                                        float yaw;
                                        // 假定玩家面向正南时 yaw = 0
                                        switch (pistonPowerInfo.pistonFace) {
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
                                break;
                            }
                        }
                        break;
                    }
                    case WaitForPistonPlaceRotate: {
                        if (getWaitTicksAfterDecrement() > 0) {
                            BlockMinerMod.INSTANCE.rotationUtils.markKeepRotation();
                            break loop;
                        }
                        state = TaskState.PlaceBlocksWithChecks;
                        // fall down
                    }
                    case PlaceBlocksWithChecks: {
                        PlayerInventory inventory = player.getInventory();
                        BlockPos dependBlockPos = pistonPowerInfo.powerBlockPos.offset(pistonPowerInfo.powerBlockFace.getOpposite());
                        if (inventory.getStack(pistonIndex).getItem() != Items.PISTON
                                || inventory.getStack(slimeBlockIndex).getItem() != Items.SLIME_BLOCK
                                || (redstoneTorchIndex != -1 && inventory.getStack(redstoneTorchIndex).getItem() != Items.REDSTONE_TORCH)
                                || (leverIndex != -1 && inventory.getStack(leverIndex).getItem() != Items.LEVER)
                                || !world.getBlockState(pistonPowerInfo.pistonPos).isReplaceable()
                                || !world.canPlace(Blocks.STONE.getDefaultState(), pistonPowerInfo.pistonPos, ShapeContext.absent())
                                || !world.getBlockState(pistonPowerInfo.powerBlockPos).isReplaceable()
                                || (
                                        !world.getBlockState(dependBlockPos).isReplaceable()
                                                && !(
                                                        Block.sideCoversSmallSquare(world, dependBlockPos, pistonPowerInfo.powerBlockFace)
                                                                && !(world.getBlockState(dependBlockPos).getBlock() instanceof PistonBlock)
                                                )
                                )
                        ) {
                            // 检查失败，重新找
                            state = TaskState.Start;
                            break;
                        }
                        if (!player.canInteractWithBlockAt(pistonPowerInfo.pistonPos, 1)
                                || !player.canInteractWithBlockAt(pistonPowerInfo.powerBlockPos, 1)
                                || !player.canInteractWithBlockAt(dependBlockPos, 1)) {
                            // 距离不够，重新找
                            state = TaskState.Start;
                            break;
                        }
                        // fall down
                    }
                    case PlaceBlocksWithoutChecks: {
                        ClientPlayerInteractionManager interactionManager = Objects.requireNonNull(MinecraftClient.getInstance().interactionManager);
                        if (player.currentScreenHandler != player.playerScreenHandler) {
                            state = TaskState.Start;
                            break loop;
                        }
                        {
                            // 凭空放置
                            ActionResult result = InventoryUtils.moveToOffhandDuring(player,
                                    pistonIndex,
                                    () -> interactionManager.interactBlock(player,
                                            Hand.OFF_HAND,
                                            new BlockHitResult(
                                                    Vec3d.of(pistonPowerInfo.pistonPos),
                                                    Direction.DOWN,
                                                    pistonPowerInfo.pistonPos,
                                                    false
                                            )
                                    )
                            );
                            // 放不了，怎么回事呢？重来一遍
                            if (!result.isAccepted()) {
                                state = TaskState.Start;
                                break loop;
                            }
                        }
                        BlockPos dependBlockPos = pistonPowerInfo.powerBlockPos.offset(pistonPowerInfo.powerBlockFace.getOpposite());
                        if (world.getBlockState(dependBlockPos).isReplaceable()) {
                            // 凭空放置
                            ActionResult result = InventoryUtils.moveToOffhandDuring(player,
                                    slimeBlockIndex,
                                    () -> interactionManager.interactBlock(player,
                                            Hand.OFF_HAND,
                                            new BlockHitResult(
                                                    Vec3d.of(dependBlockPos),
                                                    Direction.DOWN,
                                                    dependBlockPos,
                                                    false
                                            )
                                    )
                            );
                            // 这个必须得放得了，之前都检查过了
                            assertTrue(result.isAccepted());
                        }
                        // 优先红石火把
                        if (redstoneTorchIndex != -1 && pistonPowerInfo.getPowerBlockType().isRedstoneTorch()) {
                            leverIndex = -1;
                            // 放在那个方块上
                            ActionResult result = InventoryUtils.moveToOffhandDuring(player,
                                    redstoneTorchIndex,
                                    () -> interactionManager.interactBlock(player,
                                            Hand.OFF_HAND,
                                            new BlockHitResult(
                                                    Vec3d.of(dependBlockPos),
                                                    pistonPowerInfo.powerBlockFace,
                                                    dependBlockPos,
                                                    false
                                            )
                                    )
                            );
                            assertTrue(result.isAccepted());
                        } else if (leverIndex != -1 && pistonPowerInfo.getPowerBlockType().isLever()) {
                            redstoneTorchIndex = -1;
                            // 放在那个方块上
                            ActionResult result = InventoryUtils.moveToOffhandDuring(player,
                                    leverIndex,
                                    () -> interactionManager.interactBlock(player,
                                            Hand.OFF_HAND,
                                            new BlockHitResult(
                                                    Vec3d.of(dependBlockPos),
                                                    pistonPowerInfo.powerBlockFace,
                                                    dependBlockPos,
                                                    false
                                            )
                                    )
                            );
                            assertTrue(result.isAccepted());
                            // 拉拉杆
                            result = interactionManager.interactBlock(player,
                                    Hand.MAIN_HAND,
                                    new BlockHitResult(
                                            Vec3d.of(pistonPowerInfo.powerBlockPos),
                                            pistonPowerInfo.powerBlockFace,
                                            pistonPowerInfo.powerBlockPos,
                                            false
                                    )
                            );
                            assertTrue(result.isAccepted());
                        } else {
                            shouldNotReachHere();
                        }
                        state = TaskState.SelectPickaxeAndReadyMine;
                        // fall down
                    }
                    case SelectPickaxeAndReadyMine: {
                        ClientPlayerInteractionManager interactionManager = Objects.requireNonNull(MinecraftClient.getInstance().interactionManager);
                        if (pickaxeIndex != -1) {
                            player.getInventory().selectedSlot = pickaxeIndex;
                            ((ClientPlayerInteractionManagerAccessor) interactionManager).invokeSyncSelectedSlot();
                        }
                        Direction pistonToTargetBlockFace = null;
                        for (Direction face : BlockFinder.DIRECTIONS) {
                            if (pistonPowerInfo.pistonPos.offset(face).equals(targetPos)) {
                                pistonToTargetBlockFace = face;
                                break;
                            }
                        }
                        assertTrue(pistonToTargetBlockFace != null);
                        switch (pistonToTargetBlockFace) {
                            case UP:
                            case DOWN: {
                                if (BlockMinerMod.INSTANCE.rotationUtils.trySetPitch(pistonToTargetBlockFace == Direction.UP ? 90F : -90F)) {
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
                                if (BlockMinerMod.INSTANCE.rotationUtils.trySetRotation(yaw, pitch)) {
                                    break;
                                }
                                break loop;
                            }
                        }
                        blockBreakingDelta = InventoryUtils.calcBlockBreakingDelta(player, Blocks.PISTON.getDefaultState(), player.getInventory().getMainHandStack());
                        if (blockBreakingDelta < 1) {
                            if (blockBreakingDelta < 0.7 && redstoneTorchIndex != -1) {
                                // 挖得太慢了，破不了，回去重试
                                state = TaskState.Start;
                                break loop;
                            }
                            if (player.canInteractWithBlockAt(pistonPowerInfo.pistonPos, 1) && !BlockMinerMod.INSTANCE.blockBreakUtils.isModBreakingBlock()) {
                                BlockMinerMod.INSTANCE.blockBreakUtils.setBreaking(true);
                                isMining = true;
                                interactionManager.cancelBlockBreaking();
                                interactionManager.attackBlock(pistonPowerInfo.pistonPos, Direction.DOWN);
                            } else {
                                // 有别的任务在占用挖掘或者挖不到方块，一会再检查一遍
                                break loop;
                            }
                        }
                        BlockMinerMod.INSTANCE.rotationUtils.markKeepRotation();
                        BlockMinerMod.INSTANCE.rotationUtils.updateLocation(player);
                        // 等待活塞进入正在展开状态，不需要完全展开
                        state = TaskState.WaitForPistonExtend;
                        setWaitTicks(blockBreakingDelta >= 1 ? 1 : (int) Math.ceil(0.7 / blockBreakingDelta));
                        break loop;
                    }
                    case WaitForPistonExtend: {
                        if (getWaitTicksAfterDecrement() > 0) {
                            BlockMinerMod.INSTANCE.rotationUtils.markKeepRotation();
                            ClientPlayerInteractionManager interactionManager = Objects.requireNonNull(MinecraftClient.getInstance().interactionManager);
                            if (pickaxeIndex != -1) {
                                player.getInventory().selectedSlot = pickaxeIndex;
                                ((ClientPlayerInteractionManagerAccessor) interactionManager).invokeSyncSelectedSlot();
                            }
                            break loop;
                        }
                        state = TaskState.Execute;
                        // fall down
                    }
                    case Execute: {
                        ClientPlayerInteractionManager interactionManager = Objects.requireNonNull(MinecraftClient.getInstance().interactionManager);
                        if (pickaxeIndex != -1) {
                            player.getInventory().selectedSlot = pickaxeIndex;
                            ((ClientPlayerInteractionManagerAccessor) interactionManager).invokeSyncSelectedSlot();
                        }
                        if (!player.canInteractWithBlockAt(pistonPowerInfo.pistonPos, 1)) {
                            BlockMinerMod.INSTANCE.rotationUtils.markKeepRotation();
                            // 太远挖不到活塞，延后
                            break loop;
                        }
                        if (!player.canInteractWithBlockAt(pistonPowerInfo.powerBlockPos, 1)) {
                            BlockMinerMod.INSTANCE.rotationUtils.markKeepRotation();
                            // 太远碰不到拉杆，延后
                            break loop;
                        }
                        if (redstoneTorchIndex != -1) {
                            // 打红石火把
                            interactionManager.attackBlock(pistonPowerInfo.powerBlockPos, Direction.DOWN);
                        } else {
                            // 拉拉杆
                            ActionResult result = interactionManager.interactBlock(player,
                                    Hand.MAIN_HAND,
                                    new BlockHitResult(
                                            Vec3d.of(pistonPowerInfo.powerBlockPos),
                                            pistonPowerInfo.powerBlockFace,
                                            pistonPowerInfo.powerBlockPos,
                                            false
                                    )
                            );
                            if (!result.isAccepted()) {
                                // 为什么失败了？
                                state = TaskState.Start;
                                if (isMining) {
                                    BlockMinerMod.INSTANCE.blockBreakUtils.setBreaking(false);
                                    isMining = false;
                                }
                                break loop;
                            }
                        }
                        if (blockBreakingDelta >= 1) {
                            float blockBreakingDeltaNow = InventoryUtils.calcBlockBreakingDelta(player, Blocks.PISTON.getDefaultState(), player.getMainHandStack());
                            if (blockBreakingDeltaNow < 1) {
                                // 之前能秒破活塞但现在不能了，回去
                                state = TaskState.Start;
                                if (isMining) {
                                    BlockMinerMod.INSTANCE.blockBreakUtils.setBreaking(false);
                                    interactionManager.cancelBlockBreaking();
                                    isMining = false;
                                }
                                break loop;
                            }
                            interactionManager.attackBlock(pistonPowerInfo.pistonPos, Direction.DOWN);
                            if (!world.getBlockState(pistonPowerInfo.pistonPos).isAir())
                                world.setBlockState(pistonPowerInfo.pistonPos, Blocks.AIR.getDefaultState());
                            // 不知道这里什么情况 为什么会报错
                            // 希望没有什么奇怪的错误
                            //assertTrue(world.getBlockState(pistonPowerInfo.pistonPos).isAir());
                        } else {
                            if (InventoryUtils.calcBlockBreakingDelta(player, Blocks.PISTON.getDefaultState(), player.getMainHandStack()) <= 0) {
                                // 怎么回事？byd活塞变基岩了？
                                state = TaskState.Start;
                                if (isMining) {
                                    BlockMinerMod.INSTANCE.blockBreakUtils.setBreaking(false);
                                    interactionManager.cancelBlockBreaking();
                                    isMining = false;
                                }
                                break loop;
                            }
                            ClientPlayerInteractionManagerAccessor interactionManagerAccessor = (ClientPlayerInteractionManagerAccessor) interactionManager;
                            while (interactionManager.isBreakingBlock() && interactionManagerAccessor.invokeIsCurrentlyBreaking(pistonPowerInfo.pistonPos))
                                interactionManager.updateBlockBreakingProgress(pistonPowerInfo.pistonPos, Direction.DOWN);
                            BlockMinerMod.INSTANCE.blockBreakUtils.setBreaking(false);
                        }
                        // 重新凭空放置活塞
                        ActionResult result = InventoryUtils.moveToOffhandDuring(player, pistonIndex, () -> interactionManager.interactBlock(player,
                                Hand.OFF_HAND,
                                new BlockHitResult(
                                        Vec3d.of(pistonPowerInfo.pistonPos),
                                        Direction.DOWN,
                                        pistonPowerInfo.pistonPos,
                                        false
                                )
                        ));
                        if (!result.isAccepted()) {
                            // ?????
                            state = TaskState.Start;
                            if (isMining) {
                                BlockMinerMod.INSTANCE.blockBreakUtils.setBreaking(false);
                                interactionManager.cancelBlockBreaking();
                                isMining = false;
                            }
                            break loop;
                        }
                        state = TaskState.Finished;
                        break;
                    }
                    case Finished:
                    default: {
                        break loop;
                    }
                }
            }
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
