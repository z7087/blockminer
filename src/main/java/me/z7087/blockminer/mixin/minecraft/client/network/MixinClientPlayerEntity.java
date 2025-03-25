package me.z7087.blockminer.mixin.minecraft.client.network;

import me.z7087.blockminer.BlockMinerMod;
import me.z7087.blockminer.util.data.Rotation;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {
    //#if MC >= 11700
    @Redirect(method = "sendMovementPackets",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"
            )
    )
    //#else
    //$$ @Redirect(method = "sendMovementPackets",
    //$$         at = @At(
    //$$                 value = "FIELD",
    //$$                 target = "Lnet/minecraft/client/network/ClientPlayerEntity;yaw:F",
    //$$                 opcode = org.objectweb.asm.Opcodes.GETFIELD
    //$$         )
    //$$ )
    //#endif
    private float onGetYaw(ClientPlayerEntity player) {
        Rotation rotation = BlockMinerMod.getInstance().rotationUtils.getRotation();
        if (rotation.hasYaw())
            return rotation.getYaw();
        //#if MC >= 11700
        return player.getYaw();
        //#else
        //$$ return player.yaw;
        //#endif
    }

    //#if MC >= 11700
    @Redirect(method = "sendMovementPackets",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"
            )
    )
    //#else
    //$$ @Redirect(method = "sendMovementPackets",
    //$$         at = @At(
    //$$                 value = "FIELD",
    //$$                 target = "Lnet/minecraft/client/network/ClientPlayerEntity;pitch:F",
    //$$                 opcode = org.objectweb.asm.Opcodes.GETFIELD
    //$$         )
    //$$ )
    //#endif
    private float onGetPitch(ClientPlayerEntity player) {
        Rotation rotation = BlockMinerMod.getInstance().rotationUtils.getRotation();
        if (rotation.hasPitch())
            return rotation.getPitch();
        //#if MC >= 11700
        return player.getPitch();
        //#else
        //$$ return player.pitch;
        //#endif
    }
}
