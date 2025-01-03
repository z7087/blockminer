package me.z7087.blockminer.util;

import me.z7087.blockminer.util.data.Pair;
import me.z7087.blockminer.util.data.PistonPowerInfo;
import me.z7087.blockminer.util.enums.PowerBlockType;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RedstoneView;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockFinder {
    private BlockFinder() {}

    public static final Direction[] DIRECTIONS = Direction.values();
    private static final Direction[][] POSSIBLE_FACE_DIRECTIONS;
    static {
        Direction[] directions = Direction.values();
        POSSIBLE_FACE_DIRECTIONS = new Direction[directions.length][directions.length - 1];
        for (int i = 0, length = directions.length; i < length; ++i) {
            Direction back = directions[i].getOpposite();
            for (int j = 0, k = 0; j < length; j++) {
                Direction face = directions[j];
                if (face != back) {
                    POSSIBLE_FACE_DIRECTIONS[i][k++] = face;
                }
            }
        }
    }
    private static final Direction[] DIRECTIONS_WITHOUT_DOWN = POSSIBLE_FACE_DIRECTIONS[Direction.DOWN.getOpposite().ordinal()];
    private static final Direction[] DIRECTIONS_WITHOUT_UP = POSSIBLE_FACE_DIRECTIONS[Direction.UP.getOpposite().ordinal()];

    public static void findStablePistons(World world, BlockPos targetPos, List<Pair<BlockPos, Direction>> possiblePistonLocations) {
        BlockState stoneState = Blocks.STONE.getDefaultState();
        for (int i = 0, length = DIRECTIONS.length - 1; i <= length; ++i) {
            final Direction back = DIRECTIONS[i];
            final BlockPos pistonPos = targetPos.offset(back);
            if (world.isInBuildLimit(pistonPos)
                    && world.getBlockState(pistonPos).isReplaceable()
                    && world.canPlace(stoneState, pistonPos, ShapeContext.absent())
            ) {
                for (int j = 0; j < length; ++j) {
                    final Direction face = POSSIBLE_FACE_DIRECTIONS[i][j];
                    final BlockPos pistonHeadPos = pistonPos.offset(face);
                    if (world.isInBuildLimit(pistonHeadPos)
                            && world.getBlockState(pistonHeadPos).isReplaceable()
                            && isPistonPlaceSafe(world, pistonPos, face)
                    ) {
                        possiblePistonLocations.add(Pair.of(pistonPos, face));
                    }
                }
            }
        }
    }

    public static boolean isPistonPlaceSafe(RedstoneView world, BlockPos pistonPos, Direction face) {
        for (Direction direction : POSSIBLE_FACE_DIRECTIONS[face.getOpposite().ordinal()]) {
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

    // 这个方法比较耗时，建议缓存结果
    public static void findPowerBlockForPiston(World world,
                                               BlockPos targetPos,
                                               PowerBlockType powerBlockUsage,
                                               List<Pair<BlockPos, Direction>> possiblePistonLocations,
                                               List<PistonPowerInfo> possiblePistonPowerInfos
    ) {
        final boolean isRedstoneTorch = powerBlockUsage.isRedstoneTorch();
        final boolean isLever = powerBlockUsage.isLever();
        final LinkedHashMap<PistonPowerInfo, PistonPowerInfo> deduplicationMapSolidDependBlock = new LinkedHashMap<>();
        final LinkedHashMap<PistonPowerInfo, PistonPowerInfo> deduplicationMapReplaceableDependBlock = new LinkedHashMap<>();
        for (Pair<BlockPos, Direction> location : possiblePistonLocations) {
            BlockPos pistonPos = location.first;
            Direction pistonFace = location.second;
            BlockPos pistonHeadPos = pistonPos.offset(pistonFace);
            for (Direction direction : DIRECTIONS) {
                BlockPos powerBlockPos = pistonPos.offset(direction);
                findPowerBlockForPistonInternal(deduplicationMapSolidDependBlock, deduplicationMapReplaceableDependBlock, world, powerBlockPos, pistonPos, pistonHeadPos, isRedstoneTorch, isLever, pistonFace);
                powerBlockPos = powerBlockPos.up();
                findPowerBlockForPistonInternal(deduplicationMapSolidDependBlock, deduplicationMapReplaceableDependBlock, world, powerBlockPos, pistonPos, pistonHeadPos, isRedstoneTorch, isLever, pistonFace);
            }
        }
        possiblePistonPowerInfos.addAll(deduplicationMapSolidDependBlock.values());
        possiblePistonPowerInfos.addAll(deduplicationMapReplaceableDependBlock.values());
    }

    private static void findPowerBlockForPistonInternal(Map<PistonPowerInfo, PistonPowerInfo> deduplicationMapSolidDependBlock, Map<PistonPowerInfo, PistonPowerInfo> deduplicationMapReplaceableDependBlock, World world, BlockPos powerBlockPos, BlockPos pistonPos, BlockPos pistonHeadPos, boolean isRedstoneTorch, boolean isLever, Direction pistonFace) {
        if (!powerBlockPos.equals(pistonPos)
                && !powerBlockPos.equals(pistonHeadPos)
                && world.isInBuildLimit(powerBlockPos)
        ) {
            BlockState state = world.getBlockState(powerBlockPos);
            if (state.isReplaceable()) {
                for (Direction dependDirection : DIRECTIONS) {
                    BlockPos dependBlockPos = powerBlockPos.offset(dependDirection);
                    if (!dependBlockPos.equals(pistonPos)
                            && !dependBlockPos.equals(pistonHeadPos)
                            && world.isInBuildLimit(dependBlockPos)) {
                        findPowerBlockForPistonInternal2(deduplicationMapSolidDependBlock, deduplicationMapReplaceableDependBlock, world, dependDirection, dependBlockPos, isRedstoneTorch && !dependBlockPos.equals(pistonPos.up()), powerBlockPos, isLever, pistonPos, pistonFace);
                    }
                }
            } else if (state.isSolidBlock(world, powerBlockPos) && state.isFullCube(world, powerBlockPos)) {
                for (Direction dependDirection : DIRECTIONS) {
                    BlockPos actualPowerBlockPos = powerBlockPos.offset(dependDirection);
                    if (!actualPowerBlockPos.equals(pistonPos)
                            && !actualPowerBlockPos.equals(pistonHeadPos)
                            && world.isInBuildLimit(actualPowerBlockPos)
                            && world.getBlockState(actualPowerBlockPos).isReplaceable()
                    ) {
                        findPowerBlockForPistonInternal2(deduplicationMapSolidDependBlock, deduplicationMapReplaceableDependBlock, world, dependDirection.getOpposite(), powerBlockPos, false, actualPowerBlockPos, isLever, pistonPos, pistonFace);
                    }
                }
            }
        }
    }

    private static void findPowerBlockForPistonInternal2(Map<PistonPowerInfo, PistonPowerInfo> deduplicationMapSolidDependBlock, Map<PistonPowerInfo, PistonPowerInfo> deduplicationMapReplaceableDependBlock, World world, Direction dependDirection, BlockPos dependBlockPos, boolean isRedstoneTorch, BlockPos powerBlockPos, boolean isLever, BlockPos pistonPos, Direction pistonFace) {
        BlockState dependBlockState = world.getBlockState(dependBlockPos);
        final boolean dependBlockIsReplaceable = dependBlockState.isReplaceable();
        if ((dependBlockIsReplaceable && world.canPlace(Blocks.STONE.getDefaultState(), dependBlockPos, ShapeContext.absent())) || (Block.sideCoversSmallSquare(world, dependBlockPos, dependDirection.getOpposite()) && !(dependBlockState.getBlock() instanceof PistonBlock))) {
            boolean redstoneTorch = false;
            boolean lever = false;
            // 找到能源方块和其附着方向，检查会不会干扰其他task或被其他方块干扰
            if (isRedstoneTorch) {
                check:
                {
                    // 红石火把不能附着在上方的方块
                    if (dependDirection == Direction.UP)
                        break check;
                    // 把这个检测移到前面了，看看有没有用
                    /*
                    // 如果红石火把依附在目标活塞上（前面判断过），
                    // 或者依附在目标活塞的正上方的方块上，
                    // 或者依附在目标活塞紧挨着的方块上且红石火把是斜插着，无法激活目标活塞
                    for (Direction direction : DIRECTIONS) {
                        if (dependBlockPos.equals(pistonPos.offset(direction))) {
                            if (direction == Direction.UP) {
                                break check;
                            } else {
                                if (dependDirection != Direction.DOWN) {
                                    break check;
                                }
                            }
                            break;
                        }
                    }
                     */
                    // 如果有其他能源方块正在激活附着方块，不能放置
                    if (world.getReceivedStrongRedstonePower(dependBlockPos) > 0)
                        break check;

                    // 如果红石火把上面有完整固体方块，且固体方块旁边有附着在上面的红石火把或活塞，不能放置
                    {
                        BlockPos powerBlockPosUp = powerBlockPos.up();
                        BlockState powerBlockStateUp = world.getBlockState(powerBlockPosUp);
                        if (powerBlockStateUp.isSolidBlock(world, powerBlockPosUp)
                                && powerBlockStateUp.isFullCube(world, powerBlockPosUp)) {
                            for (Direction direction1 : DIRECTIONS) {
                                BlockPos pos = powerBlockPosUp.offset(direction1);
                                BlockState blockState = world.getBlockState(pos);
                                Block block = blockState.getBlock();
                                if (block instanceof RedstoneTorchBlock) {
                                    Direction direction2;
                                    if (block instanceof WallRedstoneTorchBlock) {
                                        direction2 = blockState.get(WallRedstoneTorchBlock.FACING).getOpposite();
                                    } else {
                                        direction2 = Direction.UP;
                                    }
                                    if (direction2 == direction1)
                                        break check;
                                } else if (block instanceof PistonBlock) {
                                    break check;
                                }
                                block = world.getBlockState(pos.down()).getBlock();
                                if (block instanceof PistonBlock) {
                                    break check;
                                }
                            }
                        }
                    }
                    // 如果红石火把能不依赖头顶的方块激活其他活塞，不能放置
                    for (Direction direction1 : POSSIBLE_FACE_DIRECTIONS[dependDirection.getOpposite().ordinal()]) {
                        BlockPos pos = powerBlockPos.offset(direction1);
                        if (world.getBlockState(pos).getBlock() instanceof PistonBlock
                                || world.getBlockState(pos.down()).getBlock() instanceof PistonBlock
                        )
                            break check;
                    }
                    redstoneTorch = true;
                }
            }
            if (isLever) {
                check:
                {
                    // 如果拉杆所附着的方块是完整固体方块，且固体方块旁边有附着在上面的红石火把或活塞，不能放置
                    if (dependBlockState.isSolidBlock(world, dependBlockPos)
                            && dependBlockState.isFullCube(world, dependBlockPos)) {
                        for (Direction direction1 : DIRECTIONS) {
                            BlockPos pos = dependBlockPos.offset(direction1);
                            BlockState blockState = world.getBlockState(pos);
                            Block block = blockState.getBlock();
                            if (block instanceof RedstoneTorchBlock) {
                                Direction direction2;
                                if (block instanceof WallRedstoneTorchBlock) {
                                    direction2 = blockState.get(WallRedstoneTorchBlock.FACING).getOpposite();
                                } else {
                                    direction2 = Direction.UP;
                                }
                                if (direction2 == direction1)
                                    break check;
                            } else if (block instanceof PistonBlock) {
                                break check;
                            }
                            block = world.getBlockState(pos.down()).getBlock();
                            if (block instanceof PistonBlock) {
                                break check;
                            }
                        }
                    }
                    // 如果拉杆能不依赖所附着的方块激活其他活塞，不能放置
                    for (Direction direction1 : POSSIBLE_FACE_DIRECTIONS[dependDirection.getOpposite().ordinal()]) {
                        BlockPos pos = powerBlockPos.offset(direction1);
                        if (world.getBlockState(pos).getBlock() instanceof PistonBlock
                                || world.getBlockState(pos.down()).getBlock() instanceof PistonBlock
                        )
                            break check;
                    }
                    lever = true;
                }
            }
            final PowerBlockType type = PowerBlockType.of(redstoneTorch, lever);
            if (type != null) {
                final Map<PistonPowerInfo, PistonPowerInfo> deduplicationMap = dependBlockIsReplaceable ? deduplicationMapReplaceableDependBlock : deduplicationMapSolidDependBlock;
                PistonPowerInfo pistonPowerInfo = PistonPowerInfo.of(
                        pistonPos, pistonFace, powerBlockPos,
                        dependDirection.getOpposite(), type);
                final PistonPowerInfo oldPistonPowerInfo = deduplicationMap.get(pistonPowerInfo);
                if (oldPistonPowerInfo != null) {
                    PowerBlockType oldType = oldPistonPowerInfo.getPowerBlockType();
                    PowerBlockType mergedType = PowerBlockType.merge(type, oldType);
                    if (mergedType != oldType) {
                        pistonPowerInfo = PistonPowerInfo.of(pistonPos, pistonFace,
                                powerBlockPos, dependDirection.getOpposite(), mergedType);
                        deduplicationMap.put(pistonPowerInfo, pistonPowerInfo);
                    }
                } else {
                    deduplicationMap.put(pistonPowerInfo, pistonPowerInfo);
                }
            }
        }
    }
}
