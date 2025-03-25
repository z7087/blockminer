package me.z7087.blockminer;

import me.z7087.blockminer.command.Command;
import me.z7087.blockminer.config.Config;
import me.z7087.blockminer.task.TaskManager;
import me.z7087.blockminer.util.BlockBreakUtils;
import me.z7087.blockminer.util.RotationUtils;
import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;

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
    private static final MutableCallSite INSTANCE_CONSTANT = new MutableCallSite(MethodHandles.constant(BlockMinerMod.class, null));
    private static final MethodHandle INSTANCE_GETTER = INSTANCE_CONSTANT.dynamicInvoker();

    public Config config = Config.createDefaultConfig();
    public final TaskManager taskManager = new TaskManager();
    public final BlockBreakUtils blockBreakUtils = new BlockBreakUtils();
    public final RotationUtils rotationUtils = new RotationUtils();

    public BlockMinerMod() {
        if (getInstance() != null)
            throw new AssertionError("getInstance() != null");
        INSTANCE_CONSTANT.setTarget(MethodHandles.constant(BlockMinerMod.class, this));
        MutableCallSite.syncAll(new MutableCallSite[]{
                INSTANCE_CONSTANT
        });
    }

    public static BlockMinerMod getInstance() {
        try {
            return (BlockMinerMod) INSTANCE_GETTER.invokeExact();
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
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
