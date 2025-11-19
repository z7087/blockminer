package me.z7087.blockminer.config;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;
import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.api.base.BaseConfig;
import me.z7087.blockminer.api.enums.DistanceCalculationMode;
import me.z7087.blockminer.api.enums.PowerBlockType;
import me.z7087.blockminer.api.enums.SearchMode;
import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;
import me.z7087.final2constant.util.JavaHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public abstract class Config extends BaseConfig {
    public static final File PATH_CONFIG = new File(FabricLoader.getInstance().getConfigDir().toFile(), BlockMinerMod.MOD_ID + ".json");
    public static final Gson GSON;
    static {
        GSON = new GsonBuilder()
                .registerTypeAdapter(BaseConfig.class, new ConfigTypeAdapter())
                .setPrettyPrinting()
                .create();
    }
    private Config() {}

    private static final MethodHandle CONSTRUCTOR;
    static {
        final String[] immutableNames, immutableDescriptors;
        try {
            Config configEmptyImpl = Constant.factory.ofEmptyAbstractImplInstance(
                    MethodHandles.lookup(),
                    Config.class
            );
            final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                    MethodHandles.lookup(),
                    (Supplier<DynamicConstant<Boolean>> & Serializable) configEmptyImpl::debug,
                    (Supplier<DynamicConstant<Boolean>> & Serializable) configEmptyImpl::headlessPistonMode,
                    (Supplier<DynamicConstant<Boolean>> & Serializable) configEmptyImpl::blinkDuringTasksTick,
                    (Supplier<DynamicConstant<Boolean>> & Serializable) configEmptyImpl::autoClearAfterTask,
                    (Supplier<DynamicConstant<Integer>> & Serializable) configEmptyImpl::pingSpikeThreshold,
                    (Supplier<DynamicConstant<PowerBlockType>> & Serializable) configEmptyImpl::powerBlockUsage,
                    (Supplier<DynamicConstant<DistanceCalculationMode>> & Serializable) configEmptyImpl::distanceCalculationMode,
                    (Supplier<DynamicConstant<SearchMode>> & Serializable) configEmptyImpl::searchMode,
                    (Supplier<Set<Block>> & Serializable) configEmptyImpl::blockWhitelist,
                    (Supplier<Set<Block>> & Serializable) configEmptyImpl::dependBlockWhitelist,
                    (Supplier<Set<Item>> & Serializable) configEmptyImpl::dependBlockItemWhitelist
            );
            immutableNames = immutableNamesAndDescriptors[0];
            immutableDescriptors = immutableNamesAndDescriptors[1];
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        CONSTRUCTOR = Constant.factory.ofRecordConstructor(
                MethodHandles.lookup(),
                Config.class,
                false,
                immutableNames,
                immutableDescriptors,
                null,
                null,
                true,
                false
        );
    }

    public static Config createInstance() {
        final DynamicConstant<Boolean> debug = Constant.factory.ofMutable(false);
        final DynamicConstant<Boolean> headlessPistonMode = Constant.factory.ofMutable(false);
        final DynamicConstant<Boolean> blinkDuringTasksTick = Constant.factory.ofMutable(false);
        final DynamicConstant<Boolean> autoClearAfterTask = Constant.factory.ofMutable(true);
        final DynamicConstant<Integer> pingSpikeThreshold = Constant.factory.ofMutable(0);
        final DynamicConstant<PowerBlockType> powerBlockUsage = Constant.factory.ofMutable(PowerBlockType.Both);
        final DynamicConstant<DistanceCalculationMode> distanceCalculationMode = Constant.factory.ofMutable(DistanceCalculationMode.currentClientVersion);
        final DynamicConstant<SearchMode> searchMode = Constant.factory.ofMutable(SearchMode.All);
        final Set<Block> blockWhitelist = new HashSet<>();

        final Set<Block> dependBlockWhitelist = new HashSet<>();
        final Set<Item> dependBlockItemWhitelist = new HashSet<>();
        try {
            return (Config) CONSTRUCTOR.invokeExact(
                    debug,
                    headlessPistonMode,
                    blinkDuringTasksTick,
                    autoClearAfterTask,
                    pingSpikeThreshold,
                    powerBlockUsage,
                    distanceCalculationMode,
                    searchMode,
                    blockWhitelist,
                    dependBlockWhitelist,
                    dependBlockItemWhitelist
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Config createDefaultConfig() {
        final Config config = Config.createInstance();
        config.blockWhitelist().addAll(getDefaultBlockWhitelist());
        for (Block block : getDefaultDependBlockWhitelist()) {
            config.dependBlockWhitelistAdd(block);
        }
        return config;
    }

    private static void mkdirs() {
        File parent = PATH_CONFIG.getParentFile();
        if (!parent.exists() || !parent.isDirectory())
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
    }

    public static Config loadFromFile() {
        mkdirs();
        if (!PATH_CONFIG.exists() || !PATH_CONFIG.isFile()) {
            return null;
        }
        Reader reader = null;
        //noinspection TryFinallyCanBeTryWithResources
        try {
            reader = new FileReader(PATH_CONFIG);
            return GSON.fromJson(reader, Config.class);
        } catch (FileNotFoundException ignored) {
            return null;
        } catch (Exception e) {
            BlockMinerMod.LOGGER.warn("{}", e.getMessage(), e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }

    }

    public static void saveToFile(BaseConfig config) throws IOException {
        mkdirs();
        Writer writer = null;
        //noinspection TryFinallyCanBeTryWithResources
        try {
            writer = new FileWriter(PATH_CONFIG);
            GSON.toJson(config, BaseConfig.class, writer);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static Set<Block> getDefaultBlockWhitelist() {
        final Set<Block> blocks = new HashSet<>();
        blocks.add(Blocks.BEDROCK);
        return blocks;
    }

    public static Set<Block> getDefaultDependBlockWhitelist() {
        final Set<Block> blocks = new HashSet<>();
        blocks.add(Blocks.SLIME_BLOCK);
        return blocks;
    }

    static Identifier identifierOf(String id) {
        //#if MC >= 12100
        return Identifier.of(id);
        //#else
        //$$ return new Identifier(id);
        //#endif
    }

    private static final class ConfigTypeAdapter extends TypeAdapter<BaseConfig> {
        @Override
        public void write(JsonWriter out, BaseConfig config) throws IOException {
            out.beginObject();
            out.name("debug").value(config.isDebug());
            out.name("headless-piston-mode").value(config.isHeadlessPistonMode());
            out.name("blink-during-tasks-tick").value(config.isBlinkDuringTasksTick());
            out.name("auto-clear-after-task").value(config.isAutoClearAfterTask());
            out.name("ping-spike-threshold").value(config.getPingSpikeThreshold());
            out.name("power-block-usage").value(config.getPowerBlockUsage().toString());
            out.name("distance-calculation-mode").value(config.getDistanceCalculationMode().toString());
            out.name("search-mode").value(config.getSearchMode().toString());
            final Identifier defaultId = Registries.BLOCK.getDefaultId();
            {
                out.name("whitelist").beginArray();
                for (Block block : config.blockWhitelist()) {
                    Identifier id = Registries.BLOCK.getId(block);
                    if (id != defaultId) {
                        out.value(id.toString());
                    }
                }
                out.endArray();
            }
            {
                out.name("depend-block-whitelist").beginArray();
                for (Block block : config.dependBlockWhitelist()) {
                    Identifier id = Registries.BLOCK.getId(block);
                    if (id != defaultId) {
                        out.value(id.toString());
                    }
                }
                out.endArray();
            }
            out.endObject();
        }

        @Override
        public Config read(JsonReader in) throws IOException {
            final Config config = Config.createInstance();
            try {
                in.beginObject();
                while (in.hasNext()) {
                    String name = in.nextName();
                    switch (name) {
                        case "debug": {
                            config.setDebug(in.nextBoolean());
                            break;
                        }
                        case "headless-piston-mode": {
                            config.setHeadlessPistonMode(in.nextBoolean());
                            break;
                        }
                        case "blink-during-tasks-tick": {
                            config.setBlinkDuringTasksTick(in.nextBoolean());
                            break;
                        }
                        case "auto-clear-after-task": {
                            config.setAutoClearAfterTask(in.nextBoolean());
                            break;
                        }
                        case "ping-spike-threshold": {
                            config.setPingSpikeThreshold(in.nextInt());
                            break;
                        }
                        case "power-block-usage": {
                            config.setPowerBlockUsage(PowerBlockType.of(in.nextString()));
                            break;
                        }
                        case "distance-calculation-mode": {
                            config.setDistanceCalculationMode(DistanceCalculationMode.of(in.nextString()));
                            break;
                        }
                        case "search-mode": {
                            config.setSearchMode(SearchMode.of(in.nextString()));
                            break;
                        }
                        case "whitelist": {
                            in.beginArray();
                            while (in.hasNext()) {
                                final String id = in.nextString();
                                Block block = Registries.BLOCK.get(identifierOf(id));
                                if (block != Blocks.AIR) {
                                    if (!config.blockWhitelist().add(block)) {
                                        BlockMinerMod.LOGGER.debug("Duplicate block during config loading: {}", id);
                                    }
                                } else {
                                    BlockMinerMod.LOGGER.debug("Block identifier not found during config loading, ignored: {}", id);
                                }
                            }
                            in.endArray();
                            break;
                        }
                        case "depend-block-whitelist": {
                            in.beginArray();
                            while (in.hasNext()) {
                                final String id = in.nextString();
                                Block block = Registries.BLOCK.get(identifierOf(id));
                                if (block != Blocks.AIR) {
                                    if (!config.dependBlockWhitelistAdd(block)) {
                                        BlockMinerMod.LOGGER.debug("Duplicate block during config loading: {}", id);
                                    }
                                } else {
                                    BlockMinerMod.LOGGER.debug("Block identifier not found during config loading, ignored: {}", id);
                                }
                            }
                            in.endArray();
                            break;
                        }
                        default: {
                            in.skipValue();
                        }
                    }
                }
                in.endObject();
            } catch (MalformedJsonException | JsonParseException e) {
                throw new MalformedConfigException(e);
            }
            return config;
        }
    }

    private static final class MalformedConfigException extends RuntimeException {
        public MalformedConfigException() {
        }

        public MalformedConfigException(String message) {
            super(message);
        }

        public MalformedConfigException(String message, Throwable cause) {
            super(message, cause);
        }

        public MalformedConfigException(Throwable cause) {
            super(cause);
        }

        public MalformedConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
