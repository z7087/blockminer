package me.z7087.blockminer.util.finder;

import me.z7087.blockminer.util.BlockUtils;
import me.z7087.blockminer.util.constants.PositionsInSteps;
import me.z7087.blockminer.util.data.*;
import me.z7087.blockminer.api.enums.PowerBlockType;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
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
        public static @NotNull Stream<BlockBreakStructure> findPossibleStructuresInCache(@NotNull World world, @NotNull BlockPos targetPos) {
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

        public static @NotNull Stream<BlockBreakStructureFull> findPossibleFullStructuresInCache(
                @NotNull World world,
                @NotNull BlockPos targetPos,
                @NotNull PowerBlockType powerBlockUsage,
                boolean hasDependBlock
        ) {
            Stream<BlockBreakStructure> stream = findPossibleStructuresInCache(world, targetPos);
            if (powerBlockUsage != PowerBlockType.Both) {
                stream = stream.filter(structure -> structure.powerBlockType == powerBlockUsage);
            }
            return stream
                    .filter(structure -> structure.testBeforePlace(world, targetPos, hasDependBlock))
                    .map(structure -> BlockBreakStructureFull.of(targetPos, structure));
        }

    }
}
