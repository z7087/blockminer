package me.z7087.blockminer.util.data;

import me.z7087.blockminer.util.enums.PowerBlockType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;

@Deprecated
public final class PistonPowerInfo {
    public final BlockPos pistonPos;
    public final Direction pistonFace;
    public final BlockPos powerBlockPos;
    public final Direction powerBlockFace;
    private final PowerBlockType powerBlockType;

    public PowerBlockType getPowerBlockType() {
        return powerBlockType;
    }

    public PistonPowerInfo(BlockPos pistonPos, Direction pistonFace, BlockPos powerBlockPos, Direction powerBlockFace, PowerBlockType powerBlockType) {
        this.pistonPos = Objects.requireNonNull(pistonPos);
        this.pistonFace = Objects.requireNonNull(pistonFace);
        this.powerBlockPos = Objects.requireNonNull(powerBlockPos);
        this.powerBlockFace = Objects.requireNonNull(powerBlockFace);
        this.powerBlockType = powerBlockType;
    }

    public static PistonPowerInfo of(BlockPos pistonPos, Direction pistonFace, BlockPos powerBlockPos, Direction powerBlockFace, PowerBlockType powerBlockType) {
        return new PistonPowerInfo(pistonPos, pistonFace, powerBlockPos, powerBlockFace, powerBlockType);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PistonPowerInfo) {
            PistonPowerInfo that = (PistonPowerInfo) o;
            return pistonPos.equals(that.pistonPos)
                    && pistonFace == that.pistonFace
                    && powerBlockPos.equals(that.powerBlockPos)
                    && powerBlockFace == that.powerBlockFace;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = pistonPos.hashCode();
        result = 31 * result + pistonFace.hashCode();
        result = 31 * result + powerBlockPos.hashCode();
        result = 31 * result + powerBlockFace.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PistonPowerInfo{" +
                "pistonPos=" + pistonPos +
                ", pistonFace=" + pistonFace +
                ", powerBlockPos=" + powerBlockPos +
                ", powerBlockFace=" + powerBlockFace +
                ", powerBlockType=" + powerBlockType +
                '}';
    }
}
