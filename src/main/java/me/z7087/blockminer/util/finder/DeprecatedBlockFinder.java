//package me.z7087.blockminer.util.finder;
//
//import me.z7087.blockminer.util.BlockUtils;
//import me.z7087.blockminer.util.data.Pair;
//import me.z7087.blockminer.util.data.PistonPowerInfo;
//import me.z7087.blockminer.api.enums.PowerBlockType;
//import net.minecraft.block.*;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.Direction;
//import net.minecraft.world.World;
//
//import java.util.*;
//
//@Deprecated
//public final class DeprecatedBlockFinder {
//    private DeprecatedBlockFinder() {}
//
//    public static final Direction[] DIRECTIONS = Direction.values();
//    public static final Direction[][] DIRECTIONS_WITHOUT;
//    public static final Direction[][] POSSIBLE_FACE_DIRECTIONS;
//    static {
//        Direction[] DIRECTIONS = DeprecatedBlockFinder.DIRECTIONS;
//        final int DirectionsCount = DIRECTIONS.length;
//        DIRECTIONS_WITHOUT = new Direction[DirectionsCount][DirectionsCount - 1];
//        for (int i = 0; i < DirectionsCount; ++i) {
//            Direction direction = DIRECTIONS[i];
//            for (int j = 0, k = 0; j < DirectionsCount; j++) {
//                Direction anotherDirection = DIRECTIONS[j];
//                if (anotherDirection != direction) {
//                    DIRECTIONS_WITHOUT[i][k++] = anotherDirection;
//                }
//            }
//        }
//        POSSIBLE_FACE_DIRECTIONS = new Direction[DirectionsCount][DirectionsCount - 1];
//        for (Direction direction : DIRECTIONS) {
//            POSSIBLE_FACE_DIRECTIONS[direction.getOpposite().ordinal()] = DIRECTIONS_WITHOUT[direction.ordinal()];
//        }
//    }
//
//    private static final Direction[] DIRECTIONS_WITHOUT_DOWN = DIRECTIONS_WITHOUT[Direction.DOWN.ordinal()];
//    private static final Direction[] DIRECTIONS_WITHOUT_UP = DIRECTIONS_WITHOUT[Direction.UP.ordinal()];
//
//    public static void findStablePistons(World world, BlockPos targetPos, List<Pair<BlockPos, Direction>> possiblePistonLocations) {
//        BlockState stoneState = Blocks.STONE.getDefaultState();
//        for (int i = 0, length = DIRECTIONS.length - 1; i <= length; ++i) {
//            final Direction back = DIRECTIONS[i];
//            final BlockPos pistonPos = targetPos.offset(back);
//            if (world.isInBuildLimit(pistonPos)
//                    && BlockUtils.isReplaceable(world.getBlockState(pistonPos))
//                    && world.canPlace(stoneState, pistonPos, ShapeContext.absent())
//            ) {
//                for (int j = 0; j < length; ++j) {
//                    final Direction face = POSSIBLE_FACE_DIRECTIONS[i][j];
//                    final BlockPos pistonHeadPos = pistonPos.offset(face);
//                    if (world.isInBuildLimit(pistonHeadPos)
//                            && BlockUtils.isReplaceable(world.getBlockState(pistonHeadPos))
//                            // 防止玩家被活塞推动
//                            && world.canPlace(stoneState, pistonHeadPos, ShapeContext.absent())
//                            && isPistonPlaceSafe(world, pistonPos, face)
//                    ) {
//                        possiblePistonLocations.add(Pair.of(pistonPos, face));
//                    }
//                }
//            }
//        }
//    }
//
//    public static boolean isPistonPlaceSafe(World world, BlockPos pistonPos, Direction face) {
//        for (Direction direction : DIRECTIONS_WITHOUT[face.ordinal()]) {
//            if (world.isEmittingRedstonePower(pistonPos.offset(direction), direction)) {
//                return false;
//            }
//        }
//        if (world.isEmittingRedstonePower(pistonPos, Direction.DOWN)) {
//            return false;
//        }
//        final BlockPos pistonQCPos = pistonPos.up();
//        for (Direction direction : DIRECTIONS_WITHOUT_DOWN) {
//            if (world.isEmittingRedstonePower(pistonQCPos.offset(direction), direction)) {
//                return false;
//            }
//        }
//
//        return true;
//    }
//
//    // 这个方法比较耗时，建议缓存结果
//    public static void findPowerBlockForPiston(World world,
//                                               BlockPos targetPos,
//                                               PowerBlockType powerBlockUsage,
//                                               List<Pair<BlockPos, Direction>> possiblePistonLocations,
//                                               List<PistonPowerInfo> possiblePistonPowerInfos,
//                                               boolean hasDependBlock
//    ) {
//        final boolean isRedstoneTorch = powerBlockUsage.isRedstoneTorch();
//        final boolean isLever = powerBlockUsage.isLever();
//        final LinkedHashMap<PistonPowerInfo, PistonPowerInfo> deduplicationMapSolidDependBlock = new LinkedHashMap<>();
//        final LinkedHashMap<PistonPowerInfo, PistonPowerInfo> deduplicationMapReplaceableDependBlock = hasDependBlock ? new LinkedHashMap<>() : null;
//        for (Pair<BlockPos, Direction> location : possiblePistonLocations) {
//            BlockPos pistonPos = location.first;
//            Direction pistonFace = location.second;
//            BlockPos pistonHeadPos = pistonPos.offset(pistonFace);
//            for (Direction direction : DIRECTIONS) {
//                BlockPos powerBlockPos = pistonPos.offset(direction);
//                findPowerBlockForPistonInternal(deduplicationMapSolidDependBlock, deduplicationMapReplaceableDependBlock, world, powerBlockPos, pistonPos, pistonHeadPos, isRedstoneTorch, isLever, pistonFace);
//                // QC
//                powerBlockPos = powerBlockPos.up();
//                findPowerBlockForPistonInternal(deduplicationMapSolidDependBlock, deduplicationMapReplaceableDependBlock, world, powerBlockPos, pistonPos, pistonHeadPos, isRedstoneTorch, isLever, pistonFace);
//            }
//        }
//        possiblePistonPowerInfos.addAll(deduplicationMapSolidDependBlock.values());
//        if (deduplicationMapReplaceableDependBlock != null)
//            possiblePistonPowerInfos.addAll(deduplicationMapReplaceableDependBlock.values());
//    }
//
//    private static void findPowerBlockForPistonInternal(Map<PistonPowerInfo, PistonPowerInfo> deduplicationMapSolidDependBlock, Map<PistonPowerInfo, PistonPowerInfo> deduplicationMapReplaceableDependBlock, World world, BlockPos powerBlockPos, BlockPos pistonPos, BlockPos pistonHeadPos, boolean isRedstoneTorch, boolean isLever, Direction pistonFace) {
//        if (!powerBlockPos.equals(pistonPos)
//                && !powerBlockPos.equals(pistonHeadPos)
//                && world.isInBuildLimit(powerBlockPos)
//        ) {
//            BlockState powerBlockPosState = world.getBlockState(powerBlockPos);
//            if (BlockUtils.isReplaceable(powerBlockPosState)) {
//                for (Direction dependDirection : DIRECTIONS) {
//                    BlockPos dependBlockPos = powerBlockPos.offset(dependDirection);
//                    if (!dependBlockPos.equals(pistonPos)
//                            && !dependBlockPos.equals(pistonHeadPos)
//                            && world.isInBuildLimit(dependBlockPos)) {
//                        findPowerBlockForPistonInternal2(deduplicationMapSolidDependBlock, deduplicationMapReplaceableDependBlock, world, dependDirection, dependBlockPos, isRedstoneTorch && !dependBlockPos.equals(pistonPos.up()), powerBlockPos, isLever, pistonPos, pistonFace);
//                    }
//                }
//            } else if (powerBlockPosState.isSolidBlock(world, powerBlockPos)) {
//                if (isLever) {
//                    // 原powerBlock现在是拉杆的依赖方块
//                    for (Direction facing : DIRECTIONS) {
//                        BlockPos actualPowerBlockPos = powerBlockPos.offset(facing);
//                        if (!actualPowerBlockPos.equals(pistonPos)
//                                && !actualPowerBlockPos.equals(pistonHeadPos)
//                                && world.isInBuildLimit(actualPowerBlockPos)
//                                && powerBlockPosState.isSideSolidFullSquare(world, powerBlockPos, facing)
//                                && BlockUtils.isReplaceable(world.getBlockState(actualPowerBlockPos))
//                        ) {
//                            findPowerBlockForPistonInternal2(deduplicationMapSolidDependBlock, deduplicationMapReplaceableDependBlock, world, facing.getOpposite(), powerBlockPos, false, actualPowerBlockPos, true, pistonPos, pistonFace);
//                        }
//                    }
//                }
//                if (isRedstoneTorch) {
//                    // 原powerBlock现在是红石火把上方的红石导体
//                    BlockPos actualPowerBlockPos = powerBlockPos.offset(Direction.DOWN);
//                    if (!actualPowerBlockPos.equals(pistonPos)
//                            && !actualPowerBlockPos.equals(pistonHeadPos)
//                            && world.isInBuildLimit(actualPowerBlockPos)
//                            && BlockUtils.isReplaceable(world.getBlockState(actualPowerBlockPos))
//                    ) {
//                        for (Direction dependDirection : DIRECTIONS_WITHOUT_UP) {
//                            BlockPos dependBlockPos = actualPowerBlockPos.offset(dependDirection);
//                            BlockState dependBlockState;
//                            if (!dependBlockPos.equals(pistonPos)
//                                    && !dependBlockPos.equals(pistonHeadPos)
//                                    && world.isInBuildLimit(dependBlockPos)
//                                    && (BlockUtils.isReplaceable(dependBlockState = world.getBlockState(dependBlockPos)) || (dependBlockState.isSolidBlock(world, dependBlockPos) && dependBlockState.isSideSolidFullSquare(world, dependBlockPos, dependDirection.getOpposite())))
//                            ) {
//                                findPowerBlockForPistonInternal2(deduplicationMapSolidDependBlock, deduplicationMapReplaceableDependBlock, world, dependDirection, dependBlockPos, true, actualPowerBlockPos, false, pistonPos, pistonFace);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private static void findPowerBlockForPistonInternal2(
//            Map<PistonPowerInfo, PistonPowerInfo> deduplicationMapSolidDependBlock,
//            Map<PistonPowerInfo, PistonPowerInfo> deduplicationMapReplaceableDependBlock,
//            World world,
//            Direction dependDirection,
//            BlockPos dependBlockPos,
//            boolean isRedstoneTorch,
//            BlockPos powerBlockPos,
//            boolean isLever,
//            BlockPos pistonPos,
//            Direction pistonFace
//    ) {
//        BlockState dependBlockState = world.getBlockState(dependBlockPos);
//        final boolean dependBlockIsReplaceable = BlockUtils.isReplaceable(dependBlockState);
//        if ((dependBlockIsReplaceable && world.canPlace(Blocks.STONE.getDefaultState(), dependBlockPos, ShapeContext.absent())) || (dependBlockState.isSolidBlock(world, dependBlockPos) && dependBlockState.isSideSolidFullSquare(world, dependBlockPos, dependDirection.getOpposite()))) {
//            boolean redstoneTorch = false;
//            boolean lever = false;
//            // 找到能源方块和其附着方向，检查会不会干扰其他task或被其他方块干扰
//            if (isRedstoneTorch) {
//                check:
//                {
//                    // 红石火把不能附着在上方的方块
//                    if (dependDirection == Direction.UP)
//                        break check;
//                    // 把这个检测移到前面了，看看有没有用
//                    /*
//                    // 如果红石火把依附在目标活塞上（前面判断过），
//                    // 或者依附在目标活塞的正上方的方块上，
//                    // 或者依附在目标活塞紧挨着的方块上且红石火把是斜插着，无法激活目标活塞
//                    for (Direction direction : DIRECTIONS) {
//                        if (dependBlockPos.equals(pistonPos.offset(direction))) {
//                            if (direction == Direction.UP) {
//                                break check;
//                            } else {
//                                if (dependDirection != Direction.DOWN) {
//                                    break check;
//                                }
//                            }
//                            break;
//                        }
//                    }
//                     */
//                    // 如果有其他能源方块正在激活附着方块，不能放置
//                    if (world.getReceivedStrongRedstonePower(dependBlockPos) > 0)
//                        break check;
//
//                    // 如果红石火把上面有固体方块，且固体方块旁边有附着在上面的红石火把或活塞，不能放置
//                    {
//                        BlockPos powerBlockPosUp = powerBlockPos.up();
//                        BlockState powerBlockStateUp = world.getBlockState(powerBlockPosUp);
//                        if (powerBlockStateUp.isSolidBlock(world, powerBlockPosUp)) {
//                            for (Direction direction1 : DIRECTIONS) {
//                                BlockPos pos = powerBlockPosUp.offset(direction1);
//                                BlockState blockState = world.getBlockState(pos);
//                                Block block = blockState.getBlock();
//                                if (block instanceof RedstoneTorchBlock) {
//                                    Direction direction2;
//                                    if (block instanceof WallRedstoneTorchBlock) {
//                                        direction2 = blockState.get(WallRedstoneTorchBlock.FACING).getOpposite();
//                                    } else {
//                                        direction2 = Direction.UP;
//                                    }
//                                    if (direction2 == direction1)
//                                        break check;
//                                } else if (block instanceof PistonBlock) {
//                                    break check;
//                                }
//                                block = world.getBlockState(pos.down()).getBlock();
//                                if (block instanceof PistonBlock) {
//                                    break check;
//                                }
//                            }
//                        }
//                    }
//                    // 如果红石火把能不依赖头顶的方块激活其他活塞，不能放置
//                    for (Direction direction1 : DIRECTIONS_WITHOUT[dependDirection.ordinal()]) {
//                        BlockPos pos = powerBlockPos.offset(direction1);
//                        if (world.getBlockState(pos).getBlock() instanceof PistonBlock
//                                || world.getBlockState(pos.down()).getBlock() instanceof PistonBlock
//                        )
//                            break check;
//                    }
//                    redstoneTorch = true;
//                }
//            }
//            if (isLever) {
//                check:
//                {
//                    // 如果拉杆更新不到活塞，不能放置
//                    if (BlockUtils.getDistance(pistonPos, powerBlockPos) > 1
//                            && BlockUtils.getDistance(pistonPos, dependBlockPos) > 1)
//                        break check;
//                    // 如果拉杆所附着的方块是固体方块，且固体方块旁边有附着在上面的红石火把或活塞，不能放置
//                    if (dependBlockState.isSolidBlock(world, dependBlockPos)) {
//                        for (Direction direction1 : DIRECTIONS) {
//                            BlockPos pos = dependBlockPos.offset(direction1);
//                            BlockState blockState = world.getBlockState(pos);
//                            Block block = blockState.getBlock();
//                            if (block instanceof RedstoneTorchBlock) {
//                                Direction direction2;
//                                if (block instanceof WallRedstoneTorchBlock) {
//                                    direction2 = blockState.get(WallRedstoneTorchBlock.FACING).getOpposite();
//                                } else {
//                                    direction2 = Direction.UP;
//                                }
//                                if (direction2 == direction1)
//                                    break check;
//                            } else if (block instanceof PistonBlock) {
//                                break check;
//                            }
//                            block = world.getBlockState(pos.down()).getBlock();
//                            if (block instanceof PistonBlock) {
//                                break check;
//                            }
//                        }
//                    }
//                    // 如果拉杆能不依赖所附着的方块激活其他活塞，不能放置
//                    for (Direction direction1 : DIRECTIONS) {
//                        BlockPos pos = powerBlockPos.offset(direction1);
//                        if (world.getBlockState(pos).getBlock() instanceof PistonBlock
//                                || world.getBlockState(pos.down()).getBlock() instanceof PistonBlock
//                        )
//                            break check;
//                    }
//                    lever = true;
//                }
//            }
//            final PowerBlockType type = PowerBlockType.of(redstoneTorch, lever);
//            if (type != null) {
//                final Map<PistonPowerInfo, PistonPowerInfo> deduplicationMap = dependBlockIsReplaceable ? deduplicationMapReplaceableDependBlock : deduplicationMapSolidDependBlock;
//                if (deduplicationMap != null) {
//                    PistonPowerInfo pistonPowerInfo = PistonPowerInfo.of(
//                            pistonPos, pistonFace, powerBlockPos,
//                            dependDirection.getOpposite(), type);
//                    final PistonPowerInfo oldPistonPowerInfo = deduplicationMap.get(pistonPowerInfo);
//                    if (oldPistonPowerInfo != null) {
//                        PowerBlockType oldType = oldPistonPowerInfo.getPowerBlockType();
//                        PowerBlockType mergedType = PowerBlockType.merge(type, oldType);
//                        if (mergedType != oldType) {
//                            pistonPowerInfo = PistonPowerInfo.of(pistonPos, pistonFace,
//                                    powerBlockPos, dependDirection.getOpposite(), mergedType);
//                            deduplicationMap.put(pistonPowerInfo, pistonPowerInfo);
//                        }
//                    } else {
//                        deduplicationMap.put(pistonPowerInfo, pistonPowerInfo);
//                    }
//                }
//            }
//        }
//    }
//}
