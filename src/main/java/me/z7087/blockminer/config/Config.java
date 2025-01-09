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
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.HashSet;
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
    public boolean headlessPistonMode = false;
    public boolean hello = true;
    public int pingSpikeThreshold = 0;
    public PowerBlockType powerBlockUsage = PowerBlockType.Both;
    public final Set<Block> blockWhitelist = new HashSet<>();

    private final Set<Block> dependBlockWhitelist = new HashSet<>();
    private final transient Set<Item> dependBlockItemWhitelist = new HashSet<>();

    public String dependBlockWhitelistToString() {
        return dependBlockWhitelist.toString();
    }

    public boolean dependBlockWhitelistContains(Block block) {
        return dependBlockWhitelist.contains(block);
    }

    public boolean dependBlockWhitelistContains(Item item) {
        return dependBlockItemWhitelist.contains(item);
    }

    public boolean dependBlockWhitelistAdd(Block block) {
        boolean result = dependBlockWhitelist.add(block);
        if (result) {
            Item item = block.asItem();
            if (item != Items.AIR) {
                dependBlockItemWhitelist.add(item);
            }
        }
        return result;
    }

    public boolean dependBlockWhitelistRemove(Block block) {
        boolean result = dependBlockWhitelist.remove(block);
        if (result) {
            Item item = block.asItem();
            if (item != Items.AIR) {
                dependBlockItemWhitelist.remove(item);
            }
        }
        return result;
    }

    public static Config createDefaultConfig() {
        final Config config = new Config();
        config.blockWhitelist.addAll(getDefaultBlockWhitelist());
        for (Block block : getDefaultDependBlockWhitelist()) {
            config.dependBlockWhitelistAdd(block);
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

    static Identifier identifierOf(String id) {
        //#if MC >= 12100
        return Identifier.of(id);
        //#else
        //$$ return new Identifier(id);
        //#endif
    }

    private static final class ConfigTypeAdapter extends TypeAdapter<Config> {
        @Override
        public void write(JsonWriter out, Config config) throws IOException {
            out.beginObject();
            out.name("debug").value(config.debug);
            out.name("headless-piston-mode").value(config.headlessPistonMode);
            if (!config.hello)
                out.name("hello").value(config.hello);
            out.name("ping-spike-threshold").value(config.pingSpikeThreshold);
            out.name("power-block-usage").value(config.powerBlockUsage.toString());
            final Identifier defaultId = Registries.BLOCK.getDefaultId();
            {
                out.name("whitelist").beginArray();
                for (Block block : config.blockWhitelist) {
                    Identifier id = Registries.BLOCK.getId(block);
                    if (id != defaultId) {
                        out.value(id.toString());
                    }
                }
                out.endArray();
            }
            {
                out.name("depend-block-whitelist").beginArray();
                for (Block block : config.dependBlockWhitelist) {
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
                        case "headless-piston-mode": {
                            config.headlessPistonMode = in.nextBoolean();
                            break;
                        }
                        case "hello": {
                            config.hello = in.nextBoolean();
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
                                Block block = Registries.BLOCK.get(identifierOf(id));
                                if (block != Blocks.AIR) {
                                    if (!config.blockWhitelist.add(block)) {
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
