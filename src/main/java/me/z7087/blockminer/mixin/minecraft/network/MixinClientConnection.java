package me.z7087.blockminer.mixin.minecraft.network;

import me.z7087.blockminer.util.BlinkUtils;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
    //#if MC >= 12002

    @ModifyVariable(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private boolean modifyFlushArg(boolean value) {
        // 转Object不需要check-cast字节码，转ClientConnection需要
        return value && ((Object) BlinkUtils.blinkingConnection.get()) != this;
    }

    //#else

    //$$ @org.spongepowered.asm.mixin.injection.Redirect(method = "send("
    //$$         //#if MC >= 11904
    //$$         + "Lnet/minecraft/network/packet/Packet;"
    //$$         //#else
    //$$         //$$ + "Lnet/minecraft/network/Packet;"
    //$$         //#endif
    //$$         //#if MC >= 11902
    //$$         + "Lnet/minecraft/network/PacketCallbacks;"
    //$$         //#else
    //$$         //$$ + "Lio/netty/util/concurrent/GenericFutureListener;"
    //$$         //#endif
    //$$         + ")V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;isOpen()Z"))
    //$$ private boolean modifyIsOpen(ClientConnection connection) {
    //$$     return ((Object) BlinkUtils.blinkingConnection.get()) != this && connection.isOpen();
    //$$ }

    //#endif
}
