package me.z7087.blockminer.task;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.I18n;
import me.z7087.blockminer.util.BlinkUtils;
import me.z7087.blockminer.util.MessageUtils;
import me.z7087.blockminer.util.enums.TaskState;
import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.math.BlockPos;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.*;

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

    private static final MethodHandle CONSTRUCTOR = Constant.factory.ofRecordConstructor(
            MethodHandles.lookup(),
            TaskManager.class,
            false,
            new String[] {
                    "enabled",
                    "prevWorldRef",
                    "posSet",
                    "taskQueue"
            },
            new String[] {
                    Type.getDescriptor(DynamicConstant.class),
                    Type.getDescriptor(DynamicConstant.class),
                    Type.getDescriptor(Set.class),
                    Type.getDescriptor(LinkedList.class)
            },
            null,
            null,
            true,
            false
    );

    public static TaskManager createInstance() {
        final DynamicConstant<Boolean> enabled = Constant.factory.ofMutable(Boolean.FALSE);
        final DynamicConstant<WeakReference<ClientWorld>> prevWorldRef = Constant.factory.ofMutable(null);
        final Set<BlockPos> posSet = new HashSet<>();
        final LinkedList<Task> taskQueue = new LinkedList<>();
        try {
            return (TaskManager) CONSTRUCTOR.invokeExact(
                    enabled,
                    prevWorldRef,
                    posSet,
                    taskQueue
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    abstract DynamicConstant<Boolean> enabled();
    abstract DynamicConstant<WeakReference<ClientWorld>> prevWorldRef();
    abstract Set<BlockPos> posSet();
    abstract LinkedList<Task> taskQueue();

    public void tick() {
        BlockMinerMod.getInstance().getRotationUtils().resetRotationIfNoKeepRotation();
        if (!isEnabled())
            return;
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        final ClientWorld world = MinecraftClient.getInstance().world;
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
        final ClientConnection connection = player.networkHandler.getConnection();
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
        if (posSet().contains(blockPos))
            return false;
        final Task task = Task.of(blockPos);
        posSet().add(blockPos);
        taskQueue().add(task);
        return true;
    }
    public boolean handleUseOnBlock(BlockPos targetBlock) {
        final ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        if (!BlockMinerMod.getInstance().getConfig().blockWhitelist().contains(world.getBlockState(targetBlock).getBlock()))
            return false;
        toggle();
        return true;
    }

    public boolean addAura(BlockPos start, BlockPos end) {
        final Set<Block> whitelist = BlockMinerMod.getInstance().getConfig().blockWhitelist();
        ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        Iterator<BlockPos> iterator = BlockPos.iterate(start, end).iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (world.isInBuildLimit(pos) && !posSet().contains(pos) && whitelist.contains(world.getBlockState(pos).getBlock())) {
                pos = pos.toImmutable();
                posSet().add(pos);
                taskQueue().add(Task.of(pos));
                while (iterator.hasNext()) {
                    pos = iterator.next();
                    if (world.isInBuildLimit(pos) && !posSet().contains(pos) && whitelist.contains(world.getBlockState(pos).getBlock())) {
                        pos = pos.toImmutable();
                        posSet().add(pos);
                        taskQueue().add(Task.of(pos));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public void toggle() {
        if (isEnabled()) {
            onDisable();
            MessageUtils.printMessage(I18n.TOGGLE_OFF);
        } else {
            WeakReference<ClientWorld> prevWorldRef = this.getPrevWorldRef();
            onEnable();
            MessageUtils.printMessage(I18n.TOGGLE_ON);
            // 每个世界只提醒一次
            if (!MinecraftClient.getInstance().isInSingleplayer() && prevWorldRef != this.getPrevWorldRef())
                MessageUtils.printMessage(I18n.WARN_MULTIPLAYER);
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
    }

    private void onDisable() {
        this.setEnabled(false);
        //this.prevWorldRef = null;
        clearTasks();
        BlockMinerMod.getInstance().getRotationUtils().forceClearRotations();
        BlockMinerMod.getInstance().getBlockBreakUtils().setBreaking(false);
    }

    public boolean isEnabled() {
        return enabled().orElseThrow();
    }

    public void setEnabled(boolean value) {
        enabled().set(value);
    }

    private WeakReference<ClientWorld> getPrevWorldRef() {
        return prevWorldRef().get();
    }

    private void setPrevWorldRef(WeakReference<ClientWorld> value) {
        prevWorldRef().set(value);
    }
}
