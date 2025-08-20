package me.z7087.blockminer.util.data;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.Iterator;

public final class BlockBoxHelper {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public BlockBoxHelper(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockBoxHelper(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public boolean intersects(BlockBoxHelper other) {
        return this.maxX >= other.minX
                && this.minX <= other.maxX
                && this.maxZ >= other.minZ
                && this.minZ <= other.maxZ
                && this.maxY >= other.minY
                && this.minY <= other.maxY;
    }

    public boolean contains(Vec3i pos) {
        return this.contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ && y >= this.minY && y <= this.maxY;
    }

    public static BlockBoxHelper encompassPositionsAt000(Iterable<BlockPos> positions) {
        final Iterator<BlockPos> iterator = positions.iterator();
        if (!iterator.hasNext())
            throw new IllegalArgumentException("positions iterator has 0 elements");
        int minX = 0, minY = 0, minZ = 0;
        int maxX = 0, maxY = 0, maxZ = 0;
        do {
            final BlockPos pos = iterator.next();
            final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        } while (iterator.hasNext());
        return new BlockBoxHelper(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
