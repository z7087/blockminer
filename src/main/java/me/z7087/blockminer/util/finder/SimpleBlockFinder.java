package me.z7087.blockminer.util.finder;

import me.z7087.blockminer.util.BlockUtils;
import me.z7087.blockminer.util.constants.PositionsInSteps;
import me.z7087.blockminer.util.data.PistonPowerInfo;
import me.z7087.blockminer.util.enums.PowerBlockType;
import me.z7087.blockminer.util.finder.BlockFinder.*;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SimpleBlockFinder {
    public static final class StructureFilterCache {
        private static final UniqueObjectCollection<BlockBreakStructure> STRUCTURES = UniqueObjectCollection.createInstance(
                "SimpleBlockFinder.StructureFilterCache.BlockBreakStructure",
                BlockBreakStructure[]::new,
                BlockFinder.AllStructures
                        .stream()
                        .filter(structure -> {
                            if (BlockUtils.getDistance(BlockPos.ORIGIN, structure.powerBlockOffset) > 2)
                                return false;
                            return BlockUtils.getDistance(BlockPos.ORIGIN, structure.powerBlockOffset.offset(structure.powerBlockFace.getOpposite())) <= 2;
                        })
                        .collect(Collectors.toList())
        );
        private static final UniqueObject<BlockPos>[] UNIQUE_POSITIONS = UniqueObjectPool.createUniqueObjectArray(UniqueObjectPool.withSInt32Limit("SimpleBlockFinder.StructureFilterCache.BlockPos"), PositionsInSteps.S2.posList);
        static final BitSet[] M_OUT_OF_WORLD;
        static final BitSet[] M_PLACEABLE;
        static final BitSet[] M_SOLID_BLOCK;
        static final BitSet[] M_OTHER;
        static {
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
        }

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
}
