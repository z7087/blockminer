package me.z7087.blockminer.util.finder;

import me.z7087.blockminer.util.BlockUtils;
import me.z7087.blockminer.util.data.PistonPowerInfo;
import me.z7087.blockminer.util.enums.PowerBlockType;
import net.minecraft.block.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class BlockFinder2 {
    private BlockFinder2() {}

    public static final Direction[] DIRECTIONS = Direction.values();
    private static final Direction[] DIRECTIONS_WITHOUT_UP = Arrays.stream(DIRECTIONS).filter((direction -> direction != Direction.UP)).toArray(Direction[]::new);

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
        cmp = Integer.compare(BlockUtils.getDistance(BlockPos.ORIGIN, a.powerBlockOffset), BlockUtils.getDistance(BlockPos.ORIGIN, b.powerBlockOffset));
        if (cmp != 0)
            return cmp;
        return Integer.compare(BlockUtils.getDistance(BlockPos.ORIGIN, a.powerBlockOffset.offset(a.powerBlockFace.getOpposite())), BlockUtils.getDistance(BlockPos.ORIGIN, b.powerBlockOffset.offset(b.powerBlockFace.getOpposite())));
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
        private static Direction getPistonOffsetDirectionFromPosOffset(BlockPos pistonOffsetPos) {
            final Direction pistonOffset = PosOffset2DirectionMap.get(pistonOffsetPos);
            if (pistonOffset == null) {
                throw new IllegalArgumentException("Don't know how to break target block with piston offset position: " + pistonOffsetPos);
            }
            return pistonOffset;
        }

        public final Direction pistonOffset;
        public final Direction pistonFace;
        public final BlockPos powerBlockOffset;
        public final Direction powerBlockFace;
        public final PowerBlockType powerBlockType;
        public final boolean useSolidBlockBetweenPowerBlockAndPiston;

        public final transient boolean useTargetBlockForPowerBlockDepending;

        private final transient BlockBoxHelper structureRange;

        private final transient int hash;

        BlockBreakStructure(
                Direction pistonOffset,
                Direction pistonFace,
                BlockPos powerBlockOffset,
                Direction powerBlockFace,
                PowerBlockType powerBlockType,
                boolean useSolidBlockBetweenPowerBlockAndPiston
        ) {
            this.pistonOffset = Objects.requireNonNull(pistonOffset);
            this.pistonFace = Objects.requireNonNull(pistonFace);
            this.powerBlockOffset = Objects.requireNonNull(powerBlockOffset);
            this.powerBlockFace = Objects.requireNonNull(powerBlockFace);
            if (powerBlockType == PowerBlockType.Both) {
                throw new IllegalArgumentException("powerBlockType == PowerBlockType.Both");
            }
            this.powerBlockType = Objects.requireNonNull(powerBlockType);

            final BlockPos dependBlockOffset = powerBlockOffset.offset(powerBlockFace.getOpposite());
            this.useTargetBlockForPowerBlockDepending = BlockPos.ORIGIN.equals(dependBlockOffset);
            this.useSolidBlockBetweenPowerBlockAndPiston = useSolidBlockBetweenPowerBlockAndPiston;

            final BlockPos pistonOffsetPos = BlockPos.ORIGIN.offset(pistonOffset);
            final BlockPos pistonHeadOffsetPos = pistonOffsetPos.offset(pistonFace);
            this.structureRange = BlockBoxHelper.encompassPositionsAt000(
                    Arrays.asList(
                            pistonOffsetPos,
                            pistonHeadOffsetPos,
                            powerBlockOffset,
                            dependBlockOffset
                    )
            );
            this.hash = this.calcHash();
        }

        BlockBreakStructure(
                BlockPos pistonOffsetPos,
                Direction pistonFace,
                BlockPos powerBlockOffset,
                Direction powerBlockFace,
                PowerBlockType powerBlockType,
                boolean useSolidBlockBetweenPowerBlockAndPiston
        ) {
            this(
                    getPistonOffsetDirectionFromPosOffset(pistonOffsetPos),
                    pistonFace,
                    powerBlockOffset,
                    powerBlockFace,
                    powerBlockType,
                    useSolidBlockBetweenPowerBlockAndPiston
            );
        }

        BlockBreakStructure(
                BlockPos targetPos,
                BlockPos pistonPos,
                Direction pistonFace,
                BlockPos powerBlockPos,
                Direction powerBlockFace,
                PowerBlockType powerBlockType,
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
                final BlockPos pistonHeadPos = pistonPos.offset(pistonFace);
                if (world.isInBuildLimit(pistonHeadPos)
                        && BlockUtils.isReplaceable(world.getBlockState(pistonHeadPos))
                        && world.canPlace(stoneState, pistonHeadPos, ShapeContext.absent())
                        && BlockFinder.isPistonPlaceSafe(world, pistonPos, pistonFace)
                ) {
                    final BlockPos powerBlockPos = targetPos.add(powerBlockOffset);
                    if (world.isInBuildLimit(powerBlockPos)
                            && BlockUtils.isReplaceable(world.getBlockState(powerBlockPos))
                    ) {
                        final BlockPos dependBlockPos = powerBlockPos.offset(powerBlockFace.getOpposite());
                        final BlockState dependBlockState;
                        final BlockPos strongPoweringBlockByPowerBlockPos = powerBlockType.isRedstoneTorch() ? powerBlockPos.up() : dependBlockPos;
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
                final BlockPos pistonHeadPos = pistonPos.offset(pistonFace);
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
                final BlockPos powerBlockPos = targetPos.add(powerBlockOffset);
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
                    final BlockPos strongPoweringBlockByPowerBlockPos = powerBlockType.isRedstoneTorch() ? powerBlockPos.up() : powerBlockPos.offset(powerBlockFace.getOpposite());
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
                        final BlockPos strongPoweringBlockByPowerBlockPos = powerBlockType.isRedstoneTorch() ? powerBlockOffset.up() : powerBlockOffset.offset(powerBlockFace.getOpposite());
                        return !strongPoweringBlockByPowerBlockPos.equals(offsetPos);
                    }
                    return true;
                }
                case CP_SOLID_BLOCK: {
                    if (!this.structureRange.contains(offsetPos))
                        return true;
                    final BlockPos pistonOffsetPos = BlockPos.ORIGIN.offset(pistonOffset);
                    if (pistonOffsetPos.equals(offsetPos)) {
                        return false;
                    }
                    final BlockPos pistonHeadOffsetPos = pistonOffsetPos.offset(pistonFace);
                    if (pistonHeadOffsetPos.equals(offsetPos)) {
                        return false;
                    }
                    return !powerBlockOffset.equals(offsetPos);
                }
                case CP_OTHER:
                default: {
                    final BlockPos dependBlockOffsetPos = powerBlockOffset.offset(powerBlockFace.getOpposite());
                    if (this.structureRange.contains(offsetPos)) {
                        final BlockPos pistonOffsetPos = BlockPos.ORIGIN.offset(pistonOffset);
                        if (pistonOffsetPos.equals(offsetPos)) {
                            return false;
                        }
                        final BlockPos pistonHeadOffsetPos = pistonOffsetPos.offset(pistonFace);
                        if (pistonHeadOffsetPos.equals(offsetPos)) {
                            return false;
                        }
                        if (powerBlockOffset.equals(offsetPos)) {
                            return false;
                        }
                        if (dependBlockOffsetPos.equals(offsetPos)) {
                            return false;
                        }
                    }
                    if (useSolidBlockBetweenPowerBlockAndPiston) {
                        final BlockPos strongPoweringBlockByPowerBlockPos = powerBlockType.isRedstoneTorch() ? powerBlockOffset.up() : dependBlockOffsetPos;
                        return !strongPoweringBlockByPowerBlockPos.equals(offsetPos);
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
                    && powerBlockOffset.equals(structure.powerBlockOffset)
                    && powerBlockFace == structure.powerBlockFace
                    && powerBlockType == structure.powerBlockType;
        }

        @Override
        public String toString() {
            return "BlockBreakStructure{" +
                    "pistonOffset=" + pistonOffset +
                    ", pistonFace=" + pistonFace +
                    ", powerBlockOffset=" + powerBlockOffset +
                    ", powerBlockFace=" + powerBlockFace +
                    ", powerBlockType=" + powerBlockType +
                    ", useSolidBlockBetweenPowerBlockAndPiston=" + useSolidBlockBetweenPowerBlockAndPiston +
                    ", useTargetBlockForPowerBlockDepending=" + useTargetBlockForPowerBlockDepending +
                    '}';
        }

        private int calcHash() {
            int result = pistonOffset.hashCode();
            result = 31 * result + pistonFace.hashCode();
            result = 31 * result + powerBlockOffset.hashCode();
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
            cmp = this.powerBlockOffset.compareTo(o.powerBlockOffset);
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

    public static final class StructureFilterCache {
        public static final Map<BlockPos, Set<BlockBreakStructure>> M_OUT_OF_WORLD;
        public static final Map<BlockPos, Set<BlockBreakStructure>> M_PLACEABLE;
        public static final Map<BlockPos, Set<BlockBreakStructure>> M_SOLID_BLOCK;
        public static final Map<BlockPos, Set<BlockBreakStructure>> M_OTHER;
        static {
            //long time = System.nanoTime();
            //System.out.println("starting init StructureFilterCache");
            final HashMap<BlockPos, Set<BlockBreakStructure>> mOOW = new HashMap<>();
            final HashMap<BlockPos, Set<BlockBreakStructure>> mPlaceable = new HashMap<>();
            final HashMap<BlockPos, Set<BlockBreakStructure>> mSolidBlock = new HashMap<>();
            final HashMap<BlockPos, Set<BlockBreakStructure>> mOther = new HashMap<>();
            for (BlockPos pos : PositionsIn4Steps.posList) {
                final LinkedHashSet<BlockBreakStructure> sOOW = new LinkedHashSet<>();
                final LinkedHashSet<BlockBreakStructure> sPlaceable = new LinkedHashSet<>();
                final LinkedHashSet<BlockBreakStructure> sSolidBlock = new LinkedHashSet<>();
                final LinkedHashSet<BlockBreakStructure> sOther = new LinkedHashSet<>();
                for (BlockBreakStructure structure : AllStructures) {
                    if (structure.testForPosFilter(ComparablePredicate.CP_OUT_OF_WORLD, pos)) {
                        sOOW.add(structure);
                    }
                    if (structure.testForPosFilter(ComparablePredicate.CP_PLACEABLE, pos)) {
                        sPlaceable.add(structure);
                    }
                    if (structure.testForPosFilter(ComparablePredicate.CP_SOLID_BLOCK, pos)) {
                        sSolidBlock.add(structure);
                    }
                    if (structure.testForPosFilter(ComparablePredicate.CP_OTHER, pos)) {
                        sOther.add(structure);
                    }
                }
                mOOW.put(pos, Collections.unmodifiableSet(sOOW));
                mPlaceable.put(pos, Collections.unmodifiableSet(sPlaceable));
                mSolidBlock.put(pos, Collections.unmodifiableSet(sSolidBlock));
                mOther.put(pos, Collections.unmodifiableSet(sOther));
            }
            M_OUT_OF_WORLD = Collections.unmodifiableMap(mOOW);
            M_PLACEABLE = Collections.unmodifiableMap(mPlaceable);
            M_SOLID_BLOCK = Collections.unmodifiableMap(mSolidBlock);
            M_OTHER = Collections.unmodifiableMap(mOther);
            //time = System.nanoTime() - time;
            //System.out.println("end init StructureFilterCache with " + time / 1000000.D + " milliseconds");
        }

        private static final LinkedHashSet<BlockBreakStructure> EMPTY_SET = new LinkedHashSet<>();
        private static final Iterator<PistonPowerInfo> EMPTY_ITERATOR = new Iterator<PistonPowerInfo>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public PistonPowerInfo next() {
                throw new NoSuchElementException();
            }

            @Override
            public void forEachRemaining(Consumer<? super PistonPowerInfo> action) {
                Objects.requireNonNull(action);
            }
        };
        private static final Iterable<PistonPowerInfo> EMPTY_ITERABLE = () -> EMPTY_ITERATOR;

        // 需要使用testBeforePlace二次测试
        public static LinkedHashSet<BlockBreakStructure> findPossibleStructuresInCache(World world, BlockPos targetPos) {
            final LinkedHashSet<BlockBreakStructure> possibleStructures = new LinkedHashSet<>(AllStructures);
            for (BlockPos offsetPos : PositionsIn4Steps.posList) {
                final BlockPos pos = targetPos.add(offsetPos);
                final BlockState state = world.getBlockState(pos);
                if (ComparablePredicate.CP_OUT_OF_WORLD.test(world, pos, state)) {
                    possibleStructures.retainAll(M_OUT_OF_WORLD.get(offsetPos));
                } else if (ComparablePredicate.CP_PLACEABLE.test(world, pos, state)) {
                    possibleStructures.retainAll(M_PLACEABLE.get(offsetPos));
                } else if (ComparablePredicate.CP_SOLID_BLOCK.test(world, pos, state)) {
                    possibleStructures.retainAll(M_SOLID_BLOCK.get(offsetPos));
                } else {
                    possibleStructures.retainAll(M_OTHER.get(offsetPos));
                }
                if (possibleStructures.isEmpty()) {
                    return EMPTY_SET;
                }
            }
            return possibleStructures;
        }

        // 临时的把BlockBreakStructure转为PistonPowerInfo的替代方案
        // TODO 在Task里适配BlockBreakStructure
        public static Iterable<PistonPowerInfo> findPossibleStructuresInCacheTMP(
                World world,
                BlockPos targetPos,
                PowerBlockType powerBlockUsage,
                boolean hasDependBlock
        ) {
            Stream<BlockBreakStructure> stream;
            {
                final LinkedHashSet<BlockBreakStructure> possibleStructures = findPossibleStructuresInCache(world, targetPos);
                if (possibleStructures.isEmpty()) {
                    return EMPTY_ITERABLE;
                }
                stream = possibleStructures.stream();
            }
            if (powerBlockUsage != PowerBlockType.Both) {
                stream = stream.filter(structure -> structure.powerBlockType == powerBlockUsage);
            }
            return stream2Iterable(
                    stream
                    .filter(structure -> structure.testBeforePlace(world, targetPos, hasDependBlock))
                    .map(structure -> new PistonPowerInfo(
                            targetPos.offset(structure.pistonOffset),
                            structure.pistonFace,
                            targetPos.add(structure.powerBlockOffset),
                            structure.powerBlockFace,
                            structure.powerBlockType)
                    )
            );
        }

        private static <T> Iterable<T> stream2Iterable(Stream<T> stream) {
            return stream::iterator;
        }
    }
    public static final class PositionsIn4Steps {
        public static final List<BlockPos> posList;
        static {
            posList = Collections.unmodifiableList(new ArrayList<>(PositionsIn4Steps.get()));
        }

        private static LinkedHashSet<BlockPos> get() {
            final BlockPos pO = BlockPos.ORIGIN;
            final LinkedHashSet<BlockPos> posSet = new LinkedHashSet<>();
            final LinkedHashSet<BlockPos> tmpPosSet = new LinkedHashSet<>();
            posSet.add(pO);
            for (int i = 0; i < 4; ++i) {
                for (BlockPos pos : posSet) {
                    if (BlockUtils.getDistance(pO, pos) == i) {
                        for (Direction direction : DIRECTIONS) {
                            BlockPos stepPos = pos.offset(direction);
                            if (BlockUtils.getDistance(pO, stepPos) > i) {
                                tmpPosSet.add(stepPos);
                            }
                        }
                    }
                }
                posSet.addAll(tmpPosSet);
                tmpPosSet.clear();
            }
            return posSet;
        }
    }
}
