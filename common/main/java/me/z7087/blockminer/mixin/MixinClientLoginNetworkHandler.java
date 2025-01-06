package me.z7087.blockminer.mixin;

import me.z7087.blockminer.BlockMinerMod;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLoginNetworkHandler.class)
public class MixinClientLoginNetworkHandler {
    @Shadow
    @Final
    private ClientConnection connection;

    @Inject(method = "onSuccess", at = @At("RETURN"))
    private void onOnSuccess(CallbackInfo ci) {
        if (BlockMinerMod.INSTANCE.config.hello) {
            // 似乎没有不借助fabric-api用尽量小的侵入发自定义包的方法，就这样吧
            // 要是有神奇反作弊报坏包就给这删了
            this.connection.send(new CustomPayloadC2SPacket(new BrandCustomPayload("fabric:blockminer:hello")));
        }
    }
}
