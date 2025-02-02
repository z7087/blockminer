package me.z7087.blockminer.task;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.I18n;
import me.z7087.blockminer.util.BlockUtils;
import me.z7087.blockminer.util.MessageUtils;
import me.z7087.blockminer.util.enums.TaskState;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

import java.lang.ref.WeakReference;
import java.util.*;

public class TaskManager {
    private boolean enabled = false;
    private WeakReference<ClientWorld> prevWorldRef;
    private final Set<BlockPos> posSet = new HashSet<>();
    private final LinkedList<Task> taskQueue = new LinkedList<>();
    public void tick() {
        BlockMinerMod.INSTANCE.rotationUtils.resetRotationIfNoKeepRotation();
        if (!enabled)
            return;
        if (MinecraftClient.getInstance().player == null) {
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
        Iterator<BlockPos> iterator = BlockPos.iterate(BlockUtils.clampToValidPos(start, world), BlockUtils.clampToValidPos(end, world)).iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!posSet.contains(pos) && whitelist.contains(world.getBlockState(pos).getBlock())) {
                pos = pos.toImmutable();
                posSet.add(pos);
                taskQueue.add(Task.of(pos));
                while (iterator.hasNext()) {
                    pos = iterator.next();
                    if (!posSet.contains(pos) && whitelist.contains(world.getBlockState(pos).getBlock())) {
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
