package me.z7087.blockminer.api;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.config.Config;
import net.minecraft.util.math.BlockPos;

@SuppressWarnings("unused")
public final class Apis {
    private Apis() {}

    public static boolean isEnabled() {
        return BlockMinerMod.getInstance().getTaskManager().isEnabled();
    }

    public static void toggle(boolean withToggleMessage, boolean withWarnMultiplayerMessage) {
        BlockMinerMod.getInstance().getTaskManager().toggle(withToggleMessage, withWarnMultiplayerMessage);
    }

    public static boolean addTask(BlockPos pos) {
        return BlockMinerMod.getInstance().getTaskManager().addTask(pos);
    }

    public static boolean addTasks(BlockPos start, BlockPos end, boolean checkWhitelist) {
        return BlockMinerMod.getInstance().getTaskManager().addAura(start, end, checkWhitelist);
    }

    public static boolean isTaskExists(BlockPos pos) {
        return BlockMinerMod.getInstance().getTaskManager().isTaskExists(pos);
    }


    public static Config getConfig() {
        return BlockMinerMod.getInstance().getConfig();
    }

    public static void setConfig(Config newConfig) {
        BlockMinerMod.getInstance().setConfig(newConfig);
    }

    public static void tryToSaveConfig() {
        BlockMinerMod.getInstance().tryToSaveConfig();
    }


    public static boolean isModBreakingBlock() {
        return BlockMinerMod.getInstance().getBlockBreakUtils().isModBreakingBlock();
    }
}
