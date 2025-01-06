package me.z7087.blockminer.mixin;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.util.data.Rotation;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {
    @Redirect(method = "sendMovementPackets",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"
            )
    )
    private float onGetYaw(ClientPlayerEntity player) {
        Rotation rotation = BlockMinerMod.INSTANCE.rotationUtils.getRotation();
        if (rotation.hasYaw())
            return rotation.getYaw();
        return player.getYaw();
    }
    @Redirect(method = "sendMovementPackets",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"
            )
    )
    private float onGetPitch(ClientPlayerEntity player) {
        Rotation rotation = BlockMinerMod.INSTANCE.rotationUtils.getRotation();
        if (rotation.hasPitch())
            return rotation.getPitch();
        return player.getPitch();
    }
}
