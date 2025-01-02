package me.z7087.blockminer;

import me.z7087.blockminer.config.Config;
import me.z7087.blockminer.task.TaskManager;
import me.z7087.blockminer.util.RotationUtils;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlockMinerMod implements ClientModInitializer {
    public static final String MOD_ID = "blockminer";
    public static final Logger LOGGER = LoggerFactory.getLogger(BlockMinerMod.class);
    public static BlockMinerMod INSTANCE;
    public Config config = Config.createDefaultConfig();
    public final TaskManager taskManager = new TaskManager();
    public final RotationUtils rotationUtils = new RotationUtils();

    public BlockMinerMod() {
        INSTANCE = this;
    }

    @Override
    public void onInitializeClient() {
        Config tconfig = Config.loadFromFile();
        if (tconfig != null)
            config = tconfig;
    }
}
