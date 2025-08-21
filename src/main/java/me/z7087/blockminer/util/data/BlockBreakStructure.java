package me.z7087.blockminer.util.data;

import me.z7087.blockminer.util.BlockUtils;
import me.z7087.blockminer.util.constants.PositionsInSteps;
import me.z7087.blockminer.util.enums.PowerBlockType;
import me.z7087.blockminer.util.finder.BlockFinder;
import net.minecraft.block.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class BlockBreakStructure implements Comparable<BlockBreakStructure> {
    private static final Map<BlockPos, Direction> PosOffset2DirectionMap;

    static {
        final HashMap<BlockPos, Direction> map = new HashMap<>(8);
        final BlockPos pO = BlockPos.ORIGIN;
        map.put(pO.up(), Direction.UP);
        map.put(pO.down(), Direction.DOWN);
        map.put(pO.north(), Direction.NORTH);
        map.put(pO.south(), Direction.SOUTH);
        map.put(pO.west(), Direction.WEST);
        map.put(pO.east(), Direction.EAST);
        PosOffset2DirectionMap = Collections.unmodifiableMap(map);
    }

    private static @NotNull Direction getPistonOffsetDirectionFromPosOffset(@NotNull BlockPos pistonOffsetPos) {
        final Direction pistonOffset = PosOffset2DirectionMap.get(pistonOffsetPos);
        if (pistonOffset == null) {
            throw new IllegalArgumentException("Don't know how to break target block with piston offset position: " + pistonOffsetPos);
        }
        return pistonOffset;
    }

    public final @NotNull Direction pistonOffset;
    public final @NotNull Direction pistonFace;
    public final @NotNull BlockPos powerBlockOffsetPos;
    public final @NotNull Direction powerBlockFace;
    public final @NotNull PowerBlockType powerBlockType;
    public final boolean useSolidBlockBetweenPowerBlockAndPiston;

    public final transient @NotNull BlockPos pistonOffsetPos;
    public final transient @NotNull BlockPos pistonHeadOffsetPos;
    public final transient @NotNull BlockPos dependBlockOffsetPos;
    public final transient @NotNull BlockPos strongPoweringBlockByPowerBlockOffsetPos;
    public final transient boolean useTargetBlockForPowerBlockDepending;

    private final transient @NotNull BlockBoxHelper structureRange;

    private final transient int hash;

    public BlockBreakStructure(
            @NotNull Direction pistonOffset,
            @NotNull Direction pistonFace,
            @NotNull BlockPos powerBlockOffsetPos,
            @NotNull Direction powerBlockFace,
            @NotNull PowerBlockType powerBlockType,
            boolean useSolidBlockBetweenPowerBlockAndPiston
    ) {
        this.pistonOffset = pistonOffset;
        this.pistonFace = pistonFace;
        this.powerBlockOffsetPos = PositionsInSteps.DeduplicationInS4.deduplicate(
                powerBlockOffsetPos
        );
        this.powerBlockFace = powerBlockFace;
        if (powerBlockType == PowerBlockType.Both) {
            throw new IllegalArgumentException("powerBlockType == PowerBlockType.Both");
        }
        this.powerBlockType = powerBlockType;
        this.useSolidBlockBetweenPowerBlockAndPiston = useSolidBlockBetweenPowerBlockAndPiston;

        final BlockPos pistonOffsetPos = PositionsInSteps.DeduplicationInS4.deduplicate(
                BlockPos.ORIGIN.offset(pistonOffset)
        );
        final BlockPos pistonHeadOffsetPos = PositionsInSteps.DeduplicationInS4.deduplicate(
                pistonOffsetPos.offset(pistonFace)
        );
        this.pistonOffsetPos = pistonOffsetPos;
        this.pistonHeadOffsetPos = pistonHeadOffsetPos;
        final BlockPos dependBlockOffsetPos = PositionsInSteps.DeduplicationInS4.deduplicate(
                powerBlockOffsetPos.offset(powerBlockFace.getOpposite())
        );
        this.dependBlockOffsetPos = dependBlockOffsetPos;
        switch (powerBlockType) {
            case RedstoneTorch: {
                this.strongPoweringBlockByPowerBlockOffsetPos = PositionsInSteps.DeduplicationInS4.deduplicate(
                        powerBlockOffsetPos.up()
                );
                break;
            }
            case Lever: {
                this.strongPoweringBlockByPowerBlockOffsetPos = dependBlockOffsetPos;
                break;
            }
            default: {
                throw new AssertionError();
            }
        }
        this.useTargetBlockForPowerBlockDepending = BlockPos.ORIGIN.equals(dependBlockOffsetPos);

        this.structureRange = BlockBoxHelper.encompassPositionsAt000(
                Arrays.asList(
                        pistonOffsetPos,
                        pistonHeadOffsetPos,
                        powerBlockOffsetPos,
                        dependBlockOffsetPos
                )
        );
        this.hash = this.calcHash();
    }

    public BlockBreakStructure(
            @NotNull BlockPos pistonOffsetPos,
            @NotNull Direction pistonFace,
            @NotNull BlockPos powerBlockOffsetPos,
            @NotNull Direction powerBlockFace,
            @NotNull PowerBlockType powerBlockType,
            boolean useSolidBlockBetweenPowerBlockAndPiston
    ) {
        this(
                getPistonOffsetDirectionFromPosOffset(pistonOffsetPos),
                pistonFace,
                powerBlockOffsetPos,
                powerBlockFace,
                powerBlockType,
                useSolidBlockBetweenPowerBlockAndPiston
        );
    }

    public BlockBreakStructure(
            @NotNull BlockPos targetPos,
            @NotNull BlockPos pistonPos,
            @NotNull Direction pistonFace,
            @NotNull BlockPos powerBlockPos,
            @NotNull Direction powerBlockFace,
            @NotNull PowerBlockType powerBlockType,
            boolean useSolidBlockBetweenPowerBlockAndPiston
    ) {
        this(
                pistonPos.subtract(targetPos),
                pistonFace,
                powerBlockPos.subtract(targetPos),
                powerBlockFace,
                powerBlockType,
                useSolidBlockBetweenPowerBlockAndPiston
        );
    }

    // 不含距离检测的事前判断
    public boolean testBeforePlace(World world, BlockPos targetPos, boolean hasDependBlock) {
        final BlockState stoneState = Blocks.STONE.getDefaultState();
        final BlockPos pistonPos = targetPos.offset(pistonOffset);
        if (world.isInBuildLimit(pistonPos)
                && BlockUtils.isReplaceable(world.getBlockState(pistonPos))
                && world.canPlace(stoneState, pistonPos, ShapeContext.absent())
        ) {
            final BlockPos pistonHeadPos = targetPos.add(pistonHeadOffsetPos);
            if (world.isInBuildLimit(pistonHeadPos)
                    && BlockUtils.isReplaceable(world.getBlockState(pistonHeadPos))
                    && world.canPlace(stoneState, pistonHeadPos, ShapeContext.absent())
                    && BlockFinder.isPistonPlaceSafe(world, pistonPos, pistonFace)
            ) {
                final BlockPos powerBlockPos = targetPos.add(powerBlockOffsetPos);
                if (world.isInBuildLimit(powerBlockPos)
                        && BlockUtils.isReplaceable(world.getBlockState(powerBlockPos))
                ) {
                    final BlockPos dependBlockPos = targetPos.add(dependBlockOffsetPos);
                    final BlockState dependBlockState;
                    final BlockPos strongPoweringBlockByPowerBlockPos = targetPos.add(strongPoweringBlockByPowerBlockOffsetPos);
                    final BlockState strongPoweringBlockByPowerBlockState = world.getBlockState(strongPoweringBlockByPowerBlockPos);
                    if (world.isInBuildLimit(dependBlockPos)
                            && (
                            (
                                    BlockUtils.isReplaceable(dependBlockState = world.getBlockState(dependBlockPos))
                                            && world.canPlace(stoneState, dependBlockPos, ShapeContext.absent())
                                            && hasDependBlock
                            )
                                    || (
                                    (
                                            !useSolidBlockBetweenPowerBlockAndPiston
                                                    || strongPoweringBlockByPowerBlockState.isSolidBlock(world, dependBlockPos)
                                    )
                                            && dependBlockState.isSideSolidFullSquare(world, dependBlockPos, powerBlockFace)
                            )
                    )
                    ) {
                        // 判断是否有其他能源方块正在激活附着方块
                        if (world.getReceivedStrongRedstonePower(dependBlockPos) == 0) {
                            // 判断能源方块强充能的位置是否有固体方块，如果有，判断能源方块是否能通过此方块充能到其他红石火把和活塞
                            {
                                if (strongPoweringBlockByPowerBlockState.isSolidBlock(world, strongPoweringBlockByPowerBlockPos)) {
                                    for (Direction directionsAroundStrongPoweringBlockByPowerBlock : BlockFinder.DIRECTIONS) {
                                        BlockPos mayGetPoweredPos = strongPoweringBlockByPowerBlockPos.offset(directionsAroundStrongPoweringBlockByPowerBlock);
                                        BlockState mayGetPoweredState = world.getBlockState(mayGetPoweredPos);
                                        Block mayGetPoweredBlock = mayGetPoweredState.getBlock();
                                        if (mayGetPoweredBlock instanceof RedstoneTorchBlock) {
                                            Direction redstoneTorchFace;
                                            if (mayGetPoweredBlock instanceof WallRedstoneTorchBlock) {
                                                redstoneTorchFace = mayGetPoweredState.get(WallRedstoneTorchBlock.FACING);
                                            } else {
                                                redstoneTorchFace = Direction.UP;
                                            }
                                            if (redstoneTorchFace == directionsAroundStrongPoweringBlockByPowerBlock)
                                                return false;
                                        } else if (mayGetPoweredBlock instanceof PistonBlock) {
                                            return false;
                                        }
                                        mayGetPoweredBlock = world.getBlockState(mayGetPoweredPos.down()).getBlock();
                                        if (mayGetPoweredBlock instanceof PistonBlock) {
                                            return false;
                                        }
                                    }
                                }
                            }
                            // 判断能源方块是否能不依赖强充能的方块激活其他活塞
                            {
                                for (Direction directionsAroundPowerBlock : BlockFinder.DIRECTIONS) {
                                    if (powerBlockType.isRedstoneTorch() && directionsAroundPowerBlock == powerBlockFace.getOpposite())
                                        continue;
                                    BlockPos mayGetPoweredPos = powerBlockPos.offset(directionsAroundPowerBlock);
                                    if (world.getBlockState(mayGetPoweredPos).getBlock() instanceof PistonBlock
                                            || world.getBlockState(mayGetPoweredPos.down()).getBlock() instanceof PistonBlock
                                    )
                                        return false;
                                }
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean testAfterPlace(World world, BlockPos targetPos) {
        {
            final BlockPos pistonPos = targetPos.offset(pistonOffset);
            {
                final BlockState pistonPosState = world.getBlockState(pistonPos);
                if (!(pistonPosState.getBlock() instanceof PistonBlock) || (pistonPosState.get(Properties.FACING) != pistonFace)) {
                    return false;
                }
            }
            final BlockPos pistonHeadPos = targetPos.add(pistonHeadOffsetPos);
            final BlockState pistonHeadPosState = world.getBlockState(pistonHeadPos);
            if (!pistonHeadPosState.isAir() && !(pistonHeadPosState.getBlock() instanceof PistonExtensionBlock)) {
                if (pistonHeadPosState.getBlock() instanceof PistonHeadBlock) {
                    if (pistonHeadPosState.get(Properties.FACING) != pistonFace) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        {
            final BlockPos powerBlockPos = targetPos.add(powerBlockOffsetPos);
            {
                final BlockState powerBlockPosState = world.getBlockState(powerBlockPos);
                final Block powerBlockPosBlock = powerBlockPosState.getBlock();
                switch (powerBlockType) {
                    case RedstoneTorch: {
                        if (!(powerBlockPosBlock instanceof RedstoneTorchBlock)) {
                            return false;
                        }
                        if (powerBlockFace !=
                                (
                                        (powerBlockPosBlock instanceof WallRedstoneTorchBlock)
                                                ? powerBlockPosState.get(Properties.FACING)
                                                : Direction.UP
                                )
                        ) {
                            return false;
                        }
                        if (!powerBlockPosState.get(Properties.LIT)) {
                            return false;
                        }
                        break;
                    }
                    case Lever: {
                        if (!(powerBlockPosBlock instanceof LeverBlock)) {
                            return false;
                        }
                        switch (powerBlockPosState.get(Properties.BLOCK_FACE)) {
                            case CEILING: {
                                if (powerBlockFace != Direction.DOWN)
                                    return false;
                                break;
                            }
                            case FLOOR: {
                                if (powerBlockFace != Direction.UP)
                                    return false;
                                break;
                            }
                            default: {
                                if (powerBlockFace != powerBlockPosState.get(Properties.FACING))
                                    return false;
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if (useSolidBlockBetweenPowerBlockAndPiston) {
                final BlockPos strongPoweringBlockByPowerBlockPos = targetPos.add(strongPoweringBlockByPowerBlockOffsetPos);
                //noinspection RedundantIfStatement
                if (!world.getBlockState(strongPoweringBlockByPowerBlockPos).isSolidBlock(world, strongPoweringBlockByPowerBlockPos))
                    return false;
            }
        }
        return true;
    }

    public boolean testForPosFilter(BlockFinder.ComparablePredicate cp, BlockPos offsetPos) {
        switch (cp) {
            case CP_OUT_OF_WORLD: {
                return !this.structureRange.contains(offsetPos);
            }
            case CP_PLACEABLE: {
                if (useSolidBlockBetweenPowerBlockAndPiston) {
                    return !strongPoweringBlockByPowerBlockOffsetPos.equals(offsetPos);
                }
                return true;
            }
            case CP_SOLID_BLOCK: {
                if (!this.structureRange.contains(offsetPos))
                    return true;
                if (pistonOffsetPos.equals(offsetPos)) {
                    return false;
                }
                if (pistonHeadOffsetPos.equals(offsetPos)) {
                    return false;
                }
                return !powerBlockOffsetPos.equals(offsetPos);
            }
            case CP_OTHER:
            default: {
                if (this.structureRange.contains(offsetPos)) {
                    if (pistonOffsetPos.equals(offsetPos)) {
                        return false;
                    }
                    if (pistonHeadOffsetPos.equals(offsetPos)) {
                        return false;
                    }
                    if (powerBlockOffsetPos.equals(offsetPos)) {
                        return false;
                    }
                    if (dependBlockOffsetPos.equals(offsetPos)) {
                        return false;
                    }
                }
                if (useSolidBlockBetweenPowerBlockAndPiston) {
                    return !strongPoweringBlockByPowerBlockOffsetPos.equals(offsetPos);
                }
                return true;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockBreakStructure)) return false;

        final BlockBreakStructure structure = (BlockBreakStructure) o;
        return pistonOffset == structure.pistonOffset
                && pistonFace == structure.pistonFace
                && powerBlockOffsetPos.equals(structure.powerBlockOffsetPos)
                && powerBlockFace == structure.powerBlockFace
                && powerBlockType == structure.powerBlockType;
    }

    @Override
    public String toString() {
        return "BlockBreakStructure{" +
                "pistonOffset=" + pistonOffset +
                ", pistonFace=" + pistonFace +
                ", powerBlockOffset=" + powerBlockOffsetPos +
                ", powerBlockFace=" + powerBlockFace +
                ", powerBlockType=" + powerBlockType +
                ", useSolidBlockBetweenPowerBlockAndPiston=" + useSolidBlockBetweenPowerBlockAndPiston +
                ", useTargetBlockForPowerBlockDepending=" + useTargetBlockForPowerBlockDepending +
                '}';
    }

    private int calcHash() {
        int result = pistonOffset.hashCode();
        result = 31 * result + pistonFace.hashCode();
        result = 31 * result + powerBlockOffsetPos.hashCode();
        result = 31 * result + powerBlockFace.hashCode();
        result = 31 * result + powerBlockType.hashCode();
        return result;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    // 允许HashMap在哈希冲突时做二叉树 但我觉得可能性没那么多
    @Override
    public int compareTo(BlockBreakStructure o) {
        int cmp = this.pistonOffset.compareTo(o.pistonOffset);
        if (cmp != 0)
            return cmp;
        cmp = this.pistonFace.compareTo(o.pistonFace);
        if (cmp != 0)
            return cmp;
        cmp = this.powerBlockOffsetPos.compareTo(o.powerBlockOffsetPos);
        if (cmp != 0)
            return cmp;
        cmp = this.powerBlockFace.compareTo(o.powerBlockFace);
        if (cmp != 0)
            return cmp;
        return this.powerBlockType.compareTo(o.powerBlockType);
    }
}
