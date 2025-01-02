package me.z7087.blockminer.task;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.util.BlockFinder;
import me.z7087.blockminer.util.MessageUtils;
import me.z7087.blockminer.util.data.Pair;
import me.z7087.blockminer.util.data.PistonPowerInfo;
import me.z7087.blockminer.util.enums.TaskState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Objects;

public class Task implements Comparable<Task> {
    public final BlockPos targetPos;
    private BlockPos pistonPos;
    public TaskState state = TaskState.Start;

    public Task(BlockPos targetPos) {
        this.targetPos = Objects.requireNonNull(targetPos);
    }

    public boolean tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        loop:
        {
            while (true) {
                switch (state) {
                    case Start: {
                        ArrayList<Pair<BlockPos, Direction>> pistonList = new ArrayList<>();
                        BlockFinder.findStablePistons(world, targetPos, pistonList);
                        //BlockMinerMod.LOGGER.info(pistonList.toString());
                        MessageUtils.printMessage(Text.of(pistonList.toString()));
                        ArrayList<PistonPowerInfo> pistonPowerInfos = new ArrayList<>();
                        BlockFinder.findPowerBlockForPiston(world, targetPos, BlockMinerMod.INSTANCE.config.powerBlockUsage, pistonList, pistonPowerInfos);
                        BlockMinerMod.LOGGER.info(pistonPowerInfos.toString());
                        //MessageUtils.printMessage(Text.of(pistonPowerInfos.toString()));
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
