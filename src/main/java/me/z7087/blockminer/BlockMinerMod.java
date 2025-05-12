package me.z7087.blockminer;

import me.z7087.blockminer.command.Command;
import me.z7087.blockminer.config.Config;
import me.z7087.blockminer.task.TaskManager;
import me.z7087.blockminer.util.BlockBreakUtils;
import me.z7087.blockminer.util.RotationUtils;
import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public final class BlockMinerMod implements ClientModInitializer {
    public interface ModConstants {
        DynamicConstant<Config> config();

        TaskManager taskManager();

        BlockBreakUtils blockBreakUtils();

        RotationUtils rotationUtils();
    }

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
    private static final DynamicConstant<BlockMinerMod> INSTANCE = Constant.factory.ofMutable(null);
    private static final DynamicConstant<ModConstants> MOD_CONSTANTS = Constant.factory.ofMutable(null);
    private static final MethodHandle MOD_CONSTANTS_CONSTRUCTOR = Constant.factory.ofRecordConstructor(
            MethodHandles.lookup(),
            ModConstants.class,
            new String[] {
                    "config",
                    "taskManager",
                    "blockBreakUtils",
                    "rotationUtils"
            },
            new String[] {
                    Type.getDescriptor(DynamicConstant.class),
                    Type.getDescriptor(TaskManager.class),
                    Type.getDescriptor(BlockBreakUtils.class),
                    Type.getDescriptor(RotationUtils.class)
            });

    public BlockMinerMod() {
        if (getInstanceOrNull() != null)
            throw new AssertionError("getInstance() != null");
        INSTANCE.set(this);
        INSTANCE.sync();

        try {
            MOD_CONSTANTS.set((ModConstants) MOD_CONSTANTS_CONSTRUCTOR.invokeExact(
                    Constant.factory.ofMutable(Config.createDefaultConfig()),
                    TaskManager.createInstance(),
                    BlockBreakUtils.createInstance(),
                    RotationUtils.createInstance()
            ));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        MOD_CONSTANTS.sync();
    }

    @Override
    public void onInitializeClient() {
        Config tconfig = Config.loadFromFile();
        if (tconfig != null)
            setConfig(tconfig);
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
            Config.saveToFile(getConfig());
        } catch (IOException e) {
            LOGGER.error("config saving failed: {}", e.getMessage(), e);
        }
    }

    public static BlockMinerMod getInstance() {
        return INSTANCE.orElseThrow();
    }

    public Config getConfig() {
        return MOD_CONSTANTS.orElseThrow().config().orElseThrow();
    }

    public void setConfig(Config config) {
        MOD_CONSTANTS.orElseThrow().config().set(config);
        MOD_CONSTANTS.orElseThrow().config().sync();
    }

    public TaskManager getTaskManager() {
        return MOD_CONSTANTS.orElseThrow().taskManager();
    }

    public BlockBreakUtils getBlockBreakUtils() {
        return MOD_CONSTANTS.orElseThrow().blockBreakUtils();
    }

    public RotationUtils getRotationUtils() {
        return MOD_CONSTANTS.orElseThrow().rotationUtils();
    }

    private static BlockMinerMod getInstanceOrNull() {
        return INSTANCE.get();
    }
}
