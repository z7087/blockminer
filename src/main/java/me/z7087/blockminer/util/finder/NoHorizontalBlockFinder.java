package me.z7087.blockminer.util.finder;


import me.z7087.blockminer.api.enums.PowerBlockType;
import me.z7087.blockminer.util.constants.PositionsInSteps;
import me.z7087.blockminer.util.data.*;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class NoHorizontalBlockFinder {
    public static final class StructureFilterCache {
        private static final UniqueObjectCollection<BlockBreakStructure> STRUCTURES = UniqueObjectCollection.createInstance(
                "NoHorizontalBlockFinder.StructureFilterCache.BlockBreakStructure",
                BlockBreakStructure[]::new,
                BlockFinder.AllStructures
                        .stream()
                        .filter(structure -> structure.pistonOffset.getAxis() == Direction.Axis.Y && structure.pistonFace.getAxis() == Direction.Axis.Y)
                        .collect(Collectors.toList())
        );
        private static final UniqueObject<BlockPos>[] UNIQUE_POSITIONS = UniqueObjectPool.createUniqueObjectArray(UniqueObjectPool.withSInt32Limit("NoHorizontalBlockFinder.StructureFilterCache.BlockPos"), PositionsInSteps.S4.posList);
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
                    if (structure.testForPosFilter(BlockFinder.ComparablePredicate.CP_OUT_OF_WORLD, pos)) {
                        sOOW.set(uniqueStructureId, false);
                    }
                    if (structure.testForPosFilter(BlockFinder.ComparablePredicate.CP_PLACEABLE, pos)) {
                        sPlaceable.set(uniqueStructureId, false);
                    }
                    if (structure.testForPosFilter(BlockFinder.ComparablePredicate.CP_SOLID_BLOCK, pos)) {
                        sSolidBlock.set(uniqueStructureId, false);
                    }
                    if (structure.testForPosFilter(BlockFinder.ComparablePredicate.CP_OTHER, pos)) {
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

        public static @NotNull Stream<BlockBreakStructure> findPossibleStructuresInCache(@NotNull World world, @NotNull BlockPos targetPos) {
            final BitSet possibleStructures = new BitSet(STRUCTURES.size());
            for (int uniquePosId = 0; uniquePosId < UNIQUE_POSITIONS.length; ++uniquePosId) {
                final UniqueObject<BlockPos> uniqueOffsetPos = UNIQUE_POSITIONS[uniquePosId];
                final BlockPos pos = targetPos.add(uniqueOffsetPos.get());
                final BlockState state = world.getBlockState(pos);
                if (BlockFinder.ComparablePredicate.CP_OUT_OF_WORLD.test(world, pos, state)) {
                    possibleStructures.or(M_OUT_OF_WORLD[uniquePosId]);
                } else if (BlockFinder.ComparablePredicate.CP_PLACEABLE.test(world, pos, state)) {
                    possibleStructures.or(M_PLACEABLE[uniquePosId]);
                } else if (BlockFinder.ComparablePredicate.CP_SOLID_BLOCK.test(world, pos, state)) {
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
