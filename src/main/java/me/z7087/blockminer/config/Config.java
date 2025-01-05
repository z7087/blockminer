package me.z7087.blockminer.config;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;
import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.util.enums.PowerBlockType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class Config {
    public static final File PATH_CONFIG = new File(FabricLoader.getInstance().getConfigDir().toFile(), BlockMinerMod.MOD_ID + ".json");
    public static final Gson GSON;
    static {
        GSON = new GsonBuilder()
                .registerTypeAdapter(Config.class, new ConfigTypeAdapter())
                .setPrettyPrinting()
                .create();
    }
    public boolean debug = false;
    public int pingSpikeThreshold = 0;
    public PowerBlockType powerBlockUsage = PowerBlockType.Both;
    public final Set<Block> blockWhiteList = new HashSet<>();

    private final Set<Block> dependBlockWhiteList = new HashSet<>();
    private final transient Set<Item> dependBlockItemWhiteList = new HashSet<>();

    public String dependBlockWhiteListToString() {
        return dependBlockWhiteList.toString();
    }

    public boolean dependBlockWhiteListContains(Block block) {
        return dependBlockWhiteList.contains(block);
    }

    public boolean dependBlockWhiteListContains(Item item) {
        return dependBlockItemWhiteList.contains(item);
    }

    public boolean dependBlockWhiteListAdd(Block block) {
        boolean result = dependBlockWhiteList.add(block);
        if (result) {
            Item item = block.asItem();
            if (item != Items.AIR) {
                dependBlockItemWhiteList.add(item);
            }
        }
        return result;
    }

    public boolean dependBlockWhiteListRemove(Block block) {
        boolean result = dependBlockWhiteList.remove(block);
        if (result) {
            Item item = block.asItem();
            if (item != Items.AIR) {
                dependBlockItemWhiteList.remove(item);
            }
        }
        return result;
    }

    public static Config createDefaultConfig() {
        final Config config = new Config();
        config.blockWhiteList.addAll(getDefaultBlockWhitelist());
        for (Block block : getDefaultDependBlockWhitelist()) {
            config.dependBlockWhiteListAdd(block);
        }
        return config;
    }

    @SuppressWarnings("SpellCheckingInspection")
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
            BlockMinerMod.LOGGER.warn(null, e);
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

    public static void saveToFile(Config config) throws IOException {
        mkdirs();
        Writer writer = null;
        //noinspection TryFinallyCanBeTryWithResources
        try {
            writer = new FileWriter(PATH_CONFIG);
            GSON.toJson(config, writer);
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

    private static final class ConfigTypeAdapter extends TypeAdapter<Config> {
        @Override
        public void write(JsonWriter out, Config config) throws IOException {
            out.beginObject();
            out.name("debug").value(config.debug);
            out.name("ping-spike-threshold").value(config.pingSpikeThreshold);
            out.name("power-block-usage").value(config.powerBlockUsage.toString());
            final Identifier defaultId = Registries.BLOCK.getDefaultId();
            {
                out.name("whitelist").beginArray();
                for (Block block : config.blockWhiteList) {
                    Identifier id = Registries.BLOCK.getId(block);
                    if (id != defaultId) {
                        out.value(id.toString());
                    }
                }
                out.endArray();
            }
            {
                out.name("dependBlockWhiteList").beginArray();
                for (Block block : config.dependBlockWhiteList) {
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
            final Config config = new Config();
            try {
                in.beginObject();
                while (in.hasNext()) {
                    String name = in.nextName();
                    switch (name) {
                        case "debug": {
                            config.debug = in.nextBoolean();
                            break;
                        }
                        case "ping-spike-threshold": {
                            config.pingSpikeThreshold = in.nextInt();
                            break;
                        }
                        case "power-block-usage": {
                            config.powerBlockUsage = PowerBlockType.of(in.nextString());
                            break;
                        }
                        case "whitelist": {
                            in.beginArray();
                            while (in.hasNext()) {
                                final String id = in.nextString();
                                Optional<Reference<Block>> entry = Registries.BLOCK.getEntry(Identifier.of(id));
                                if (entry.isPresent()) {
                                    Reference<Block> block = entry.get();
                                    if (!config.blockWhiteList.add(block.value())) {
                                        BlockMinerMod.LOGGER.debug("Duplicate block during config loading: {}", id);
                                    }
                                } else {
                                    BlockMinerMod.LOGGER.debug("Block identifier not found during config loading, ignored: {}", id);
                                }
                            }
                            in.endArray();
                            break;
                        }
                        case "dependBlockWhitelist": {
                            in.beginArray();
                            while (in.hasNext()) {
                                final String id = in.nextString();
                                Optional<Reference<Block>> entry = Registries.BLOCK.getEntry(Identifier.of(id));
                                if (entry.isPresent()) {
                                    Reference<Block> block = entry.get();
                                    if (!config.dependBlockWhiteListAdd(block.value())) {
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
