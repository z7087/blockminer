package me.z7087.blockminer.api.base;

import me.z7087.blockminer.BlockMinerMod;
import net.fabricmc.api.ClientModInitializer;

public interface IBlockMinerMod extends ClientModInitializer {
    static IBlockMinerMod getInstance() {
        return BlockMinerMod.getInstance();
    }

    BaseConfig getConfig();

    void setConfig(BaseConfig config);
}
