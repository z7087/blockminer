package me.z7087.blockminer.mixin;

import me.z7087.blockminer.BlockMinerMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void onTick(CallbackInfo ci) {
        BlockMinerMod.INSTANCE.taskManager.tick();
    }
}
