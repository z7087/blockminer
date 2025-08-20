package me.z7087.blockminer.util.finder;

import me.z7087.blockminer.util.BlockUtils;
import me.z7087.blockminer.util.constants.PositionsInSteps;
import me.z7087.blockminer.util.data.PistonPowerInfo;
import me.z7087.blockminer.util.enums.PowerBlockType;
import me.z7087.final2constant.Constant;
import me.z7087.final2constant.util.JavaHelper;
import net.minecraft.block.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class BlockFinder {
    private BlockFinder() {}

    public static final Direction[] DIRECTIONS = Direction.values();
    public static final Direction[][] DIRECTIONS_WITHOUT;
    static {
        Direction[] DIRECTIONS = BlockFinder.DIRECTIONS;
        final int DirectionsCount = DIRECTIONS.length;
        DIRECTIONS_WITHOUT = new Direction[DirectionsCount][DirectionsCount - 1];
        for (int i = 0; i < DirectionsCount; ++i) {
            Direction direction = DIRECTIONS[i];
            for (int j = 0, k = 0; j < DirectionsCount; j++) {
                Direction anotherDirection = DIRECTIONS[j];
                if (anotherDirection != direction) {
                    DIRECTIONS_WITHOUT[i][k++] = anotherDirection;
                }
            }
        }
    }
    private static final Direction[] DIRECTIONS_WITHOUT_UP = DIRECTIONS_WITHOUT[Direction.UP.ordinal()];
    private static final Direction[] DIRECTIONS_WITHOUT_DOWN = DIRECTIONS_WITHOUT[Direction.DOWN.ordinal()];

    public static final Comparator<BlockBreakStructure> STRUCTURE_SORT_COMPARATOR = (a, b) -> {
        // 对true=better的条件交换排序
        // 能源方块能放在正在破坏的方块上的优先，免去手动清理能源方块和依赖方块
        int cmp = Boolean.compare(b.useTargetBlockForPowerBlockDepending, a.useTargetBlockForPowerBlockDepending);
        if (cmp != 0)
            return cmp;

        // 对于活塞摆放位置和方向，优先筛选上下两个，然后再把上排在下前面
        cmp = Boolean.compare(canInstantRotateTo(b.pistonFace), canInstantRotateTo(a.pistonFace));
        if (cmp != 0)
            return cmp;
        cmp = Boolean.compare(canInstantRotateTo(b.pistonOffset), canInstantRotateTo(a.pistonOffset));
        if (cmp != 0)
            return cmp;
        cmp = Integer.compare(getDirectionCompareIndex(a.pistonFace), getDirectionCompareIndex(b.pistonFace));
        if (cmp != 0)
            return cmp;
        cmp = Integer.compare(getDirectionCompareIndex(a.pistonOffset), getDirectionCompareIndex(b.pistonOffset));
        if (cmp != 0)
            return cmp;

        // 我不知道这个条件该放哪...或者直接删掉呢
        //cmp = Boolean.compare(a.useSolidBlockBetweenPowerBlockAndPiston, b.useSolidBlockBetweenPowerBlockAndPiston);
        //if (cmp != 0)
        //    return cmp;

        // 能源方块和能源方块的依赖方块离中心点越近越好
        cmp = Integer.compare(BlockUtils.getDistance(BlockPos.ORIGIN, a.powerBlockOffsetPos), BlockUtils.getDistance(BlockPos.ORIGIN, b.powerBlockOffsetPos));
        if (cmp != 0)
            return cmp;
        return Integer.compare(BlockUtils.getDistance(BlockPos.ORIGIN, a.powerBlockOffsetPos.offset(a.powerBlockFace.getOpposite())), BlockUtils.getDistance(BlockPos.ORIGIN, b.powerBlockOffsetPos.offset(b.powerBlockFace.getOpposite())));
    };

    public static final Set<BlockBreakStructure> AllStructures;
    static {
        ArrayList<BlockBreakStructure> allStructures = new ArrayList<>();
        //long time = System.nanoTime();
        //System.out.println("starting find structures");
        findAllPossibleStructures(allStructures);
        //time = System.nanoTime() - time;
        //System.out.println("end find with " + time / 1000000.0D + " milliseconds, starting sort " + allStructures.size() + " structures");
        //time = System.nanoTime();
        allStructures.sort(STRUCTURE_SORT_COMPARATOR);
        LinkedHashSet<BlockBreakStructure> allStructuresSet = new LinkedHashSet<>(allStructures);
        //time = System.nanoTime() - time;
        //System.out.println("end sorting " + allStructuresSet.size() + " structures with " + time / 1000000.0D + " milliseconds");
        AllStructures = Collections.unmodifiableSet(allStructuresSet);
    }

    private static boolean canInstantRotateTo(Direction direction) {
        return direction.getAxis() == Direction.Axis.Y;
    }

    private static int getDirectionCompareIndex(Direction direction) {
        switch (direction) {
            case UP:
                return 0;
            case DOWN:
                return 1;
            default:
                return 2;
        }
    }

    public static void findAllPossibleStructures(List<BlockBreakStructure> possibleStructures) {
        final LinkedHashMap<BlockBreakStructure, BlockBreakStructure> structureDeduplicationMap = new LinkedHashMap<>(256);
        final BiFunction<BlockBreakStructure, BlockBreakStructure, BlockBreakStructure> mergeRemappingFunc =
                (oldStructure, newStructure) -> {
                    if (!oldStructure.useSolidBlockBetweenPowerBlockAndPiston)
                        return oldStructure;
                    return newStructure;
                };
        for (Direction pistonOffset : DIRECTIONS) {
            final BlockPos pistonOffsetPos = BlockPos.ORIGIN.offset(pistonOffset);
            for (Direction pistonFace : DIRECTIONS) {
                if (pistonFace.getOpposite() != pistonOffset) {
                    final ArrayList<BlockBreakStructure> tmpRedstoneTorchPossibleStructures = new ArrayList<>();
                    final ArrayList<BlockBreakStructure> tmpLeverPossibleStructures = new ArrayList<>();
                    {
                        findAllPossibleStructuresRedstoneTorch(tmpRedstoneTorchPossibleStructures, pistonOffsetPos, pistonFace, pistonOffsetPos);
                        findAllPossibleStructuresLever(tmpLeverPossibleStructures, pistonOffsetPos, pistonFace, pistonOffsetPos);
                        // QC
                        final BlockPos pistonQCOffsetPos = pistonOffsetPos.up();
                        findAllPossibleStructuresRedstoneTorch(tmpRedstoneTorchPossibleStructures, pistonOffsetPos, pistonFace, pistonQCOffsetPos);
                        findAllPossibleStructuresLever(tmpLeverPossibleStructures, pistonOffsetPos, pistonFace, pistonQCOffsetPos);
                    }
                    {
                        for (BlockBreakStructure structure : tmpRedstoneTorchPossibleStructures) {
                            structureDeduplicationMap.merge(structure, structure, mergeRemappingFunc);
                        }
                        possibleStructures.addAll(structureDeduplicationMap.values());
                        structureDeduplicationMap.clear();
                        for (BlockBreakStructure structure : tmpLeverPossibleStructures) {
                            structureDeduplicationMap.merge(structure, structure, mergeRemappingFunc);
                        }
                        possibleStructures.addAll(structureDeduplicationMap.values());
                        structureDeduplicationMap.clear();
                    }
                    /*
                    for (BlockBreakStructure structure : tmpPossibleStructures) {
                        structureDeduplicationMap.merge(
                                structure,
                                structure,
                                (oldStructure, newStructure) -> {
                                    if (oldStructure.powerBlockType == PowerBlockType.Both) {
                                        return oldStructure;
                                    }
                                    if (oldStructure.powerBlockType == newStructure.powerBlockType) {
                                        return oldStructure;
                                    }
                                    if (newStructure.powerBlockType == PowerBlockType.Both) {
                                        return newStructure;
                                    }
                                    return new BlockBreakStructure(
                                            oldStructure.pistonOffset,
                                            oldStructure.pistonFace,
                                            oldStructure.powerBlockOffset,
                                            oldStructure.powerBlockFace,
                                            PowerBlockType.merge(oldStructure.powerBlockType, newStructure.powerBlockType)
                                    );
                                }
                                );
                    }
                    possibleStructures.addAll(structureDeduplicationMap.values());
                     */

                }
            }
        }
    }

    private static void findAllPossibleStructuresRedstoneTorch(
            List<BlockBreakStructure> possibleStructures,
            BlockPos pistonOffsetPos,
            Direction pistonFace,
            BlockPos poweredOffsetPos
    ) {
        final BlockPos targetOffsetPos = BlockPos.ORIGIN; // always 000
        final BlockPos pistonHeadOffsetPos = pistonOffsetPos.offset(pistonFace);
        final BlockPos pistonUpOffsetPos = pistonOffsetPos.up();
        for (Direction direction : DIRECTIONS) {
            {
                final BlockPos redstoneTorchOffsetPos = poweredOffsetPos.offset(direction);
                if (!targetOffsetPos.equals(redstoneTorchOffsetPos)
                        && !pistonOffsetPos.equals(redstoneTorchOffsetPos)
                        && !pistonHeadOffsetPos.equals(redstoneTorchOffsetPos)
                ) {
                    for (Direction dependDirection : DIRECTIONS_WITHOUT_UP) {
                        final BlockPos dependBlockOffsetPos = redstoneTorchOffsetPos.offset(dependDirection);
                        if (!pistonOffsetPos.equals(dependBlockOffsetPos)
                                && !pistonHeadOffsetPos.equals(dependBlockOffsetPos)
                                && !pistonUpOffsetPos.equals(dependBlockOffsetPos) // 对于红石火把，依赖方块不能是活塞上方的方块
                        ) {
                            possibleStructures.add(
                                    new BlockBreakStructure(
                                            pistonOffsetPos,
                                            pistonFace,
                                            redstoneTorchOffsetPos,
                                            dependDirection.getOpposite(),
                                            PowerBlockType.RedstoneTorch,
                                            false
                                    )
                            );
                        }
                    }
                }
            }
            {
                final BlockPos extraBlockBetweenRedstoneTorchAndPistonOffsetPos = poweredOffsetPos.offset(direction);
                if (!pistonOffsetPos.equals(extraBlockBetweenRedstoneTorchAndPistonOffsetPos)
                        && !pistonHeadOffsetPos.equals(extraBlockBetweenRedstoneTorchAndPistonOffsetPos)
                ) {
                    final BlockPos redstoneTorchOffsetPos = poweredOffsetPos.down();
                    if (!targetOffsetPos.equals(redstoneTorchOffsetPos)
                            && !pistonOffsetPos.equals(redstoneTorchOffsetPos)
                            && !pistonHeadOffsetPos.equals(redstoneTorchOffsetPos)
                    ) {
                        for (Direction dependDirection : DIRECTIONS_WITHOUT_UP) {
                            final BlockPos dependBlockOffsetPos = redstoneTorchOffsetPos.offset(dependDirection);
                            if (!pistonOffsetPos.equals(dependBlockOffsetPos)
                                    && !pistonHeadOffsetPos.equals(dependBlockOffsetPos)
                            ) {
                                possibleStructures.add(
                                        new BlockBreakStructure(
                                                pistonOffsetPos,
                                                pistonFace,
                                                redstoneTorchOffsetPos,
                                                dependDirection.getOpposite(),
                                                PowerBlockType.RedstoneTorch,
                                                true
                                        )
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    private static void findAllPossibleStructuresLever(
            List<BlockBreakStructure> possibleStructures,
            BlockPos pistonOffsetPos,
            Direction pistonFace,
            BlockPos poweredOffsetPos
    ) {
        final BlockPos targetOffsetPos = BlockPos.ORIGIN;
        final BlockPos pistonHeadOffsetPos = pistonOffsetPos.offset(pistonFace);
        for (Direction direction : DIRECTIONS) {
            {
                final BlockPos leverOffsetPos = poweredOffsetPos.offset(direction);
                if (!targetOffsetPos.equals(leverOffsetPos)
                        && !pistonOffsetPos.equals(leverOffsetPos)
                        && !pistonHeadOffsetPos.equals(leverOffsetPos)
                ) {
                    for (Direction dependDirection : DIRECTIONS) {
                        final BlockPos dependBlockOffsetPos = leverOffsetPos.offset(dependDirection);
                        if (!pistonOffsetPos.equals(dependBlockOffsetPos)
                                && !pistonHeadOffsetPos.equals(dependBlockOffsetPos)
                                && (
                                        BlockUtils.getDistance(pistonOffsetPos, leverOffsetPos) <= 1
                                                || BlockUtils.getDistance(pistonOffsetPos, dependBlockOffsetPos) <= 1
                        )
                        ) {
                            possibleStructures.add(
                                    new BlockBreakStructure(
                                            pistonOffsetPos,
                                            pistonFace,
                                            leverOffsetPos,
                                            dependDirection.getOpposite(),
                                            PowerBlockType.Lever,
                                            false
                                    )
                            );
                        }
                    }
                }
            }
            {
                final BlockPos dependBlockBetweenLeverAndPistonOffsetPos = poweredOffsetPos.offset(direction);
                if (!pistonOffsetPos.equals(dependBlockBetweenLeverAndPistonOffsetPos)
                        && !pistonHeadOffsetPos.equals(dependBlockBetweenLeverAndPistonOffsetPos)
                ) {
                    for (Direction leverFace : DIRECTIONS) {
                        final BlockPos leverOffsetPos = dependBlockBetweenLeverAndPistonOffsetPos.offset(leverFace);
                        if (!targetOffsetPos.equals(leverOffsetPos)
                                && !pistonOffsetPos.equals(leverOffsetPos)
                                && !pistonHeadOffsetPos.equals(leverOffsetPos)
                                && (
                                        BlockUtils.getDistance(pistonOffsetPos, leverOffsetPos) <= 1
                                                || BlockUtils.getDistance(pistonOffsetPos, dependBlockBetweenLeverAndPistonOffsetPos) <= 1
                        )
                        ) {
                            possibleStructures.add(
                                    new BlockBreakStructure(
                                            pistonOffsetPos,
                                            pistonFace,
                                            leverOffsetPos,
                                            leverFace,
                                            PowerBlockType.Lever,
                                            true
                                    )
                            );
                        }
                    }
                }
            }
        }
    }

    public static boolean isPistonPlaceSafe(World world, BlockPos pistonPos, Direction face) {
        for (Direction direction : DIRECTIONS_WITHOUT[face.ordinal()]) {
            if (world.isEmittingRedstonePower(pistonPos.offset(direction), direction)) {
                return false;
            }
        }
        if (world.isEmittingRedstonePower(pistonPos, Direction.DOWN)) {
            return false;
        }
        final BlockPos pistonQCPos = pistonPos.up();
        for (Direction direction : DIRECTIONS_WITHOUT_DOWN) {
            if (world.isEmittingRedstonePower(pistonQCPos.offset(direction), direction)) {
                return false;
            }
        }

        return true;
    }

    public static final class BlockBreakStructure implements Comparable<BlockBreakStructure> {
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

        @NotNull public final Direction pistonOffset;
        @NotNull public final Direction pistonFace;
        @NotNull public final BlockPos powerBlockOffsetPos;
        @NotNull public final Direction powerBlockFace;
        @NotNull public final PowerBlockType powerBlockType;
        public final boolean useSolidBlockBetweenPowerBlockAndPiston;

        @NotNull public final transient BlockPos pistonOffsetPos;
        @NotNull public final transient BlockPos pistonHeadOffsetPos;
        @NotNull public final transient BlockPos dependBlockOffsetPos;
        @NotNull public final transient BlockPos strongPoweringBlockByPowerBlockOffsetPos;
        public final transient boolean useTargetBlockForPowerBlockDepending;

        @NotNull private final transient BlockBoxHelper structureRange;

        private final transient int hash;

        BlockBreakStructure(
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

        BlockBreakStructure(
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

        BlockBreakStructure(
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
                                        for (Direction directionsAroundStrongPoweringBlockByPowerBlock : DIRECTIONS) {
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
                                    for (Direction directionsAroundPowerBlock : DIRECTIONS) {
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

        public boolean testForPosFilter(ComparablePredicate cp, BlockPos offsetPos) {
            switch (cp) {
                case CP_OUT_OF_WORLD:{
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

    public static final class BlockBoxHelper {
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

    // 我忘记我搞这个要做什么了
    public static class LinkedNode {
        final LinkedNode lastNode;
        final Object key;
        final transient int hash;

        LinkedNode(LinkedNode lastNode, Object key) {
            this.lastNode = lastNode;
            this.key = Objects.requireNonNull(key);

            this.hash = lastNode == null
                    ? key.hashCode()
                    : (lastNode.hashCode() * 31 + key.hashCode());
        }

        public static LinkedNode of(Object key) {
            return new LinkedNode(null, key);
        }

        public LinkedNode then(Object key) {
            return new LinkedNode(this, key);
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof LinkedNode)) return false;

            LinkedNode thisNode = this;
            LinkedNode thatNode = (LinkedNode) o;
            do {
                if (thisNode == thatNode)
                    return true;
                if (!thisNode.key.equals(thatNode.key))
                    return false;
                thisNode = thisNode.lastNode;
                thatNode = thatNode.lastNode;
            } while (thisNode != null && thatNode != null);
            return thisNode == thatNode;
        }

        @Override
        public final int hashCode() {
            return hash;
        }
    }

    @FunctionalInterface
    public interface CPFunc {
        boolean test(World world, BlockPos pos, BlockState state);
    }

    public enum ComparablePredicate implements CPFunc {
        CP_OUT_OF_WORLD((world, pos, state) -> !world.isInBuildLimit(pos)),
        CP_PLACEABLE((world, pos, state) -> BlockUtils.isReplaceable(state)/* && world.canPlace(Blocks.STONE.getDefaultState(), pos, ShapeContext.absent())*/),
        CP_SOLID_BLOCK((world, pos, state) -> state.isSolidBlock(world, pos)),
        CP_OTHER((world, pos, state) -> true);

        private final CPFunc cpFunc;
        ComparablePredicate(CPFunc cpFunc) {
            this.cpFunc = cpFunc;
        }

        @Override
        public boolean test(World world, BlockPos pos, BlockState state) {
            return this.cpFunc.test(world, pos, state);
        }
    }
    /*
    public static void main(String[] args) {
        System.out.println(AllStructures);
        StructureFilterCache.M_SOLID_BLOCK.hashCode();
    }
     */


    // 对在一个集合内唯一的对象的包装。
    // 对于哈希表中的键/集合中的元素，可以用于优化hashCode/equals计算速度、减少哈希冲突，也能在哈希表/集合过大时允许哈希表/集合对未实现Comparable的对象做红黑树。
    // 由于id连续递增，应该也可以将id和对象对应上后存储在BitSet里。
    // 取原始元素需要额外的时间。
    public static final class UniqueObjectPool<T> {
        private final AtomicLong counter = new AtomicLong();
        private final String name;
        private final long limit;

        public UniqueObjectPool() {
            this.name = null;
            this.limit = -1;
        }

        public UniqueObjectPool(String name) {
            this.name = Objects.requireNonNull(name);
            this.limit = -1;
        }

        public UniqueObjectPool(long limit) {
            this.name = null;
            this.limit = limit;
        }

        public UniqueObjectPool(String name, long limit) {
            this.name = Objects.requireNonNull(name);
            this.limit = limit;
        }

        private static final long SIGNED_INT32_MAX_VALUE = ((long) Integer.MAX_VALUE) & 0xFFFFFFFFL;
        private static final long UNSIGNED_INT32_MAX_VALUE = ((((long) Integer.MAX_VALUE) & 0xFFFFFFFFL) | (((long) Integer.MIN_VALUE) & 0xFFFFFFFFL));

        public static <F> UniqueObjectPool<F> withSInt32Limit() {
            return new UniqueObjectPool<>(SIGNED_INT32_MAX_VALUE);
        }

        public static <F> UniqueObjectPool<F> withSInt32Limit(String name) {
            return new UniqueObjectPool<>(name, SIGNED_INT32_MAX_VALUE);
        }

        public static <F> UniqueObjectPool<F> withUInt32Limit() {
            return new UniqueObjectPool<>(UNSIGNED_INT32_MAX_VALUE);
        }

        public static <F> UniqueObjectPool<F> withUInt32Limit(String name) {
            return new UniqueObjectPool<>(name, UNSIGNED_INT32_MAX_VALUE);
        }

        public UniqueObject<T> ofUnique(T o) {
            long id;
            do {
                id = counter.get();
                if (id == limit) {
                    throw new IllegalStateException(name != null ? "Too many unique objects in pool '" + name + "' !" : "Too many unique objects in one pool!");
                }
            } while (!counter.compareAndSet(id, id + 1));
            return new UniqueObject<>(id, o);
        }

        public long nextId() {
            return counter.get();
        }

        @Override
        public String toString() {
            return "UniqueObjectPool(" + (name != null ? ('"' + name + "\", ") : "") + counter.get() + ')';
        }

        public static <T> UniqueObject<T>[] createUniqueObjectArray(UniqueObjectPool<T> pool, T[] originalObjects) {
            return createUniqueObjectArray(pool, originalObjects, 0);
        }
        public static <T> UniqueObject<T>[] createUniqueObjectArray(UniqueObjectPool<T> pool, Collection<T> originalObjects) {
            return createUniqueObjectArray(pool, originalObjects, 0);
        }
        public static <T> UniqueObject<T>[] createUniqueObjectArray(UniqueObjectPool<T> pool, T[] originalObjects, int poolStartIndex) {
            final int size = originalObjects.length;
            @SuppressWarnings("unchecked")
            final UniqueObject<T>[] uniqueObjectArray = (UniqueObject<T>[]) new UniqueObject[size];
            for (int i = 0; i < size; ++i, ++poolStartIndex) {
                long gotId;
                if (pool.nextId() == poolStartIndex) {
                    final UniqueObject<T> uniqueObject = pool.ofUnique(originalObjects[i]);
                    if (uniqueObject.id() == poolStartIndex) {
                        uniqueObjectArray[i] = uniqueObject;
                        continue;
                    } else {
                        gotId = uniqueObject.id();
                    }
                } else {
                    gotId = pool.nextId();
                }
                throw new IllegalStateException("Expected index " + poolStartIndex + ", but got " + gotId + "!");
            }
            return uniqueObjectArray;
        }
        public static <T> UniqueObject<T>[] createUniqueObjectArray(UniqueObjectPool<T> pool, Collection<T> originalObjects, int poolStartIndex) {
            final int size = originalObjects.size();
            @SuppressWarnings("unchecked")
            final UniqueObject<T>[] uniqueObjectArray = (UniqueObject<T>[]) new UniqueObject[size];
            final Iterator<T> iterator = originalObjects.iterator();
            for (int i = 0; i < size; ++i, ++poolStartIndex) {
                if (!iterator.hasNext()) {
                    throw new IllegalStateException("List iterator terminated unexpectedly");
                }
                long gotId;
                if (pool.nextId() == poolStartIndex) {
                    final UniqueObject<T> uniqueObject = pool.ofUnique(iterator.next());
                    if (uniqueObject.id() == poolStartIndex) {
                        uniqueObjectArray[i] = uniqueObject;
                        continue;
                    } else {
                        gotId = uniqueObject.id();
                    }
                } else {
                    gotId = pool.nextId();
                }
                throw new IllegalStateException("Expected index " + poolStartIndex + ", but got " + gotId + "!");
            }
            return uniqueObjectArray;
        }
    }

    public static final class UniqueObject<T> implements Comparable<UniqueObject<T>> {
        private final long id;
        private final T o;

        UniqueObject(long id, T o) {
            this.id = id;
            this.o = o;
        }

        public T get() {
            return o;
        }

        public long id() {
            return id;
        }

        @Override
        public int hashCode() {
            return (int) id;
        }

        @Override
        public int compareTo(UniqueObject<T> other) {
            return Long.compare(this.id, other.id);
        }

        @Override
        public String toString() {
            return "UniqueObject{" +
                    "id=" + id +
                    ", o=" + o +
                    '}';
        }
    }

    public static abstract class UniqueObjectCollection<T> {
        protected UniqueObjectCollection() {}

        private static final MethodHandle CONSTRUCTOR;
        static {
            final String[] immutableNames, immutableDescriptors;
            try {
                UniqueObjectCollection<?> uniqueObjectCollectionEmptyImpl = Constant.factory.ofEmptyAbstractImplInstance(
                        MethodHandles.lookup(),
                        UniqueObjectCollection.class
                );
                final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                        MethodHandles.lookup(),
                        (Supplier<String> & Serializable) uniqueObjectCollectionEmptyImpl::name,
                        (Supplier<Object[]> & Serializable) uniqueObjectCollectionEmptyImpl::originalObjects,
                        (Supplier<UniqueObject<?>[]> & Serializable) uniqueObjectCollectionEmptyImpl::uniqueObjects
                );
                immutableNames = immutableNamesAndDescriptors[0];
                immutableDescriptors = immutableNamesAndDescriptors[1];
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            CONSTRUCTOR = Constant.factory.ofRecordConstructor(
                    MethodHandles.lookup(),
                    UniqueObjectCollection.class,
                    false,
                    immutableNames,
                    immutableDescriptors,
                    null,
                    null,
                    true,
                    false
            );
        }

        public static <T> UniqueObjectCollection<T> createInstance(String name, IntFunction<T[]> arrayProvider, Collection<T> originalObjects) {
            final UniqueObjectPool<T> pool = name != null ? UniqueObjectPool.withSInt32Limit(name) : UniqueObjectPool.withSInt32Limit();
            final T[] originalObjectsArray = originalObjects.toArray(arrayProvider.apply(originalObjects.size()));
            final UniqueObject<T>[] uniqueObjects = UniqueObjectPool.createUniqueObjectArray(pool, originalObjects);
            try {
                @SuppressWarnings("unchecked")
                final UniqueObjectCollection<T> instance = (UniqueObjectCollection<T>) CONSTRUCTOR.invokeExact(
                        name,
                        originalObjectsArray,
                        uniqueObjects
                );
                return instance;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        abstract String name();
        abstract T[] originalObjects();
        abstract UniqueObject<T>[] uniqueObjects();

        public int size() {
            return uniqueObjects().length;
        }

        public T[] getOriginalObjects() {
            return originalObjects();
        }

        public UniqueObject<T>[] getUniqueObjects() {
            return uniqueObjects();
        }

        public String getName() {
            return name();
        }
    }

    public static final class StructureFilterCache {
        private static final UniqueObjectCollection<BlockBreakStructure> STRUCTURES = UniqueObjectCollection.createInstance("StructureFilterCache.BlockBreakStructure", BlockBreakStructure[]::new, AllStructures);
        private static final UniqueObject<BlockPos>[] UNIQUE_POSITIONS = UniqueObjectPool.createUniqueObjectArray(UniqueObjectPool.withSInt32Limit("StructureFilterCache.BlockPos"), PositionsInSteps.S4.posList);
        // 这里用0来表示可用，1表示不可用，因为新建BitSet默认填充0
        // 因为BlockPos也有一个从0开始的连续递增编号，似乎不需要哈希表了
        static final BitSet[] M_OUT_OF_WORLD;
        static final BitSet[] M_PLACEABLE;
        static final BitSet[] M_SOLID_BLOCK;
        static final BitSet[] M_OTHER;
        static {
            //long time = System.nanoTime();
            //System.out.println("starting init StructureFilterCache");
            final BitSet initnalSearchBitSet = new BitSet(STRUCTURES.size());
            initnalSearchBitSet.set(0, STRUCTURES.size(), true);

            final BitSet[] mOOW = new BitSet[UNIQUE_POSITIONS.length];
            final BitSet[] mPlaceable = mOOW.clone();
            final BitSet[] mSolidBlock = mOOW.clone();
            final BitSet[] mOther = mOOW.clone();
            for (UniqueObject<BlockPos> uniquePos : UNIQUE_POSITIONS) {
                final BlockPos pos = uniquePos.get();
                final BitSet sOOW = (BitSet) initnalSearchBitSet.clone();
                final BitSet sPlaceable = (BitSet) initnalSearchBitSet.clone();
                final BitSet sSolidBlock = (BitSet) initnalSearchBitSet.clone();
                final BitSet sOther = (BitSet) initnalSearchBitSet.clone();
                for (UniqueObject<BlockBreakStructure> uniqueStructure : STRUCTURES.getUniqueObjects()) {
                    final BlockBreakStructure structure = uniqueStructure.get();
                    final int uniqueStructureId = (int) uniqueStructure.id();
                    if (structure.testForPosFilter(ComparablePredicate.CP_OUT_OF_WORLD, pos)) {
                        sOOW.set(uniqueStructureId, false);
                    }
                    if (structure.testForPosFilter(ComparablePredicate.CP_PLACEABLE, pos)) {
                        sPlaceable.set(uniqueStructureId, false);
                    }
                    if (structure.testForPosFilter(ComparablePredicate.CP_SOLID_BLOCK, pos)) {
                        sSolidBlock.set(uniqueStructureId, false);
                    }
                    if (structure.testForPosFilter(ComparablePredicate.CP_OTHER, pos)) {
                        sOther.set(uniqueStructureId, false);
                    }
                }
                final int uniquePosId = (int) uniquePos.id();
                mOOW[uniquePosId] = sOOW;
                mPlaceable[uniquePosId] = sPlaceable;
                mSolidBlock[uniquePosId] = sSolidBlock;
                mOther[uniquePosId] = sOther;
            }
            M_OUT_OF_WORLD = mOOW;
            M_PLACEABLE = mPlaceable;
            M_SOLID_BLOCK = mSolidBlock;
            M_OTHER = mOther;
            //time = System.nanoTime() - time;
            //System.out.println("end init StructureFilterCache with " + time / 1000000.D + " milliseconds");
        }

        // 需要使用testBeforePlace二次测试
        public static Stream<BlockBreakStructure> findPossibleStructuresInCache(World world, BlockPos targetPos) {
            final BitSet possibleStructures = new BitSet(STRUCTURES.size());
            for (int uniquePosId = 0; uniquePosId < UNIQUE_POSITIONS.length; ++uniquePosId) {
                final UniqueObject<BlockPos> uniqueOffsetPos = UNIQUE_POSITIONS[uniquePosId];
                final BlockPos pos = targetPos.add(uniqueOffsetPos.get());
                final BlockState state = world.getBlockState(pos);
                if (ComparablePredicate.CP_OUT_OF_WORLD.test(world, pos, state)) {
                    possibleStructures.or(M_OUT_OF_WORLD[uniquePosId]);
                } else if (ComparablePredicate.CP_PLACEABLE.test(world, pos, state)) {
                    possibleStructures.or(M_PLACEABLE[uniquePosId]);
                } else if (ComparablePredicate.CP_SOLID_BLOCK.test(world, pos, state)) {
                    possibleStructures.or(M_SOLID_BLOCK[uniquePosId]);
                } else {
                    possibleStructures.or(M_OTHER[uniquePosId]);
                }
            }
            possibleStructures.flip(0, STRUCTURES.size()); // TODO
            return possibleStructures.stream().mapToObj((id) -> STRUCTURES.getOriginalObjects()[id]);
        }

        // 临时的把BlockBreakStructure转为PistonPowerInfo的替代方案
        // TODO 在Task里适配BlockBreakStructure
        public static Iterable<PistonPowerInfo> findPossibleStructuresInCacheTMP(
                World world,
                BlockPos targetPos,
                PowerBlockType powerBlockUsage,
                boolean hasDependBlock
        ) {
            Stream<BlockBreakStructure> stream = findPossibleStructuresInCache(world, targetPos);
            if (powerBlockUsage != PowerBlockType.Both) {
                stream = stream.filter(structure -> structure.powerBlockType == powerBlockUsage);
            }
            return stream2Iterable(
                    stream
                    .filter(structure -> structure.testBeforePlace(world, targetPos, hasDependBlock))
                    .map(structure -> new PistonPowerInfo(
                            targetPos.offset(structure.pistonOffset),
                            structure.pistonFace,
                            targetPos.add(structure.powerBlockOffsetPos),
                            structure.powerBlockFace,
                            structure.powerBlockType)
                    )
            );
        }

        private static <T> Iterable<T> stream2Iterable(Stream<T> stream) {
            return stream::iterator;
        }

    }
}
