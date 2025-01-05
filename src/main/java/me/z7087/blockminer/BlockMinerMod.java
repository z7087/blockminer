package me.z7087.blockminer;

import me.z7087.blockminer.command.Command;
import me.z7087.blockminer.config.Config;
import me.z7087.blockminer.task.TaskManager;
import me.z7087.blockminer.util.BlockBreakUtils;
import me.z7087.blockminer.util.RotationUtils;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class BlockMinerMod implements ClientModInitializer {
    public static final String MOD_ID = "blockminer";
    public static final Logger LOGGER = LoggerFactory.getLogger(BlockMinerMod.class);
    public static BlockMinerMod INSTANCE;
    public Config config = Config.createDefaultConfig();
    public final TaskManager taskManager = new TaskManager();
    public final BlockBreakUtils blockBreakUtils = new BlockBreakUtils();
    public final RotationUtils rotationUtils = new RotationUtils();

    public BlockMinerMod() {
        INSTANCE = this;
    }

    @Override
    public void onInitializeClient() {
        Config tconfig = Config.loadFromFile();
        if (tconfig != null)
            config = tconfig;
        boolean hasFabricCommandApi = true;
        try {
            Class<?> ignored = net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.class;
        } catch (NoClassDefFoundError e) {
            hasFabricCommandApi = false;
        }
        //noinspection ConstantValue
        if (hasFabricCommandApi)
            Command.load();
    }

    public void tryToSaveConfig() {
        try {
            Config.saveToFile(config);
        } catch (IOException e) {
            LOGGER.error("config saving failed: {}", e.getMessage(), e);
        }
    }
}
