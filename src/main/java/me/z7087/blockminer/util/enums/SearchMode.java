package me.z7087.blockminer.util.enums;

import me.z7087.blockminer.util.data.BlockBreakStructureFull;
import me.z7087.blockminer.util.finder.BlockFinder;
import me.z7087.blockminer.util.finder.SimpleBlockFinder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.stream.Stream;

public enum SearchMode {
    All("all"),
    Simple("simple");

    private final String name;

    SearchMode(String name) {
        this.name = name;
    }

    public static SearchMode of(String name) {
        if (name == null)
            throw new NullPointerException("Name is null");
        switch (name) {
            case "all":
                return All;
            case "simple":
                return Simple;
        }
        throw new IllegalArgumentException(
                "No enum constant " + SearchMode.class.getCanonicalName() + "." + name);
    }

    public Stream<BlockBreakStructureFull> findPossibleStructures(
            World world,
            BlockPos targetPos,
            PowerBlockType powerBlockUsage,
            boolean hasDependBlock
    ) {
        switch (this) {
            case All:
                return BlockFinder.StructureFilterCache.findPossibleFullStructuresInCache(world, targetPos, powerBlockUsage, hasDependBlock);
            case Simple:
                return SimpleBlockFinder.StructureFilterCache.findPossibleFullStructuresInCache(world, targetPos, powerBlockUsage, hasDependBlock);
        }
        throw new AssertionError();
    }

    @Override
    public String toString() {
        return name;
    }
}
