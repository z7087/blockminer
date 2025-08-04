package me.z7087.blockminer.util.constants;

import me.z7087.blockminer.util.BlockUtils;
import me.z7087.blockminer.util.finder.BlockFinder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public final class PositionsInSteps {
    public static final class S1 {
        public static final List<BlockPos> posList;

        static {
            posList = Collections.unmodifiableList(new ArrayList<>(get()));
        }

        private static LinkedHashSet<BlockPos> get() {
            final BlockPos pO = BlockPos.ORIGIN;
            final LinkedHashSet<BlockPos> posSet = new LinkedHashSet<>();
            final LinkedHashSet<BlockPos> tmpPosSet = new LinkedHashSet<>();
            posSet.add(pO);
            for (int i = 0; i < 1; ++i) {
                for (BlockPos pos : posSet) {
                    if (BlockUtils.getDistance(pO, pos) == i) {
                        for (Direction direction : BlockFinder.DIRECTIONS) {
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

    public static final class S2 {
        public static final List<BlockPos> posList;

        static {
            posList = Collections.unmodifiableList(new ArrayList<>(get()));
        }

        private static LinkedHashSet<BlockPos> get() {
            final BlockPos pO = BlockPos.ORIGIN;
            final LinkedHashSet<BlockPos> posSet = new LinkedHashSet<>(S1.posList);
            final LinkedHashSet<BlockPos> tmpPosSet = new LinkedHashSet<>();
            for (int i = 1; i < 2; ++i) {
                for (BlockPos pos : posSet) {
                    if (BlockUtils.getDistance(pO, pos) == i) {
                        for (Direction direction : BlockFinder.DIRECTIONS) {
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

    public static final class S3 {
        public static final List<BlockPos> posList;

        static {
            posList = Collections.unmodifiableList(new ArrayList<>(get()));
        }

        private static LinkedHashSet<BlockPos> get() {
            final BlockPos pO = BlockPos.ORIGIN;
            final LinkedHashSet<BlockPos> posSet = new LinkedHashSet<>(S2.posList);
            final LinkedHashSet<BlockPos> tmpPosSet = new LinkedHashSet<>();
            for (int i = 2; i < 3; ++i) {
                for (BlockPos pos : posSet) {
                    if (BlockUtils.getDistance(pO, pos) == i) {
                        for (Direction direction : BlockFinder.DIRECTIONS) {
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

    public static final class S4 {
        public static final List<BlockPos> posList;

        static {
            posList = Collections.unmodifiableList(new ArrayList<>(get()));
        }

        private static LinkedHashSet<BlockPos> get() {
            final BlockPos pO = BlockPos.ORIGIN;
            final LinkedHashSet<BlockPos> posSet = new LinkedHashSet<>(S3.posList);
            final LinkedHashSet<BlockPos> tmpPosSet = new LinkedHashSet<>();
            for (int i = 3; i < 4; ++i) {
                for (BlockPos pos : posSet) {
                    if (BlockUtils.getDistance(pO, pos) == i) {
                        for (Direction direction : BlockFinder.DIRECTIONS) {
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
