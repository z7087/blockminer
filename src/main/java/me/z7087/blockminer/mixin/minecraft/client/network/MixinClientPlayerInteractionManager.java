package me.z7087.blockminer.mixin.minecraft.client.network;

import me.z7087.blockminer.BlockMinerMod;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager {
    @Inject(method = "tick", at = @At(value = "RETURN"))
    private void onTick(CallbackInfo ci) {
        BlockMinerMod.getInstance().getTaskManager().tick();
    }
}
