package me.z7087.blockminer.api.base;

import me.z7087.blockminer.api.enums.DistanceCalculationMode;
import me.z7087.blockminer.api.enums.PowerBlockType;
import me.z7087.blockminer.api.enums.SearchMode;
import me.z7087.final2constant.DynamicConstant;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Set;

public abstract class BaseConfig {
    protected abstract DynamicConstant<Boolean> debug();
    protected abstract DynamicConstant<Boolean> headlessPistonMode();
    protected abstract DynamicConstant<Boolean> blinkDuringTasksTick();
    protected abstract DynamicConstant<Boolean> autoClearAfterTask();
    protected abstract DynamicConstant<Integer> pingSpikeThreshold();
    protected abstract DynamicConstant<PowerBlockType> powerBlockUsage();
    protected abstract DynamicConstant<DistanceCalculationMode> distanceCalculationMode();
    protected abstract DynamicConstant<SearchMode> searchMode();

    public abstract Set<Block> blockWhitelist();

    public abstract Set<Block> dependBlockWhitelist();

    public abstract Set<Item> dependBlockItemWhitelist();

    public String dependBlockWhitelistToString() {
        return dependBlockWhitelist().toString();
    }

    public boolean dependBlockWhitelistContains(Block block) {
        return dependBlockWhitelist().contains(block);
    }

    public boolean dependBlockWhitelistContains(Item item) {
        return dependBlockItemWhitelist().contains(item);
    }

    public boolean dependBlockWhitelistAdd(Block block) {
        boolean result = dependBlockWhitelist().add(block);
        if (result) {
            Item item = block.asItem();
            if (item != Items.AIR) {
                dependBlockItemWhitelist().add(item);
            }
        }
        return result;
    }

    public boolean dependBlockWhitelistRemove(Block block) {
        boolean result = dependBlockWhitelist().remove(block);
        if (result) {
            Item item = block.asItem();
            if (item != Items.AIR) {
                dependBlockItemWhitelist().remove(item);
            }
        }
        return result;
    }

    public boolean isDebug() {
        return debug().orElseThrow();
    }

    public void setDebug(boolean value) {
        debug().set(value);
        debug().sync();
    }

    public boolean isHeadlessPistonMode() {
        return headlessPistonMode().orElseThrow();
    }

    public void setHeadlessPistonMode(boolean value) {
        headlessPistonMode().set(value);
        headlessPistonMode().sync();
    }

    public boolean isBlinkDuringTasksTick() {
        return blinkDuringTasksTick().orElseThrow();
    }

    public void setBlinkDuringTasksTick(boolean value) {
        blinkDuringTasksTick().set(value);
        blinkDuringTasksTick().sync();
    }

    public boolean isAutoClearAfterTask() {
        return autoClearAfterTask().orElseThrow();
    }

    public void setAutoClearAfterTask(boolean value) {
        autoClearAfterTask().set(value);
        autoClearAfterTask().sync();
    }

    public int getPingSpikeThreshold() {
        return pingSpikeThreshold().orElseThrow();
    }

    public void setPingSpikeThreshold(int value) {
        pingSpikeThreshold().set(value);
        pingSpikeThreshold().sync();
    }

    public PowerBlockType getPowerBlockUsage() {
        return powerBlockUsage().orElseThrow();
    }

    public void setPowerBlockUsage(PowerBlockType value) {
        powerBlockUsage().set(value);
        powerBlockUsage().sync();
    }

    public DistanceCalculationMode getDistanceCalculationMode() {
        return distanceCalculationMode().orElseThrow();
    }

    public void setDistanceCalculationMode(DistanceCalculationMode value) {
        distanceCalculationMode().set(value);
        distanceCalculationMode().sync();
    }

    public SearchMode getSearchMode() {
        return searchMode().orElseThrow();
    }

    public void setSearchMode(SearchMode value) {
        searchMode().set(value);
        searchMode().sync();
    }
}
