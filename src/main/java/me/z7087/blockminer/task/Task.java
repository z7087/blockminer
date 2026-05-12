package me.z7087.blockminer.task;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.api.enums.EasyPlaceProtocol;
import me.z7087.blockminer.mixin.minecraft.client.network.ClientPlayerEntityAccessor;
import me.z7087.blockminer.mixin.minecraft.client.network.ClientPlayerInteractionManagerAccessor;
import me.z7087.blockminer.util.*;
import me.z7087.blockminer.util.data.BlockBreakStructureFull;
import me.z7087.blockminer.api.enums.PowerBlockType;
import me.z7087.blockminer.api.enums.TaskState;
import me.z7087.blockminer.util.finder.BlockFinder;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// TODO 史山，等待重构
public class Task implements Comparable<Task> {
    public final BlockPos targetPos;
    private int pistonIndex, dependBlockIndex;
    private int redstoneTorchIndex, leverIndex;
    private int pickaxeIndex;
    private BlockBreakStructureFull structure;
    private int waitTicks = 0;
    private int uncertainManagerKeepTicks = 0;
    private float blockBreakingDelta;
    private boolean isMining = false;
    public TaskState state = TaskState.Start;

    public Task(BlockPos targetPos) {
        this.targetPos = Objects.requireNonNull(targetPos);
    }

    private int getWaitTicksAfterDecrement() {
        return --waitTicks;
    }

    private int getWaitTicksAfterDecrementIfPositive() {
        if (waitTicks > 0)
            return --waitTicks;
        return 0;
    }

    private void stopWaiting() {
        if (this.waitTicks == 0) {
            throw new IllegalStateException("not waiting");
        }
        waitTicks = 0;
    }

    private void setWaitTicks(int ticks) {
        if (this.waitTicks > 0) {
            throw new IllegalStateException("still waiting");
        }
        this.waitTicks = ticks + BlockMinerMod.getInstance().getConfig().getPingSpikeThreshold();
    }

    private void setUncertainManagerKeepTicks(int ticks) {
        this.uncertainManagerKeepTicks = ticks + BlockMinerMod.getInstance().getConfig().getPingSpikeThreshold();
    }

    public boolean tick() {
        //noinspection resource
        if (BlockMinerMod.getInstance().ticklyUpdateConstants().player() == null
                || BlockMinerMod.getInstance().ticklyUpdateConstants().world() == null) {
            return false;
        }

        loop:
        while (true) {
            switch (state) {
                case Start: {
                    if (start()) {
                        continue;
                    }
                    break loop;
                }
                case WaitForPistonPlaceRotate: {
                    if (waitForPistonPlaceRotate()) {
                        continue;
                    }
                    break loop;
                }
                case PlaceBlocksWithChecks: {
                    // 这里是为了不改变state的时候fall down
                    if (!placeBlocksWithChecks()) {
                        break loop;
                    }
                    // fall down
                }
                case PlaceBlocksWithoutChecks: {
                    if (placeBlocksWithoutChecks()) {
                        continue;
                    }
                    break loop;
                }
                case SelectPickaxeAndReadyMine: {
                    selectPickaxeAndReadyMine();
                    break loop;
                }
                case WaitForPistonExtend: {
                    if (waitForPistonExtend()) {
                        continue;
                    }
                    break loop;
                }
                case Execute: {
                    execute();
                    break loop;
                }
                case WaitForPistonClear: {
                    if (waitForPistonClear()) {
                        continue;
                    }
                    break loop;
                }
                case ClearPiston: {
                    clearPiston();
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

    private boolean start() {
        final ClientWorld world;
        final ClientPlayerEntity player;
        final PlayerInventory inventory;
        final RotationUtils rotationUtils;
        {
            BlockMinerMod.TicklyUpdateConstants ticklyUpdateConstants = BlockMinerMod.getInstance().ticklyUpdateConstants();
            world = ticklyUpdateConstants.world();
            player = ticklyUpdateConstants.player();
            inventory = ticklyUpdateConstants.inventory();
            rotationUtils = BlockMinerMod.getInstance().getRotationUtils();
        }
        if (player.currentScreenHandler != player.playerScreenHandler) {
            return false;
        }
        if (!BlockUtils.playerCanTouchServerside(player, targetPos, 0)) {
            return false;
        }
        pistonIndex = InventoryUtils.findFirstItemInHotbar(inventory, Items.PISTON);
        if (pistonIndex == -1 && BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode()) {
            pistonIndex = InventoryUtils.findFirstItemInHotbar(inventory, Items.STICKY_PISTON);
        }
        if (pistonIndex == -1) {
            return false;
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
            return false;
        for (BlockBreakStructureFull structure : (Iterable<? extends BlockBreakStructureFull>) BlockMinerMod.getInstance().getConfig().getSearchMode().findPossibleStructures(world, targetPos, powerBlockUsage, dependBlockIndex != -1)::iterator) {
            if (BlockUtils.playerCanTouchServerside(player, structure.getPistonPos(), 1, false)
                    && BlockUtils.playerCanTouchServerside(player, structure.getPowerBlockPos(), 1, false)
                    && BlockUtils.playerCanTouchServerside(player, structure.getDependBlockPos(), 1, false)
                    && world.canPlace(Blocks.STONE.getDefaultState(), structure.getPistonPos(), ShapeContext.absent())
                    // 当依赖方块是目标方块时，无法创建无头活塞
                    && (!BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode() || !structure.getDependBlockPos().equals(targetPos))
                    // 保证依赖方块不会被其他task意外清除
                    && !BlockMinerMod.getInstance().getTaskManager().positionsToClear().hasPos(structure.getDependBlockPos())
            ) {
                this.structure = structure;
                if (BlockMinerMod.getInstance().getConfig().getEasyPlaceProtocol() != EasyPlaceProtocol.None) {
                    // EasyPlace下不需要等yaw延迟 甚至省了uncertain部分 我真的哭死
                    state = TaskState.PlaceBlocksWithoutChecks;
                    return true;
                } else {
                    if (structure.getPistonFace().getAxis() != Direction.Axis.Y) {
                        float yaw;
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
                        if (rotationUtils.canPushYaw(yaw)) {
                            assertTrue(rotationUtils.tryPushYaw(yaw, Lookup.of(this)));
                            rotationUtils.markKeepYaw(Lookup.of(this));
                            rotationUtils.updateLocation(player);
                            state = TaskState.WaitForPistonPlaceRotate;
                            setUncertainManagerKeepTicks(1);
                            ((ClientPlayerEntityAccessor) player).invokeSendMovementPackets();
                            return true;
                        }
                        return false; // 此处可改为continue
                    } else {
                        state = TaskState.PlaceBlocksWithoutChecks;
                        // 继续循环
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean waitForPistonPlaceRotate() {
        if (!BlockMinerMod.getInstance().getUncertainManager().isYawDirectionKeeping(uncertainManagerKeepTicks)) {
            BlockMinerMod.getInstance().getRotationUtils().markKeepYaw(Lookup.of(this));
            return false;
        }
        state = TaskState.PlaceBlocksWithChecks;
        return true;
    }

    private boolean placeBlocksWithChecks() {
        final ClientWorld world;
        final ClientPlayerEntity player;
        final PlayerInventory inventory;
        {
            BlockMinerMod.TicklyUpdateConstants ticklyUpdateConstants = BlockMinerMod.getInstance().ticklyUpdateConstants();
            world = ticklyUpdateConstants.world();
            player = ticklyUpdateConstants.player();
            inventory = ticklyUpdateConstants.inventory();
        }
        final BlockState dependBlockState = world.getBlockState(structure.getDependBlockPos());
        if ((inventory.getStack(pistonIndex).getItem() != Items.PISTON && (BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode() && inventory.getStack(pistonIndex).getItem() != Items.STICKY_PISTON))
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
            return false;
        }
        if (!structure.testBeforePlace(world, targetPos, dependBlockIndex != -1)) {
            retry();
            return false;
        }
        if (!BlockUtils.playerCanTouchServerside(player, structure.getPistonPos(), 1, false)
                || !BlockUtils.playerCanTouchServerside(player, structure.getPowerBlockPos(), 1, false)
                || !BlockUtils.playerCanTouchServerside(player, structure.getDependBlockPos(), 1, false)) {
            // 距离不够，重新找
            retry();
            return false;
        }
        return true;
    }

    private boolean placeBlocksWithoutChecks() {
        final ClientWorld world;
        final ClientPlayerEntity player;
        final PlayerInventory inventory;
        final ClientPlayerInteractionManager interactionManager;
        final RotationUtils rotationUtils;
        {
            BlockMinerMod.TicklyUpdateConstants ticklyUpdateConstants = BlockMinerMod.getInstance().ticklyUpdateConstants();
            //noinspection resource
            world = ticklyUpdateConstants.world();
            player = ticklyUpdateConstants.player();
            inventory = ticklyUpdateConstants.inventory();
            interactionManager = ticklyUpdateConstants.interactionManager();
            rotationUtils = BlockMinerMod.getInstance().getRotationUtils();
        }
        if (player.currentScreenHandler != player.playerScreenHandler) {
            retry();
            return false;
        }
        if (redstoneTorchIndex == -1 || !structure.getPowerBlockType().isRedstoneTorch()) {
            // 对于拉杆放置，检查是否无法使用拉杆
            if (canSwitchHandDenyUse(player, inventory)) {
                retry();
                return false;
            }
        }
        {
            // 凭空放置
            final ActionResult result;
            final EasyPlaceProtocol easyPlaceProtocol = BlockMinerMod.getInstance().getConfig().getEasyPlaceProtocol();
            if (easyPlaceProtocol == EasyPlaceProtocol.V3) {
                result = InventoryUtils.moveToOffHandDuring(player,
                        pistonIndex,
                        () -> EasyPlaceUtils.placePistonV3(
                                interactionManager,
                                player,
                                world,
                                Hand.OFF_HAND,
                                new BlockHitResult(
                                        Vec3d.of(structure.getPistonPos()),
                                        Direction.DOWN,
                                        structure.getPistonPos(),
                                        false
                                ),
                                structure.getPistonFace()
                        )
                );
            } else if (easyPlaceProtocol == EasyPlaceProtocol.V2) {
                result = InventoryUtils.moveToOffHandDuring(player,
                        pistonIndex,
                        () -> EasyPlaceUtils.placePistonCarpetExtra(
                                interactionManager,
                                player,
                                world,
                                Hand.OFF_HAND,
                                new BlockHitResult(
                                        Vec3d.of(structure.getPistonPos()),
                                        Direction.DOWN,
                                        structure.getPistonPos(),
                                        false
                                ),
                                structure.getPistonFace()
                        )
                );
            } else {
                float pitch = 0F;
                final Direction pistonFace = structure.getPistonFace();
                if (pistonFace.getAxis() == Direction.Axis.Y) {
                    pitch = pistonFace == Direction.UP ? 90F : -90F;
                }
                final float finalPitch = pitch;
                result = InventoryUtils.moveToOffHandDuring(player,
                        pistonIndex,
                        () -> rotationUtils.pushPitchAndUpdateLocationDuring(
                                player,
                                finalPitch,
                                Lookup.of(this),
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
                        )
                );
            }
            // 放不了，怎么回事呢？重来一遍
            if (!result.isAccepted()) {
                retry();
                return false;
            }
        }
        if (BlockUtils.isReplaceable(world.getBlockState(structure.getDependBlockPos()))) {
            if (dependBlockIndex == -1) {
                // 本来那有个方块但消失了，手里又没有粘液块，回去重找
                retry();
                return false;
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
            ActionResult result = PlayerUtils.sneakDuring(player,
                    () -> InventoryUtils.moveToOffHandDuring(player,
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
                    )
            );
            assertTrue(result.isAccepted());
        } else if (leverIndex != -1 && structure.getPowerBlockType().isLever()) {
            redstoneTorchIndex = -1;
            // 放在那个方块上
            ActionResult result = PlayerUtils.sneakDuring(player,
                    () -> InventoryUtils.moveToOffHandDuring(player,
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
            assertTrue(result.isAccepted());
        } else {
            shouldNotReachHere();
        }
        state = TaskState.SelectPickaxeAndReadyMine;
        return true;
    }

    private void selectPickaxeAndReadyMine() {
        final ClientPlayerEntity player;
        final PlayerInventory inventory;
        final ClientPlayerInteractionManager interactionManager;
        final RotationUtils rotationUtils;
        {
            BlockMinerMod.TicklyUpdateConstants ticklyUpdateConstants = BlockMinerMod.getInstance().ticklyUpdateConstants();
            player = ticklyUpdateConstants.player();
            inventory = ticklyUpdateConstants.inventory();
            interactionManager = ticklyUpdateConstants.interactionManager();
            rotationUtils = BlockMinerMod.getInstance().getRotationUtils();
        }
        if (pickaxeIndex != -1) {
            InventoryUtils.setSelectedSlot(inventory, pickaxeIndex);
            ((ClientPlayerInteractionManagerAccessor) interactionManager).invokeSyncSelectedSlot();
        }
        Direction pistonToTargetBlockFace = structure.getPistonOffset().getOpposite();

        boolean startBreakPistonLater = false;
        blockBreakingDelta = InventoryUtils.calcBlockBreakingDelta(player, Blocks.PISTON.getDefaultState(), inventory.getSelectedStack());
        if (blockBreakingDelta < 1) {
            if (blockBreakingDelta < 0.7 && redstoneTorchIndex != -1) {
                // 挖得太慢了，破不了，回去重试
                // 这里真的要重试吗？别的任务占用挖掘和挖不到方块的情况下都能等 这里不能等吗？先注释看看
                //retry();
                return;
            }
            if (blockBreakingDelta >= 0.7) {
                // 延后挖掘到实际放置活塞的那一刻...?
                // 但是如果一直碰不到还可能会导致视角卡在这里 麻烦欸
                // 这块多做个用处不大的检查得了
                if (BlockMinerMod.getInstance().getConfig().getEasyPlaceProtocol() == EasyPlaceProtocol.None && !BlockUtils.playerCanTouchServerside(player, structure.getPistonPos(), 1, true)) {
                    return;
                }
            } else if (BlockUtils.playerCanTouchServerside(player, structure.getPistonPos(), 1, true) && !BlockMinerMod.getInstance().getBlockBreakUtils().isModBreakingBlock()) {
                startBreakPistonLater = true;
            } else {
                // 有别的任务在占用挖掘或者挖不到方块，一会再检查一遍
                return;
            }
        }
        if (BlockMinerMod.getInstance().getConfig().getEasyPlaceProtocol() == EasyPlaceProtocol.None && pistonToTargetBlockFace.getAxis() != Direction.Axis.Y) {
            float yaw;
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
            if (rotationUtils.tryPushYaw(yaw, Lookup.of(this))) {
                rotationUtils.markKeepYaw(Lookup.of(this));
                rotationUtils.updateLocation(player);
            } else {
                return;
            }
        }
        if (startBreakPistonLater) {
            BlockMinerMod.getInstance().getBlockBreakUtils().setBreaking(true);
            isMining = true;
            interactionManager.cancelBlockBreaking();
            interactionManager.attackBlock(structure.getPistonPos(), Direction.DOWN);
        }
        // 等待活塞进入正在展开状态，不需要完全展开
        state = TaskState.WaitForPistonExtend;
        setWaitTicks(blockBreakingDelta >= 0.7 ? 1 : (int) Math.ceil(0.7 / blockBreakingDelta) + 1);
    }

    private boolean waitForPistonExtend() {
        final PlayerInventory inventory;
        final ClientPlayerInteractionManager interactionManager;
        final RotationUtils rotationUtils;
        {
            BlockMinerMod.TicklyUpdateConstants ticklyUpdateConstants = BlockMinerMod.getInstance().ticklyUpdateConstants();
            inventory = ticklyUpdateConstants.inventory();
            interactionManager = ticklyUpdateConstants.interactionManager();
            rotationUtils = BlockMinerMod.getInstance().getRotationUtils();
        }
        if (getWaitTicksAfterDecrement() > 0) {
            if (BlockMinerMod.getInstance().getConfig().getEasyPlaceProtocol() == EasyPlaceProtocol.None && structure.getPistonOffset().getAxis() != Direction.Axis.Y) {
                rotationUtils.markKeepYaw(Lookup.of(this));
            }
            if (pickaxeIndex != -1) {
                InventoryUtils.setSelectedSlot(inventory, pickaxeIndex);
                ((ClientPlayerInteractionManagerAccessor) interactionManager).invokeSyncSelectedSlot();
            }
            return false;
        }
        state = TaskState.Execute;
        return true;
    }

    private void execute() {
        final ClientWorld world;
        final ClientPlayerEntity player;
        final PlayerInventory inventory;
        final ClientPlayerInteractionManager interactionManager;
        final RotationUtils rotationUtils;
        {
            BlockMinerMod.TicklyUpdateConstants ticklyUpdateConstants = BlockMinerMod.getInstance().ticklyUpdateConstants();
            //noinspection resource
            world = ticklyUpdateConstants.world();
            player = ticklyUpdateConstants.player();
            inventory = ticklyUpdateConstants.inventory();
            interactionManager = ticklyUpdateConstants.interactionManager();
            rotationUtils = BlockMinerMod.getInstance().getRotationUtils();
        }
        if (pickaxeIndex != -1) {
            InventoryUtils.setSelectedSlot(inventory, pickaxeIndex);
            ((ClientPlayerInteractionManagerAccessor) interactionManager).invokeSyncSelectedSlot();
        }
        // 如果太远挖不到活塞...
        // 如果太远碰不到能源方块...
        // 如果是无头活塞模式，且此task使用红石火把，且太远碰不到红石火把依附的方块...
        // 如果是拉杆模式且无法使用拉杆...
        // 如果之前能秒破活塞但现在不能 或者别的地方正在挖掘...
        if (
                (!BlockUtils.playerCanTouchServerside(player, structure.getPistonPos(), 1, true))
                        || (!BlockUtils.playerCanTouchServerside(player, structure.getPowerBlockPos(), 1, true))
                        || (redstoneTorchIndex != -1 && BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode() && !BlockUtils.playerCanTouchServerside(player, structure.getDependBlockPos(), 1, false))
                        || (leverIndex != -1 && canSwitchHandDenyUse(player, inventory))
                        || (blockBreakingDelta >= 0.7 && (InventoryUtils.calcBlockBreakingDelta(player, Blocks.PISTON.getDefaultState(), player.getMainHandStack()) < 0.7 || BlockMinerMod.getInstance().getBlockBreakUtils().isModBreakingBlock()))
        ) {
            // 延后。
            if (BlockMinerMod.getInstance().getConfig().getEasyPlaceProtocol() == EasyPlaceProtocol.None && structure.getPistonOffset().getAxis() != Direction.Axis.Y) {
                rotationUtils.markKeepYaw(Lookup.of(this));
            }
            return;
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
                return;
            }
        }
        if (blockBreakingDelta >= 0.7) {
            float blockBreakingDeltaNow = InventoryUtils.calcBlockBreakingDelta(player, Blocks.PISTON.getDefaultState(), player.getMainHandStack());
            // 这里应该必然是0.7以上 上面检查过了
            assertTrue(blockBreakingDeltaNow >= 0.7);
//            if (blockBreakingDeltaNow < 0.7) {
//                // 之前能秒破活塞但现在不能了，回去
//                retry();
//                return;
//            }
            interactionManager.attackBlock(structure.getPistonPos(), Direction.DOWN);
            if (world.getBlockState(structure.getPistonPos()).getBlock() instanceof PistonBlock) {
                if (blockBreakingDeltaNow < 1) {
                    ClientPlayerInteractionManagerAccessor interactionManagerAccessor = (ClientPlayerInteractionManagerAccessor) interactionManager;
                    while (interactionManager.isBreakingBlock() && interactionManagerAccessor.invokeIsCurrentlyBreaking(structure.getPistonPos()))
                        interactionManager.updateBlockBreakingProgress(structure.getPistonPos(), Direction.DOWN);
                } else {
                    retry();
                    return;
                }
                if (!world.getBlockState(structure.getPistonPos()).isAir())
                    world.setBlockState(structure.getPistonPos(), Blocks.AIR.getDefaultState());
            } else if (blockBreakingDeltaNow < 1) {
                // TODO: 已经不记得这里为什么要retry了 下次看看
                retry();
                return;
            }
        } else {
            if (BlockUtils.getHardness(world.getBlockState(structure.getPistonPos())) < 0) {
                // 怎么回事？byd活塞变基岩了？
                retry();
                return;
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
                result = PlayerUtils.sneakDuring(player,
                        () -> InventoryUtils.moveToOffHandDuring(player,
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
                return;
            }
        }
        // 重新凭空放置活塞
        ActionResult result;
        final EasyPlaceProtocol easyPlaceProtocol = BlockMinerMod.getInstance().getConfig().getEasyPlaceProtocol();
        if (easyPlaceProtocol == EasyPlaceProtocol.V3) {
            result = InventoryUtils.moveToOffHandDuring(
                    player,
                    pistonIndex,
                    () -> EasyPlaceUtils.placePistonV3(
                            interactionManager,
                            player,
                            world,
                            Hand.OFF_HAND,
                            new BlockHitResult(
                                    Vec3d.of(structure.getPistonPos()),
                                    Direction.DOWN,
                                    structure.getPistonPos(),
                                    false
                            ),
                            structure.getPistonOffset().getOpposite()
                    )
            );
        } else if (easyPlaceProtocol == EasyPlaceProtocol.V2) {
            result = InventoryUtils.moveToOffHandDuring(
                    player,
                    pistonIndex,
                    () -> EasyPlaceUtils.placePistonCarpetExtra(
                            interactionManager,
                            player,
                            world,
                            Hand.OFF_HAND,
                            new BlockHitResult(
                                    Vec3d.of(structure.getPistonPos()),
                                    Direction.DOWN,
                                    structure.getPistonPos(),
                                    false
                            ),
                            structure.getPistonOffset().getOpposite()
                    )
            );
        } else {
            float pitch = 0F;
            final Direction pistonToTargetBlockFace = structure.getPistonOffset().getOpposite();
            if (pistonToTargetBlockFace.getAxis() == Direction.Axis.Y) {
                pitch = pistonToTargetBlockFace == Direction.UP ? 90F : -90F;
            }
            final float finalPitch = pitch;
            result = InventoryUtils.moveToOffHandDuring(
                    player,
                    pistonIndex,
                    () -> rotationUtils.pushPitchAndUpdateLocationDuring(
                            player,
                            finalPitch,
                            Lookup.of(this),
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
                    )
            );
        }
        if (!result.isAccepted()) {
            // ?????
            retry();
            return;
        }
        if (!BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode() && BlockMinerMod.getInstance().getConfig().isAutoClearAfterTask()) {
            // 检查dependBlockPos处的方块是否硬度==0 并且确保没有方块依附在dependBlockPos处
            dependBlockClearCheck: {
                final BlockPos dependBlockPos = structure.getDependBlockPos();
                final BlockState dependBlockState = world.getBlockState(dependBlockPos);
                if (BlockUtils.getHardness(dependBlockState) != 0)
                    break dependBlockClearCheck;
                // 如果已经是空气，不需要清除，但真的会是空气吗？
                if (dependBlockState.isAir())
                    break dependBlockClearCheck;
                if (!BlockMinerMod.getInstance().getConfig().dependBlockWhitelistContains(dependBlockState.getBlock()))
                    break dependBlockClearCheck;
                for (Direction direction : BlockFinder.DIRECTIONS) {
                    final BlockPos dependBlockNearPos = dependBlockPos.offset(direction);
                    if (world.isInBuildLimit(dependBlockNearPos)) {
                        final BlockState dependBlockNearPosState = world.getBlockState(dependBlockNearPos);
                        final Block dependBlockNearPosBlock = dependBlockNearPosState.getBlock();
                        if (dependBlockNearPosBlock instanceof RedstoneTorchBlock) {
                            Direction redstoneTorchFace;
                            if (dependBlockNearPosBlock instanceof WallRedstoneTorchBlock) {
                                redstoneTorchFace = dependBlockNearPosState.get(WallRedstoneTorchBlock.FACING);
                            } else {
                                redstoneTorchFace = Direction.UP;
                            }
                            if (redstoneTorchFace == direction)
                                break dependBlockClearCheck;
                        } else if (dependBlockNearPosBlock instanceof LeverBlock) {
                            Direction leverFace;
                            switch (dependBlockNearPosState.get(net.minecraft.state.property.Properties.BLOCK_FACE)) {
                                case CEILING: {
                                    leverFace = Direction.DOWN;
                                    break;
                                }
                                case FLOOR: {
                                    leverFace = Direction.UP;
                                    break;
                                }
                                default: {
                                    leverFace = dependBlockNearPosState.get(LeverBlock.FACING);
                                    break;
                                }
                            }
                            if (leverFace == direction)
                                break dependBlockClearCheck;
                        }
                    }
                }

                BlockMinerMod.getInstance().getTaskManager().positionsToClear().registerPos(dependBlockPos);
            }
            // 由于活塞需要等到服务端运行下一tick破掉目标方块，等待
            state = TaskState.WaitForPistonClear;
            setWaitTicks(8); // TODO 不清楚活塞几tick能到位 后面再改？
        } else {
            // 无头活塞模式或不事后清理模式，不需要事后清理
            state = TaskState.Finished;
        }
    }

    private boolean waitForPistonClear() {
        final ClientWorld world;
        final PlayerInventory inventory;
        final ClientPlayerInteractionManager interactionManager;
        {
            BlockMinerMod.TicklyUpdateConstants ticklyUpdateConstants = BlockMinerMod.getInstance().ticklyUpdateConstants();
            //noinspection resource
            world = ticklyUpdateConstants.world();
            inventory = ticklyUpdateConstants.inventory();
            interactionManager = ticklyUpdateConstants.interactionManager();
        }
        if (getWaitTicksAfterDecrement() > 0) {
            if (pickaxeIndex != -1) {
                InventoryUtils.setSelectedSlot(inventory, pickaxeIndex);
                ((ClientPlayerInteractionManagerAccessor) interactionManager).invokeSyncSelectedSlot();
            }
            final BlockState pistonPosState = world.getBlockState(structure.getPistonPos());
            final Block pistonPosBlock = pistonPosState.getBlock();
            if (pistonPosBlock instanceof PistonBlock) {
                // 如果活塞不朝向目标方块 说明完成了计划刻继承
                if (pistonPosState.get(Properties.FACING) != structure.getPistonOffset().getOpposite()) {
                    stopWaiting();
                    state = TaskState.ClearPiston;
                    return true;
                }
            } else if (!(pistonPosBlock instanceof PistonExtensionBlock)) {
                // 活塞所在的位置被其他方块替代了，放弃清理活塞
                stopWaiting();
                state = TaskState.Finished;
            }
            return false;
        }
        final BlockState pistonPosState = world.getBlockState(structure.getPistonPos());
        if (pistonPosState.getBlock() instanceof PistonBlock && pistonPosState.get(Properties.FACING) != structure.getPistonOffset().getOpposite()) {
            // 如果活塞不朝向目标方块 说明完成了计划刻继承
            state = TaskState.ClearPiston;
            return true;
        } else {
            // 这么久还没破掉？放弃清理活塞
            state = TaskState.Finished;
            return false;
        }
    }

    private void clearPiston() {
        final ClientWorld world;
        {
            BlockMinerMod.TicklyUpdateConstants ticklyUpdateConstants = BlockMinerMod.getInstance().ticklyUpdateConstants();
            //noinspection resource
            world = ticklyUpdateConstants.world();
        }
        final BlockPos pistonPos = structure.getPistonPos();
        final BlockState pistonPosState = world.getBlockState(pistonPos);
        if (pistonPosState.getBlock() instanceof PistonBlock && pistonPosState.get(Properties.FACING) != structure.getPistonOffset().getOpposite()) {
            BlockMinerMod.getInstance().getTaskManager().positionsToClear().registerPos(pistonPos);
        }
        state = TaskState.Finished;
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
    
    public static class Lookup {
        @NotNull
        public final Task instance;

        private Lookup(Task instance) {
            this.instance = Objects.requireNonNull(instance);
        }

        static Lookup of(Task instance) {
            return new Lookup(instance);
        }
    }
}
