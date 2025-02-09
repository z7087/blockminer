package me.z7087.blockminer.task;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.I18n;
import me.z7087.blockminer.util.BlinkUtils;
import me.z7087.blockminer.util.MessageUtils;
import me.z7087.blockminer.util.enums.TaskState;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.math.BlockPos;

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

public class TaskManager {
    private boolean enabled = false;
    private WeakReference<ClientWorld> prevWorldRef;
    private final Set<BlockPos> posSet = new HashSet<>();
    private final LinkedList<Task> taskQueue = new LinkedList<>();
    public void tick() {
        BlockMinerMod.INSTANCE.rotationUtils.resetRotationIfNoKeepRotation();
        if (!enabled)
            return;
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        final ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            this.prevWorldRef = null;
            onDisable();
            return;
        } else {
            WeakReference<ClientWorld> prevWorldRef = this.prevWorldRef;
            if (prevWorldRef == null || prevWorldRef.get() != world) {
                this.prevWorldRef = new WeakReference<>(world);
                onDisable();
                return;
            }
        }
        final ClientConnection connection = player.networkHandler.getConnection();
        final boolean startedBlinking = connection != null
                && BlockMinerMod.INSTANCE.config.blinkDuringTasksTick
                && !taskQueue.isEmpty()
                && BlinkUtils.tryStartBlinking(connection);
        try {
            final Iterator<Task> taskIterator = taskQueue.iterator();
            while (taskIterator.hasNext()) {
                final Task task = taskIterator.next();
                final boolean ignoreOtherTasks = task.tick();
                if (task.state == TaskState.Finished) {
                    taskIterator.remove();
                    posSet.remove(task.targetPos);
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
        if (!enabled)
            return false;
        final ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        if (!BlockMinerMod.INSTANCE.config.blockWhitelist.contains(world.getBlockState(blockPos).getBlock()))
            return false;
        if (posSet.contains(blockPos))
            return false;
        final Task task = Task.of(blockPos);
        posSet.add(blockPos);
        taskQueue.add(task);
        return true;
    }
    public boolean handleUseOnBlock(BlockPos targetBlock) {
        final ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        if (!BlockMinerMod.INSTANCE.config.blockWhitelist.contains(world.getBlockState(targetBlock).getBlock()))
            return false;
        toggle();
        return true;
    }

    public boolean addAura(BlockPos start, BlockPos end) {
        final Set<Block> whitelist = BlockMinerMod.INSTANCE.config.blockWhitelist;
        ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        Iterator<BlockPos> iterator = BlockPos.iterate(start, end).iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (world.isInBuildLimit(pos) && !posSet.contains(pos) && whitelist.contains(world.getBlockState(pos).getBlock())) {
                pos = pos.toImmutable();
                posSet.add(pos);
                taskQueue.add(Task.of(pos));
                while (iterator.hasNext()) {
                    pos = iterator.next();
                    if (world.isInBuildLimit(pos) && !posSet.contains(pos) && whitelist.contains(world.getBlockState(pos).getBlock())) {
                        pos = pos.toImmutable();
                        posSet.add(pos);
                        taskQueue.add(Task.of(pos));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        if (enabled) {
            onDisable();
            MessageUtils.printMessage(I18n.TOGGLE_OFF);
        } else {
            WeakReference<ClientWorld> prevWorldRef = this.prevWorldRef;
            onEnable();
            MessageUtils.printMessage(I18n.TOGGLE_ON);
            // 每个世界只提醒一次
            if (!MinecraftClient.getInstance().isInSingleplayer() && prevWorldRef != this.prevWorldRef)
                MessageUtils.printMessage(I18n.WARN_MULTIPLAYER);
        }
    }

    private void clearTasks() {
        posSet.clear();
        taskQueue.clear();
    }

    private void onEnable() {
        this.enabled = true;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            this.prevWorldRef = null;
        } else {
            WeakReference<ClientWorld> prevWorldRef = this.prevWorldRef;
            if (prevWorldRef == null || prevWorldRef.get() != world) {
                this.prevWorldRef = new WeakReference<>(world);
            }
        }
    }

    private void onDisable() {
        this.enabled = false;
        //this.prevWorldRef = null;
        clearTasks();
        BlockMinerMod.INSTANCE.rotationUtils.forceClearRotations();
        BlockMinerMod.INSTANCE.blockBreakUtils.setBreaking(false);
    }
}
