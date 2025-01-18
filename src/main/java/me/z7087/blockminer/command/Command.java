package me.z7087.blockminer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.command.argument.BlockPosArgumentType;
import me.z7087.blockminer.config.Config;
import me.z7087.blockminer.util.enums.DistanceCalculationMode;
import me.z7087.blockminer.util.enums.PowerBlockType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class Command {
    //#if MC >= 11900
    private final net.minecraft.command.CommandRegistryAccess registryAccess;
    private Command(net.minecraft.command.CommandRegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
    }
    //#else
    //$$ private Command() {}
    //#endif

    public static void load() {
        //#if MC >= 11900
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> new Command(registryAccess).registerInternal(dispatcher));
        //#else
        //$$ new Command().registerInternal(net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.DISPATCHER);
        //#endif
    }

    private BlockStateArgumentType getBlockStateArgumentType() {
        //#if MC >= 11900
        return BlockStateArgumentType.blockState(registryAccess);
        //#else
        //$$ return BlockStateArgumentType.blockState();
        //#endif
    }

    private void registerInternal(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        final LiteralArgumentBuilder<FabricClientCommandSource> builder = literal("blockminer");
        builder
                .then(literal("toggle")
                        .executes((context) -> {
                            BlockMinerMod.INSTANCE.taskManager.toggle();
                            return 1;
                        })
                ).then(literal("config")
                        .then(literal("reload")
                                .executes(context -> {
                                    Config config = Config.loadFromFile();
                                    if (config != null) {
                                        BlockMinerMod.INSTANCE.config = config;
                                        return 1;
                                    }
                                    return 0;
                                })
                        ).then(literal("save")
                                .executes(context -> {
                                    try {
                                        Config.saveToFile(BlockMinerMod.INSTANCE.config);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return 1;
                                })
                        ).then(literal("reset")
                                .executes(context -> {
                                    BlockMinerMod.INSTANCE.config = Config.createDefaultConfig();
                                    BlockMinerMod.INSTANCE.tryToSaveConfig();
                                    return 1;
                                })
                        )
                ).then(literal("debug")
                        .executes((context) -> {
                            context.getSource().sendFeedback(Text.of(String.valueOf(BlockMinerMod.INSTANCE.config.debug)));
                            return 1;
                        })
                        .then(
                                argument("bool", BoolArgumentType.bool())
                                        .executes(context -> {
                                            Config config = BlockMinerMod.INSTANCE.config;
                                            boolean input = BoolArgumentType.getBool(context, "bool");
                                            if (config.debug != input) {
                                                config.debug = input;
                                                BlockMinerMod.INSTANCE.tryToSaveConfig();
                                                return 1;
                                            }
                                            context.getSource().sendFeedback(Text.of("debug already set to " + input));
                                            return 0;
                                        })
                        )
                ).then(literal("headless-piston-mode")
                        .executes((context) -> {
                            context.getSource().sendFeedback(Text.of(String.valueOf(BlockMinerMod.INSTANCE.config.headlessPistonMode)));
                            return 1;
                        })
                        .then(
                                argument("bool", BoolArgumentType.bool())
                                        .executes(context -> {
                                            Config config = BlockMinerMod.INSTANCE.config;
                                            boolean input = BoolArgumentType.getBool(context, "bool");
                                            if (config.headlessPistonMode != input) {
                                                config.headlessPistonMode = input;
                                                BlockMinerMod.INSTANCE.tryToSaveConfig();
                                                return 1;
                                            }
                                            context.getSource().sendFeedback(Text.of("headless-piston-mode already set to " + input));
                                            return 0;
                                        })
                        )
                ).then(literal("ping-spike-threshold")
                        .executes((context) -> {
                            context.getSource().sendFeedback(Text.of(String.valueOf(BlockMinerMod.INSTANCE.config.pingSpikeThreshold)));
                            return 1;
                        })
                        .then(
                                argument("integer", IntegerArgumentType.integer(0, 1200))
                                        .executes(context -> {
                                            Config config = BlockMinerMod.INSTANCE.config;
                                            int input = IntegerArgumentType.getInteger(context, "integer");
                                            if (config.pingSpikeThreshold != input) {
                                                config.pingSpikeThreshold = input;
                                                BlockMinerMod.INSTANCE.tryToSaveConfig();
                                                return 1;
                                            }
                                            context.getSource().sendFeedback(Text.of("ping-spike-threshold already set to " + input));
                                            return 0;
                                        })
                        )
                ).then(literal("power-block-usage")
                        .executes((context) -> {
                            context.getSource().sendFeedback(Text.of(BlockMinerMod.INSTANCE.config.powerBlockUsage.toString()));
                            return 1;
                        })
                        .then(literal("redstone-torch")
                                .executes(context -> {
                                    Config config = BlockMinerMod.INSTANCE.config;
                                    if (config.powerBlockUsage != PowerBlockType.RedstoneTorch) {
                                        config.powerBlockUsage = PowerBlockType.RedstoneTorch;
                                        BlockMinerMod.INSTANCE.tryToSaveConfig();
                                        return 1;
                                    }
                                    context.getSource().sendFeedback(Text.of("ping-spike-threshold already set to \"redstone-torch\""));
                                    return 0;
                                })
                        ).then(literal("lever")
                                .executes(context -> {
                                    Config config = BlockMinerMod.INSTANCE.config;
                                    if (config.powerBlockUsage != PowerBlockType.Lever) {
                                        config.powerBlockUsage = PowerBlockType.Lever;
                                        BlockMinerMod.INSTANCE.tryToSaveConfig();
                                        return 1;
                                    }
                                    context.getSource().sendFeedback(Text.of("ping-spike-threshold already set to \"lever\""));
                                    return 0;
                                })
                        ).then(literal("both")
                                .executes(context -> {
                                    Config config = BlockMinerMod.INSTANCE.config;
                                    if (config.powerBlockUsage != PowerBlockType.Both) {
                                        config.powerBlockUsage = PowerBlockType.Both;
                                        BlockMinerMod.INSTANCE.tryToSaveConfig();
                                        return 1;
                                    }
                                    context.getSource().sendFeedback(Text.of("ping-spike-threshold already set to \"both\""));
                                    return 0;
                                })
                        )
                ).then(literal("distance-calculation-mode")
                        .executes((context) -> {
                            context.getSource().sendFeedback(Text.of(BlockMinerMod.INSTANCE.config.distanceCalculationMode.toString()));
                            return 1;
                        })
                        .then(literal("old")
                                .executes(context -> {
                                    Config config = BlockMinerMod.INSTANCE.config;
                                    if (config.distanceCalculationMode != DistanceCalculationMode.Old) {
                                        config.distanceCalculationMode = DistanceCalculationMode.Old;
                                        BlockMinerMod.INSTANCE.tryToSaveConfig();
                                        return 1;
                                    }
                                    context.getSource().sendFeedback(Text.of("distance-calculation-mode already set to \"old\""));
                                    return 0;
                                })
                        ).then(literal("1.19")
                                .executes(context -> {
                                    Config config = BlockMinerMod.INSTANCE.config;
                                    if (config.distanceCalculationMode != DistanceCalculationMode.V1_19) {
                                        config.distanceCalculationMode = DistanceCalculationMode.V1_19;
                                        BlockMinerMod.INSTANCE.tryToSaveConfig();
                                        return 1;
                                    }
                                    context.getSource().sendFeedback(Text.of("distance-calculation-mode already set to \"1.19\""));
                                    return 0;
                                })
                        ).then(literal("1.20.6")
                                .executes(context -> {
                                    Config config = BlockMinerMod.INSTANCE.config;
                                    if (config.distanceCalculationMode != DistanceCalculationMode.V1_20_6) {
                                        config.distanceCalculationMode = DistanceCalculationMode.V1_20_6;
                                        BlockMinerMod.INSTANCE.tryToSaveConfig();
                                        return 1;
                                    }
                                    context.getSource().sendFeedback(Text.of("distance-calculation-mode already set to \"1.20.6\""));
                                    return 0;
                                })
                        )
                ).then(literal("target-block")
                        .then(literal("whitelist")
                                .executes((context) -> {
                                    context.getSource().sendFeedback(Text.of(BlockMinerMod.INSTANCE.config.blockWhitelist.toString()));
                                    return 1;
                                })
                                .then(literal("add")
                                        .then(
                                                argument("block", getBlockStateArgumentType())
                                                        .executes(context -> {
                                                            Config config = BlockMinerMod.INSTANCE.config;
                                                            Block input = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();
                                                            if (config.blockWhitelist.add(input)) {
                                                                BlockMinerMod.INSTANCE.tryToSaveConfig();
                                                                return 1;
                                                            }
                                                            context.getSource().sendFeedback(Text.of("target-block whitelist already has " + input));
                                                            return 0;
                                                        })
                                        )
                                ).then(literal("remove")
                                        .then(
                                                argument("block", getBlockStateArgumentType())
                                                        .executes(context -> {
                                                            Config config = BlockMinerMod.INSTANCE.config;
                                                            Block input = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();
                                                            if (config.blockWhitelist.remove(input)) {
                                                                BlockMinerMod.INSTANCE.tryToSaveConfig();
                                                                return 1;
                                                            }
                                                            context.getSource().sendFeedback(Text.of("target-block whitelist has no " + input));
                                                            return 0;
                                                        })
                                        )
                                )
                        )
                ).then(literal("depend-block")
                        .then(literal("whitelist")
                                .executes((context) -> {
                                    context.getSource().sendFeedback(Text.of(BlockMinerMod.INSTANCE.config.dependBlockWhitelistToString()));
                                    return 1;
                                })
                                .then(literal("add")
                                        .then(
                                                argument("block", getBlockStateArgumentType())
                                                        .executes(context -> {
                                                            Config config = BlockMinerMod.INSTANCE.config;
                                                            Block input = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();
                                                            if (config.dependBlockWhitelistAdd(input)) {
                                                                BlockMinerMod.INSTANCE.tryToSaveConfig();
                                                                return 1;
                                                            }
                                                            context.getSource().sendFeedback(Text.of("target-block whitelist already has " + input));
                                                            return 0;
                                                        })
                                        )
                                ).then(literal("remove")
                                        .then(
                                                argument("block", getBlockStateArgumentType())
                                                        .executes(context -> {
                                                            Config config = BlockMinerMod.INSTANCE.config;
                                                            Block input = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();
                                                            if (config.dependBlockWhitelistRemove(input)) {
                                                                BlockMinerMod.INSTANCE.tryToSaveConfig();
                                                                return 1;
                                                            }
                                                            context.getSource().sendFeedback(Text.of("target-block whitelist has no " + input));
                                                            return 0;
                                                        })
                                        )
                                )
                        )
                ).then(literal("area")
                        .then(argument("start", BlockPosArgumentType.blockPos())
                                .then(argument("end", BlockPosArgumentType.blockPos())
                                        .executes(context -> {
                                            if (!BlockMinerMod.INSTANCE.config.debug) {
                                                context.getSource().sendFeedback(Text.of("debug not enabled"));
                                                return 0;
                                            }
                                            final BlockPos start = BlockPosArgumentType.getBlockPos(context, "start");
                                            final BlockPos end = BlockPosArgumentType.getBlockPos(context, "end");
                                            return BlockMinerMod.INSTANCE.taskManager.addAura(start, end) ? 1 : 0;
                                        })
                                )
                        )
                )
        ;
        dispatcher.register(builder);
    }
}
