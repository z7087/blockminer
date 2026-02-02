package me.z7087.blockminer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.api.base.BaseConfig;
import me.z7087.blockminer.command.argument.BlockPosArgumentType;
import me.z7087.blockminer.config.Config;
import me.z7087.blockminer.api.enums.DistanceCalculationMode;
import me.z7087.blockminer.api.enums.PowerBlockType;
import me.z7087.blockminer.api.enums.SearchMode;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.function.*;

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

    private static LiteralArgumentBuilder<FabricClientCommandSource> boolConfigArgBuilder(
            String name,
            BooleanSupplier getter,
            BooleanConsumer setter
    ) {
        return literal(name)
                .executes((context) -> {
                    context.getSource().sendFeedback(Text.of(String.valueOf(getter.getAsBoolean())));
                    return 1;
                })
                .then(
                        argument("bool", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean input = BoolArgumentType.getBool(context, "bool");
                                    if (getter.getAsBoolean() != input) {
                                        setter.accept(input);
                                        BlockMinerMod.getInstance().tryToSaveConfig();
                                        return 1;
                                    }
                                    context.getSource().sendFeedback(Text.of("\"" + name + "\" already set to " + input));
                                    return 0;
                                })
                );
    }

    @SuppressWarnings("SameParameterValue")
    private static LiteralArgumentBuilder<FabricClientCommandSource> intConfigArgBuilder(
            String name,
            int min,
            int max,
            IntSupplier getter,
            IntConsumer setter
    ) {
        return literal(name)
                .executes((context) -> {
                    context.getSource().sendFeedback(Text.of(String.valueOf(getter.getAsInt())));
                    return 1;
                })
                .then(
                        argument("integer", IntegerArgumentType.integer(min, max))
                                .executes(context -> {
                                    int input = IntegerArgumentType.getInteger(context, "integer");
                                    if (getter.getAsInt() != input) {
                                        setter.accept(input);
                                        BlockMinerMod.getInstance().tryToSaveConfig();
                                        return 1;
                                    }
                                    context.getSource().sendFeedback(Text.of("\"" + name + "\" already set to " + input));
                                    return 0;
                                })
                );
    }


    private static <T> LiteralArgumentBuilder<FabricClientCommandSource> enumConfigArgBuilder(
            String name,
            T[] values,
            Supplier<T> getter,
            Consumer<T> setter
    ) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder =
                literal(name)
                        .executes((context) -> {
                            context.getSource().sendFeedback(Text.of(getter.get().toString()));
                            return 1;
                        });
        for (T enumValue : values) {
            builder = builder.then(
                    literal(enumValue.toString())
                            .executes(context -> {
                                if (!getter.get().equals(enumValue)) {
                                    setter.accept(enumValue);
                                    BlockMinerMod.getInstance().tryToSaveConfig();
                                    return 1;
                                }
                                context.getSource().sendFeedback(Text.of("\"" + name + "\" already set to \"" + enumValue + "\""));
                                return 0;
                            })
            );
        }
        return builder;
    }

    private void registerInternal(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        final LiteralArgumentBuilder<FabricClientCommandSource> builder = literal("blockminer");
        builder
                .then(literal("toggle")
                        .executes((context) -> {
                            BlockMinerMod.getInstance().getTaskManager().toggle();
                            return 1;
                        })
                ).then(literal("config")
                        .then(literal("reload")
                                .executes(context -> {
                                    Config config = Config.loadFromFile();
                                    if (config != null) {
                                        BlockMinerMod.getInstance().setConfig(config);
                                        return 1;
                                    }
                                    return 0;
                                })
                        ).then(literal("save")
                                .executes(context -> {
                                    try {
                                        Config.saveToFile(BlockMinerMod.getInstance().getConfig());
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return 1;
                                })
                        ).then(literal("reset")
                                .executes(context -> {
                                    BlockMinerMod.getInstance().setConfig(Config.createDefaultConfig());
                                    BlockMinerMod.getInstance().tryToSaveConfig();
                                    return 1;
                                })
                        )
                ).then(
                        boolConfigArgBuilder(
                                "debug",
                                () -> BlockMinerMod.getInstance().getConfig().isDebug(),
                                (value) -> BlockMinerMod.getInstance().getConfig().setDebug(value)
                        )
                ).then(
                        boolConfigArgBuilder(
                                "headless-piston-mode",
                                () -> BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode(),
                                (value) -> BlockMinerMod.getInstance().getConfig().setHeadlessPistonMode(value)
                        )
                ).then(
                        boolConfigArgBuilder(
                                "headless-piston-mode",
                                () -> BlockMinerMod.getInstance().getConfig().isHeadlessPistonMode(),
                                (value) -> BlockMinerMod.getInstance().getConfig().setHeadlessPistonMode(value)
                        )
                ).then(
                        boolConfigArgBuilder(
                                "blink-during-tasks-tick",
                                () -> BlockMinerMod.getInstance().getConfig().isBlinkDuringTasksTick(),
                                (value) -> BlockMinerMod.getInstance().getConfig().setBlinkDuringTasksTick(value)
                        )
                ).then(
                        boolConfigArgBuilder(
                                "auto-clear-after-task",
                                () -> BlockMinerMod.getInstance().getConfig().isAutoClearAfterTask(),
                                (value) -> BlockMinerMod.getInstance().getConfig().setAutoClearAfterTask(value)
                        )
                ).then(
                        intConfigArgBuilder(
                                "ping-spike-threshold",
                                0, 1200,
                                () -> BlockMinerMod.getInstance().getConfig().getPingSpikeThreshold(),
                                (value) -> BlockMinerMod.getInstance().getConfig().setPingSpikeThreshold(value)
                        )
                ).then(
                        enumConfigArgBuilder(
                                "power-block-usage",
                                PowerBlockType.values(),
                                () -> BlockMinerMod.getInstance().getConfig().getPowerBlockUsage(),
                                (value) -> BlockMinerMod.getInstance().getConfig().setPowerBlockUsage(value)
                        )
                ).then(
                        enumConfigArgBuilder(
                                "distance-calculation-mode",
                                DistanceCalculationMode.values(),
                                () -> BlockMinerMod.getInstance().getConfig().getDistanceCalculationMode(),
                                (value) -> BlockMinerMod.getInstance().getConfig().setDistanceCalculationMode(value)
                        )
                ).then(
                        enumConfigArgBuilder(
                                "search-mode",
                                SearchMode.values(),
                                () -> BlockMinerMod.getInstance().getConfig().getSearchMode(),
                                (value) -> BlockMinerMod.getInstance().getConfig().setSearchMode(value)
                        )
                ).then(literal("target-block")
                        .then(literal("whitelist")
                                .executes((context) -> {
                                    context.getSource().sendFeedback(Text.of(BlockMinerMod.getInstance().getConfig().blockWhitelist().toString()));
                                    return 1;
                                })
                                .then(literal("add")
                                        .then(
                                                argument("block", getBlockStateArgumentType())
                                                        .executes(context -> {
                                                            BaseConfig config = BlockMinerMod.getInstance().getConfig();
                                                            Block input = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();
                                                            if (config.blockWhitelist().add(input)) {
                                                                BlockMinerMod.getInstance().tryToSaveConfig();
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
                                                            BaseConfig config = BlockMinerMod.getInstance().getConfig();
                                                            Block input = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();
                                                            if (config.blockWhitelist().remove(input)) {
                                                                BlockMinerMod.getInstance().tryToSaveConfig();
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
                                    context.getSource().sendFeedback(Text.of(BlockMinerMod.getInstance().getConfig().dependBlockWhitelistToString()));
                                    return 1;
                                })
                                .then(literal("add")
                                        .then(
                                                argument("block", getBlockStateArgumentType())
                                                        .executes(context -> {
                                                            BaseConfig config = BlockMinerMod.getInstance().getConfig();
                                                            Block input = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();
                                                            if (config.dependBlockWhitelistAdd(input)) {
                                                                BlockMinerMod.getInstance().tryToSaveConfig();
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
                                                            BaseConfig config = BlockMinerMod.getInstance().getConfig();
                                                            Block input = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();
                                                            if (config.dependBlockWhitelistRemove(input)) {
                                                                BlockMinerMod.getInstance().tryToSaveConfig();
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
                                            if (!BlockMinerMod.getInstance().getConfig().isDebug()) {
                                                context.getSource().sendFeedback(Text.of("debug not enabled"));
                                                return 0;
                                            }
                                            final BlockPos start = BlockPosArgumentType.getBlockPos(context, "start");
                                            final BlockPos end = BlockPosArgumentType.getBlockPos(context, "end");
                                            return BlockMinerMod.getInstance().getTaskManager().addAura(start, end) ? 1 : 0;
                                        })
                                )
                        )
                )
        ;
        dispatcher.register(builder);
    }
}
