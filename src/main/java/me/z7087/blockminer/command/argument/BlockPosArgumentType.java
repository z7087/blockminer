package me.z7087.blockminer.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class BlockPosArgumentType implements ArgumentType<BlockPos> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "~0.5 ~1 ~-5");
    private static final Collection<CommandSource.RelativePosition> DEFAULT_BLOCK_POSITION_SUGGESTIONS = Collections.singleton(CommandSource.RelativePosition.ZERO_WORLD);

    @Override
    public BlockPos parse(StringReader reader) throws CommandSyntaxException {
        final int x, y, z;
        final Entity player = MinecraftClient.getInstance().player;
        x = parsePositionX(reader, player);
        reader.skipWhitespace();
        y = parsePositionY(reader, player);
        reader.skipWhitespace();
        z = parsePositionZ(reader, player);
        return new BlockPos(x, y, z);
    }

    private static int parsePositionX(StringReader reader, Entity player) throws CommandSyntaxException {
        if (reader.peek() == '~') {
            reader.skip();
            return (int) ((player == null ? 0 : player.getX()) + reader.readDouble());
        }
        return (int) reader.readDouble();
    }

    private static int parsePositionY(StringReader reader, Entity player) throws CommandSyntaxException {
        if (reader.peek() == '~') {
            reader.skip();
            return (int) ((player == null ? 0 : player.getY()) + reader.readDouble());
        }
        return (int) reader.readDouble();
    }

    private static int parsePositionZ(StringReader reader, Entity player) throws CommandSyntaxException {
        if (reader.peek() == '~') {
            reader.skip();
            return (int) ((player == null ? 0 : player.getZ()) + reader.readDouble());
        }
        return (int) reader.readDouble();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        final HitResult target = MinecraftClient.getInstance().crosshairTarget;
        final Collection<CommandSource.RelativePosition> blockPosSuggestions;
        if (target != null && target.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) target).getBlockPos();
            blockPosSuggestions = Collections.singleton(
                    new CommandSource.RelativePosition(
                            Integer.toString(pos.getX()),
                            Integer.toString(pos.getY()),
                            Integer.toString(pos.getZ())
                    )
            );
        } else {
            blockPosSuggestions = DEFAULT_BLOCK_POSITION_SUGGESTIONS;
        }
        return CommandSource.suggestPositions(
                builder.getRemaining(),
                blockPosSuggestions,
                builder,
                (ignored) -> true
        );
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static BlockPosArgumentType blockPos() {
        return new BlockPosArgumentType();
    }

    public static BlockPos getBlockPos(CommandContext<?> context, String name) {
        return context.getArgument(name, BlockPos.class);
    }
}
