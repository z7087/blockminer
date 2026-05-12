package me.z7087.blockminer.task;

import it.unimi.dsi.fastutil.longs.LongListIterator;
import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.I18n;
import me.z7087.blockminer.mixin.minecraft.client.network.ClientPlayerInteractionManagerAccessor;
import me.z7087.blockminer.util.*;
import me.z7087.blockminer.util.data.PositionStorage;
import me.z7087.blockminer.api.enums.TaskState;
import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;
import me.z7087.final2constant.util.JavaHelper;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Function;

// 单方块方案：
// 先拿到所有可能的活塞能源方块放置方法
// 预先按：俩都行-红石火把-拉杆；无需额外方块-需要额外方块；排序，从目标方块开始从内向外遍历曼哈顿距离<=4的方块，对每个方块遍历那个set，方块属性不通过就移除
// --
// 多方块方案：
// 在addAura的时候尝试为群体添加task，尽量让一个信号源激活多个活塞
// --
// 其他：
// 使用其他更简单的搜索算法，用无法涵盖所有情况换取高搜索性能

public abstract class TaskManager {
    protected TaskManager() {}

    private static final MethodHandle CONSTRUCTOR;
    static {
        final String[] immutableNames, immutableDescriptors;
        try {
            final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                    MethodHandles.lookup(),
                    (Function<TaskManager, DynamicConstant<Boolean>> & Serializable) TaskManager::enabled,
                    (Function<TaskManager, DynamicConstant<WeakReference<ClientWorld>>> & Serializable) TaskManager::prevWorldRef,
                    (Function<TaskManager, Set<BlockPos>> & Serializable) TaskManager::posSet,
                    (Function<TaskManager, LinkedList<Task>> & Serializable) TaskManager::taskQueue,
                    (Function<TaskManager, PositionStorage> & Serializable) TaskManager::positionsToClear
            );
            immutableNames = immutableNamesAndDescriptors[0];
            immutableDescriptors = immutableNamesAndDescriptors[1];
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        CONSTRUCTOR = Constant.factory.ofRecordConstructor(
                MethodHandles.lookup(),
                TaskManager.class,
                false,
                immutableNames,
                immutableDescriptors,
                null,
                null,
                true,
                false
        );
    }

    public static TaskManager createInstance() {
        final DynamicConstant<Boolean> enabled = Constant.factory.ofVolatile(Boolean.FALSE);
        final DynamicConstant<WeakReference<ClientWorld>> prevWorldRef = Constant.factory.ofMutable(null);
        final Set<BlockPos> posSet = new HashSet<>();
        final LinkedList<Task> taskQueue = new LinkedList<>();
        final PositionStorage positionsToClear = PositionStorage.createInstance();
        try {
            return (TaskManager) CONSTRUCTOR.invokeExact(
                    enabled,
                    prevWorldRef,
                    posSet,
                    taskQueue,
                    positionsToClear
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    abstract DynamicConstant<Boolean> enabled();
    abstract DynamicConstant<WeakReference<ClientWorld>> prevWorldRef();
    abstract Set<BlockPos> posSet();
    abstract LinkedList<Task> taskQueue();
    abstract PositionStorage positionsToClear();

    public void tick() {
        if (!isEnabled())
            return;
        BlockMinerMod.getInstance().getRotationUtils().resetYawRotationIfNoKeepYaw();
        final ClientPlayerEntity player = BlockMinerMod.getInstance().ticklyUpdateConstants().player();
        if (player == null) {
            return;
        }
        final ClientWorld world = BlockMinerMod.getInstance().ticklyUpdateConstants().world();
        if (world == null) {
            this.setPrevWorldRef(null);
            onDisable();
            return;
        } else {
            WeakReference<ClientWorld> prevWorldRef = this.getPrevWorldRef();
            if (prevWorldRef == null || prevWorldRef.get() != world) {
                this.setPrevWorldRef(new WeakReference<>(world));
                onDisable();
                return;
            }
        }
        BlockMinerMod.getInstance().getUncertainManager().tickAndCheck();
        final ClientConnection connection = player.networkHandler.getConnection();
        final PositionStorage positionsToClear = positionsToClear();
        final boolean startedBlinking = connection != null
                && BlockMinerMod.getInstance().getConfig().isBlinkDuringTasksTick()
                && !taskQueue().isEmpty()
                && BlinkUtils.tryStartBlinking(connection);
        try {
            final Iterator<Task> taskIterator = taskQueue().iterator();
            while (taskIterator.hasNext()) {
                final Task task = taskIterator.next();
                final boolean ignoreOtherTasks = task.tick();
                if (task.state == TaskState.Finished) {
                    taskIterator.remove();
                    posSet().remove(task.targetPos);
                }
                if (ignoreOtherTasks)
                    break;
            }
            if (!BlockMinerMod.getInstance().getBlockBreakUtils().isModBreakingBlock() && BlockMinerMod.getInstance().getConfig().isAutoClearAfterTask()) {
                final ClientPlayerInteractionManager interactionManager = BlockMinerMod.getInstance().ticklyUpdateConstants().interactionManager();
                final ItemStack mainHandStack = player.getMainHandStack();
                final LongListIterator positionsToClearIterator = positionsToClear.iterator();
                while (positionsToClearIterator.hasNext()) {
                    final BlockPos pos = BlockPos.fromLong(positionsToClearIterator.nextLong());
                    if (BlockUtils.playerCanTouchServerside(player, pos, 1.0, true)) {
                        final BlockState state = world.getBlockState(pos);
                        final float blockBreakingDelta = InventoryUtils.calcBlockBreakingDelta(player, state, mainHandStack);
                        if (blockBreakingDelta >= 1) {
                            if (!state.isAir()) {
                                interactionManager.cancelBlockBreaking();
                                interactionManager.attackBlock(pos, Direction.DOWN);
                            }
                            positionsToClearIterator.remove();
                        } else if (blockBreakingDelta >= 0.7) {
                            interactionManager.cancelBlockBreaking();
                            interactionManager.attackBlock(pos, Direction.DOWN);
                            ClientPlayerInteractionManagerAccessor interactionManagerAccessor = (ClientPlayerInteractionManagerAccessor) interactionManager;
                            while (interactionManager.isBreakingBlock() && interactionManagerAccessor.invokeIsCurrentlyBreaking(pos))
                                interactionManager.updateBlockBreakingProgress(pos, Direction.DOWN);
                            positionsToClearIterator.remove();
                        } else {
                            final Block block = state.getBlock();
                            // 不可秒破且不是活塞或移动中的方块，可能是后加的什么东西，不管了
                            if (!(block instanceof PistonBlock || block instanceof PistonExtensionBlock)) {
                                positionsToClearIterator.remove();
                            }
                        }
                    }
                }
            }
        } finally {
            if (startedBlinking) {
                BlinkUtils.tryStopBlinking(connection);
            }
        }
    }
    public boolean handleAttackBlock(BlockPos blockPos) {
        if (!isEnabled())
            return false;
        final ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        if (!BlockMinerMod.getInstance().getConfig().blockWhitelist().contains(world.getBlockState(blockPos).getBlock()))
            return false;
        return addTask(blockPos);
    }
    public boolean handleUseOnBlock(BlockPos targetBlock) {
        final ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        if (!BlockMinerMod.getInstance().getConfig().blockWhitelist().contains(world.getBlockState(targetBlock).getBlock()))
            return false;
        toggle();
        return true;
    }
    public boolean addTask(BlockPos pos) {
        if (posSet().contains(pos))
            return false;
        addTask0(pos);
        return true;
    }
    private void addTask0(BlockPos pos) {
        final Task task = Task.of(pos);
        posSet().add(pos);
        taskQueue().add(task);
    }

    public boolean addAura(BlockPos start, BlockPos end) {
        return addAura(start, end, true);
    }

    public boolean addAura(BlockPos start, BlockPos end, boolean checkWhitelist) {
        final Set<Block> whitelist = BlockMinerMod.getInstance().getConfig().blockWhitelist();
        ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        Iterator<BlockPos> iterator = BlockPos.iterate(start, end).iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (world.isInBuildLimit(pos) && !posSet().contains(pos) && (!checkWhitelist || whitelist.contains(world.getBlockState(pos).getBlock()))) {
                addTask0(pos.toImmutable());
                while (iterator.hasNext()) {
                    pos = iterator.next();
                    if (world.isInBuildLimit(pos) && !posSet().contains(pos) && (!checkWhitelist || whitelist.contains(world.getBlockState(pos).getBlock()))) {
                        addTask0(pos.toImmutable());
                    }
                }
                return true;
            }
        }
        return false;
    }

    public void toggle() {
        toggle(true, true);
    }

    public void toggle(boolean withToggleMessage, boolean withWarnMultiplayerMessage) {
        if (isEnabled()) {
            onDisable();
            if (withToggleMessage)
                MessageUtils.printMessage(I18n.TOGGLE_OFF);
        } else {
            WeakReference<ClientWorld> prevWorldRef = this.getPrevWorldRef();
            onEnable();
            if (withToggleMessage) {
                MessageUtils.printMessage(I18n.TOGGLE_ON);
            }
            if (withWarnMultiplayerMessage) {
                // 每个世界只提醒一次
                if (!MinecraftClient.getInstance().isInSingleplayer() && prevWorldRef != this.getPrevWorldRef())
                    MessageUtils.printMessage(I18n.WARN_MULTIPLAYER);
            }
        }
    }

    private void clearTasks() {
        posSet().clear();
        taskQueue().clear();
    }

    private void onEnable() {
        this.setEnabled(true);
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            this.setPrevWorldRef(null);
        } else {
            WeakReference<ClientWorld> prevWorldRef = this.getPrevWorldRef();
            if (prevWorldRef == null || prevWorldRef.get() != world) {
                this.setPrevWorldRef(new WeakReference<>(world));
            }
        }
        BlockMinerMod.getInstance().getUncertainManager().startup();
    }

    private void onDisable() {
        this.setEnabled(false);
        //this.prevWorldRef = null;
        clearTasks();
        positionsToClear().clear();
        BlockMinerMod.getInstance().getUncertainManager().reset();
        BlockMinerMod.getInstance().getRotationUtils().forceClearRotations();
        BlockMinerMod.getInstance().getBlockBreakUtils().setBreaking(false);
    }

    public boolean isEnabled() {
        return enabled().orElseThrow();
    }

    private void setEnabled(boolean value) {
        enabled().set(value);
    }

    public boolean isTaskExists(BlockPos pos) {
        return posSet().contains(pos);
    }

    private WeakReference<ClientWorld> getPrevWorldRef() {
        return prevWorldRef().get();
    }

    private void setPrevWorldRef(WeakReference<ClientWorld> value) {
        prevWorldRef().set(value);
    }
}
