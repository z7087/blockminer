package me.z7087.blockminer;

import me.z7087.blockminer.api.base.BaseConfig;
import me.z7087.blockminer.api.base.IBlockMinerMod;
import me.z7087.blockminer.command.Command;
import me.z7087.blockminer.config.Config;
import me.z7087.blockminer.task.TaskManager;
import me.z7087.blockminer.util.BlockBreakUtils;
import me.z7087.blockminer.util.InventoryUtils;
import me.z7087.blockminer.util.RotationUtils;
import me.z7087.blockminer.util.finder.BlockFinder;
import me.z7087.blockminer.util.finder.NoHorizontalBlockFinder;
import me.z7087.blockminer.util.finder.NoHorizontalSimpleBlockFinder;
import me.z7087.blockminer.util.finder.SimpleBlockFinder;
import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;
import me.z7087.final2constant.util.JavaHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Function;

public final class BlockMinerMod implements IBlockMinerMod, ClientModInitializer {
    public interface ModConstants {
        DynamicConstant<BaseConfig> config();

        TaskManager taskManager();

        BlockBreakUtils blockBreakUtils();

        RotationUtils rotationUtils();
    }
    public static abstract class StableConstants {
        private StableConstants() {}

        private static final MethodHandle CONSTRUCTOR;
        static {
            final String[] immutableNames, immutableDescriptors;
            try {
                final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                        MethodHandles.lookup(),
                        (Function<StableConstants, DynamicConstant<MinecraftClient>> & Serializable) StableConstants::minecraftClient
                );
                immutableNames = immutableNamesAndDescriptors[0];
                immutableDescriptors = immutableNamesAndDescriptors[1];
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            CONSTRUCTOR = Constant.factory.ofRecordConstructor(
                    MethodHandles.lookup(),
                    StableConstants.class,
                    false,
                    immutableNames,
                    immutableDescriptors,
                    null,
                    null,
                    true,
                    false
            );
        }

        public static StableConstants createInstance() {
            try {
                return (StableConstants) CONSTRUCTOR.invokeExact(
                        Constant.factory.ofMutable(null)
                );
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        abstract DynamicConstant<MinecraftClient> minecraftClient();

        public final @NotNull MinecraftClient mc() {
            return minecraftClient().orElseThrow();
        }

        public final void update() {
            updateMinecraftClient();
        }

        private void updateMinecraftClient() {
            final DynamicConstant<MinecraftClient> minecraftClientDC = minecraftClient();
            if (minecraftClientDC.isEmpty()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    minecraftClientDC.set(mc);
                    minecraftClientDC.sync();
                }
            }
        }
    }
    public static abstract class TicklyUpdateConstants {
        private TicklyUpdateConstants() {}

        private static final MethodHandle CONSTRUCTOR;
        static {
            final String[] immutableNames, immutableDescriptors;
            try {
                final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                        MethodHandles.lookup(),
                        (Function<TicklyUpdateConstants, DynamicConstant<ClientPlayerEntity>> & Serializable) TicklyUpdateConstants::clientPlayerEntity,
                        (Function<TicklyUpdateConstants, DynamicConstant<PlayerInventory>> & Serializable) TicklyUpdateConstants::clientPlayerInventory,
                        (Function<TicklyUpdateConstants, DynamicConstant<PlayerScreenHandler>> & Serializable) TicklyUpdateConstants::clientPlayerScreenHandler,
                        (Function<TicklyUpdateConstants, DynamicConstant<ClientPlayerInteractionManager>> & Serializable) TicklyUpdateConstants::clientPlayerInteractionManager,
                        (Function<TicklyUpdateConstants, DynamicConstant<ClientWorld>> & Serializable) TicklyUpdateConstants::clientWorld
                );
                immutableNames = immutableNamesAndDescriptors[0];
                immutableDescriptors = immutableNamesAndDescriptors[1];
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            CONSTRUCTOR = Constant.factory.ofRecordConstructor(
                    MethodHandles.lookup(),
                    TicklyUpdateConstants.class,
                    false,
                    immutableNames,
                    immutableDescriptors,
                    null,
                    null,
                    true,
                    false
            );
        }

        public static TicklyUpdateConstants createInstance() {
            try {
                return (TicklyUpdateConstants) CONSTRUCTOR.invokeExact(
                        Constant.factory.ofMutable(null),
                        Constant.factory.ofMutable(null),
                        Constant.factory.ofMutable(null),
                        Constant.factory.ofMutable(null),
                        Constant.factory.ofMutable(null)
                );
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        abstract DynamicConstant<ClientPlayerEntity> clientPlayerEntity();
        abstract DynamicConstant<PlayerInventory> clientPlayerInventory();
        abstract DynamicConstant<PlayerScreenHandler> clientPlayerScreenHandler();

        abstract DynamicConstant<ClientPlayerInteractionManager> clientPlayerInteractionManager();

        abstract DynamicConstant<ClientWorld> clientWorld();

        public final ClientPlayerEntity player() {
            return clientPlayerEntity().get();
        }

        public final PlayerInventory inventory() {
            return clientPlayerInventory().get();
        }

        public final PlayerScreenHandler playerScreenHandler() {
            return clientPlayerScreenHandler().get();
        }

        public final ClientPlayerInteractionManager interactionManager() {
            return clientPlayerInteractionManager().get();
        }

        public final ClientWorld world() {
            return clientWorld().get();
        }

        public final void tick(MinecraftClient mc) {
            if (mc == null) return;
            tickClientPlayerEntity(mc);
            tickClientPlayerInteractionManager(mc);
            tickClientWorld(mc);
        }

        private void tickClientPlayerEntity(@NotNull MinecraftClient mc) {
            final DynamicConstant<ClientPlayerEntity> clientPlayerEntityDC = clientPlayerEntity();
            final ClientPlayerEntity clientPlayerEntity = mc.player;
            if (clientPlayerEntity != clientPlayerEntityDC.get()) {
                clientPlayerEntityDC.set(clientPlayerEntity);
                if (clientPlayerEntity != null) {
                    updateClientPlayerInventoryScreenHandler(clientPlayerEntity);
                } else {
                    clearClientPlayerInventoryScreenHandler();
                }
            }
        }

        private void updateClientPlayerInventoryScreenHandler(ClientPlayerEntity clientPlayerEntity) {
            {
                final DynamicConstant<PlayerInventory> clientPlayerInventoryDC = clientPlayerInventory();
                final PlayerInventory inventory = InventoryUtils.getInventory(clientPlayerEntity);
                if (inventory != clientPlayerInventoryDC.get()) {
                    clientPlayerInventoryDC.set(inventory);
                }
            }
            {
                final DynamicConstant<PlayerScreenHandler> clientPlayerScreenHandlerDC = clientPlayerScreenHandler();
                final PlayerScreenHandler playerScreenHandler = clientPlayerEntity.playerScreenHandler;
                if (playerScreenHandler != clientPlayerScreenHandlerDC.get()) {
                    clientPlayerScreenHandlerDC.set(playerScreenHandler);
                }
            }
        }

        private void clearClientPlayerInventoryScreenHandler() {
            clientPlayerInventory().set(null);
            clientPlayerScreenHandler().set(null);
        }

        private void tickClientPlayerInteractionManager(@NotNull MinecraftClient mc) {
            final DynamicConstant<ClientPlayerInteractionManager> clientPlayerInteractionManagerDC = clientPlayerInteractionManager();
            final ClientPlayerInteractionManager clientPlayerInteractionManager = mc.interactionManager;
            if (clientPlayerInteractionManager != clientPlayerInteractionManagerDC.get()) {
                clientPlayerInteractionManagerDC.set(clientPlayerInteractionManager);
            }
        }

        private void tickClientWorld(@NotNull MinecraftClient mc) {
            final DynamicConstant<ClientWorld> clientWorldDC = clientWorld();
            final ClientWorld clientWorld = mc.world;
            if (clientWorld != clientWorldDC.get()) {
                clientWorldDC.set(clientWorld);
            }
        }
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
    private static final DynamicConstant<StableConstants> STABLE_CONSTANTS = Constant.factory.ofMutable(null);
    private static final DynamicConstant<TicklyUpdateConstants> TICKLY_UPDATE_CONSTANTS = Constant.factory.ofMutable(null);
    private static final MethodHandle MOD_CONSTANTS_CONSTRUCTOR;
    static {
        final String[] immutableNames, immutableDescriptors;
        try {
            final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                    MethodHandles.lookup(),
                    (Function<ModConstants, DynamicConstant<BaseConfig>> & Serializable) ModConstants::config,
                    (Function<ModConstants, TaskManager> & Serializable) ModConstants::taskManager,
                    (Function<ModConstants, BlockBreakUtils> & Serializable) ModConstants::blockBreakUtils,
                    (Function<ModConstants, RotationUtils> & Serializable) ModConstants::rotationUtils
            );
            immutableNames = immutableNamesAndDescriptors[0];
            immutableDescriptors = immutableNamesAndDescriptors[1];
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        MOD_CONSTANTS_CONSTRUCTOR = Constant.factory.ofRecordConstructor(
                MethodHandles.lookup(),
                ModConstants.class,
                immutableNames,
                immutableDescriptors
        );
    }

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
        STABLE_CONSTANTS.set(StableConstants.createInstance());
        STABLE_CONSTANTS.sync();
        TICKLY_UPDATE_CONSTANTS.set(TicklyUpdateConstants.createInstance());
        TICKLY_UPDATE_CONSTANTS.sync();
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
        loadClassesBackground();
    }

    private void loadClassesBackground() {
        Thread thread = new Thread(() -> {
            ClassLoader loader = BlockMinerMod.class.getClassLoader();
            ensureClassInitialized(loader, BlockFinder.StructureFilterCache.class);
            ensureClassInitialized(loader, SimpleBlockFinder.StructureFilterCache.class);
            ensureClassInitialized(loader, NoHorizontalBlockFinder.StructureFilterCache.class);
            ensureClassInitialized(loader, NoHorizontalSimpleBlockFinder.StructureFilterCache.class);
        }, "BlockMinerMod Classes Background Loader");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void ensureClassInitialized(ClassLoader loader, Class<?> cls) {
        try {
            Class.forName(cls.getName(), true, loader);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class not found during ensureClassInitialized: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (LinkageError e) {
            LOGGER.error("Class loading failed during ensureClassInitialized: {}", e.getMessage(), e);
            throw e;
        }
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

    @Override
    public BaseConfig getConfig() {
        return MOD_CONSTANTS.orElseThrow().config().orElseThrow();
    }

    @Override
    public void setConfig(BaseConfig config) {
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

    public StableConstants stableConstants() {
        return STABLE_CONSTANTS.orElseThrow();
    }

    public TicklyUpdateConstants ticklyUpdateConstants() {
        return TICKLY_UPDATE_CONSTANTS.orElseThrow();
    }
}
