package me.z7087.blockminer;

import me.z7087.blockminer.command.Command;
import me.z7087.blockminer.config.Config;
import me.z7087.blockminer.task.TaskManager;
import me.z7087.blockminer.util.BlockBreakUtils;
import me.z7087.blockminer.util.RotationUtils;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;

import java.io.IOException;

public final class BlockMinerMod implements ClientModInitializer {
    public static final String MOD_ID = "blockminer";
    public static final String HELLO_MESSAGE = "fabric:" + MOD_ID + ":hello";
    //#if MC >= 11802
    public static final org.slf4j.Logger LOGGER;
    //#else
    //$$ public static final org.apache.logging.log4j.Logger LOGGER;
    //#endif
    static {
        //#if MC >= 11802
        LOGGER = com.mojang.logging.LogUtils.getLogger();
        //#else
        //$$ LOGGER = org.apache.logging.log4j.LogManager.getLogger();
        //#endif
    }
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
            Class<?> ignored = ClientCommandManager.class;
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
