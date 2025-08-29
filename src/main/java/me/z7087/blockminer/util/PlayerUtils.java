package me.z7087.blockminer.util;

import me.z7087.blockminer.mixin.minecraft.client.network.ClientPlayerEntityAccessor;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.function.Supplier;

public class PlayerUtils {
    // 在supplier运行期间保证玩家在客户端和服务端都在潜行，并在运行后回滚玩家在客户端和服务端的潜行状态
    public static <T> T sneakDuring(ClientPlayerEntity player, Supplier<T> supplier) {
        if (player.isSneaking()) {
            if (
                //#if MC <= 12105
                    !((ClientPlayerEntityAccessor) player).getLastSneaking()
                //#else
                //$$ !((ClientPlayerEntityAccessor) player).getLastPlayerInput().sneak()
                //#endif
            ) {
                updateServersideSneaking(player, true);
                final T result = supplier.get();
                updateServersideSneaking(player, false);
                return result;
            }
            return supplier.get();
        } else {
            final Input originInput = player.input;
            //#if MC >= 12102
            final net.minecraft.util.PlayerInput originPlayerInput = originInput.playerInput;
            originInput.playerInput = new net.minecraft.util.PlayerInput(
                    originPlayerInput.forward(),
                    originPlayerInput.backward(),
                    originPlayerInput.left(),
                    originPlayerInput.right(),
                    originPlayerInput.jump(),
                    true,
                    originPlayerInput.sprint()
            );
            //#else
            //$$ // damn nullable?
            //$$ Objects.requireNonNull(originInput).sneaking = true;
            //#endif

            if (
                //#if MC <= 12105
                    !((ClientPlayerEntityAccessor) player).getLastSneaking()
                //#else
                //$$ !((ClientPlayerEntityAccessor) player).getLastPlayerInput().sneak()
                //#endif
            ) {
                updateServersideSneaking(player, true);
                final T result = supplier.get();
                //#if MC >= 12102
                originInput.playerInput = originPlayerInput;
                //#else
                //$$ originInput.sneaking = false;
                //#endif
                updateServersideSneaking(player, false);
                return result;
            }
            final T result = supplier.get();
            //#if MC >= 12102
            originInput.playerInput = originPlayerInput;
            //#else
            //$$ originInput.sneaking = false;
            //#endif
            return result;
        }
    }

    public static void updateServersideSneaking(ClientPlayerEntity player) {
        //#if MC <= 12105
        final boolean isSneaking = player.isSneaking();
        if (isSneaking != ((ClientPlayerEntityAccessor) player).getLastSneaking()) {
            updateServersideSneaking(player, isSneaking);
        }
        //#else
        //$$ final net.minecraft.util.PlayerInput playerInput = player.input.playerInput;
        //$$ if (!((ClientPlayerEntityAccessor) player).getLastPlayerInput().equals(playerInput)) {
        //$$     player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket(playerInput));
        //$$     ((ClientPlayerEntityAccessor) player).setLastPlayerInput(playerInput);
        //$$ }
        //#endif
    }

    // 执行未检查的潜行设置 确保适当的检查后再调用这个方法
    private static void updateServersideSneaking(ClientPlayerEntity player, boolean isSneaking) {
        //#if MC <= 12105
        net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode mode = isSneaking ? net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY : net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY;
        player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(player, mode));
        ((ClientPlayerEntityAccessor) player).setLastSneaking(isSneaking);
        //#else
        //$$ net.minecraft.util.PlayerInput playerInput = player.input.playerInput;
        //$$ if (playerInput.sneak() != isSneaking) {
        //$$     playerInput = new net.minecraft.util.PlayerInput(
        //$$             playerInput.forward(),
        //$$             playerInput.backward(),
        //$$             playerInput.left(),
        //$$             playerInput.right(),
        //$$             playerInput.jump(),
        //$$             isSneaking,
        //$$             playerInput.sprint()
        //$$     );
        //$$ }
        //$$ if (!((ClientPlayerEntityAccessor) player).getLastPlayerInput().equals(playerInput)) {
        //$$     player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket(playerInput));
        //$$     ((ClientPlayerEntityAccessor) player).setLastPlayerInput(playerInput);
        //$$ }
        //#endif
    }
}
