package me.z7087.blockminer.util.data;

import me.z7087.blockminer.util.enums.PowerBlockType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public final class BlockBreakStructureFull {
    private final @NotNull BlockPos targetPos;
    private final @NotNull BlockBreakStructure structure;

    private final transient @NotNull BlockPos pistonPos;
    private final transient @NotNull BlockPos pistonHeadPos;
    private final transient @NotNull BlockPos powerBlockPos;
    private final transient @NotNull BlockPos dependBlockPos;
    private final transient @NotNull BlockPos strongPoweringBlockByPowerBlockPos;

    public BlockBreakStructureFull(@NotNull BlockPos targetPos, @NotNull BlockBreakStructure structure) {
        this.targetPos = targetPos;
        this.structure = structure;

        this.pistonPos = targetPos.add(structure.pistonOffsetPos);
        this.pistonHeadPos = targetPos.add(structure.pistonHeadOffsetPos);
        this.powerBlockPos = targetPos.add(structure.powerBlockOffsetPos);
        this.dependBlockPos = targetPos.add(structure.dependBlockOffsetPos);
        this.strongPoweringBlockByPowerBlockPos = targetPos.add(structure.strongPoweringBlockByPowerBlockOffsetPos);
    }

    public static @NotNull BlockBreakStructureFull of(@NotNull BlockPos targetPos, @NotNull BlockBreakStructure structure) {
        return new BlockBreakStructureFull(targetPos, structure);
    }

    public @NotNull BlockPos getPistonPos() {
        return pistonPos;
    }

    public @NotNull BlockPos getPistonHeadPos() {
        return pistonHeadPos;
    }

    public @NotNull BlockPos getPowerBlockPos() {
        return powerBlockPos;
    }

    public @NotNull BlockPos getDependBlockPos() {
        return dependBlockPos;
    }

    public @NotNull BlockPos getStrongPoweringBlockByPowerBlockPos() {
        return strongPoweringBlockByPowerBlockPos;
    }

    public @NotNull BlockPos getTargetPos() {
        return targetPos;
    }

    public @NotNull Direction getPistonOffset() {
        return structure.pistonOffset;
    }

    public @NotNull Direction getPistonFace() {
        return structure.pistonFace;
    }

    public @NotNull Direction getPowerBlockFace() {
        return structure.powerBlockFace;
    }

    public @NotNull PowerBlockType getPowerBlockType() {
        return structure.powerBlockType;
    }

    public boolean useSolidBlockBetweenPowerBlockAndPiston() {
        return structure.useSolidBlockBetweenPowerBlockAndPiston;
    }

    public boolean useTargetBlockForPowerBlockDepending() {
        return structure.useTargetBlockForPowerBlockDepending;
    }

    public boolean testBeforePlace(@NotNull World world, @NotNull BlockPos targetPos, boolean hasDependBlock) {
        return structure.testBeforePlace(world, targetPos, hasDependBlock);
    }

    public boolean testAfterPlace(@NotNull World world, @NotNull BlockPos targetPos) {
        return structure.testAfterPlace(world, targetPos);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockBreakStructureFull)) return false;

        BlockBreakStructureFull that = (BlockBreakStructureFull) o;
        return targetPos.equals(that.targetPos) && structure.equals(that.structure);
    }

    @Override
    public int hashCode() {
        int result = targetPos.hashCode();
        result = 31 * result + structure.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BlockBreakStructureFull{" +
                "targetPos=" + targetPos +
                ", structure=" + structure +
                ", pistonPos=" + pistonPos +
                ", pistonHeadPos=" + pistonHeadPos +
                ", powerBlockPos=" + powerBlockPos +
                ", dependBlockPos=" + dependBlockPos +
                ", strongPoweringBlockByPowerBlockPos=" + strongPoweringBlockByPowerBlockPos +
                '}';
    }
}
