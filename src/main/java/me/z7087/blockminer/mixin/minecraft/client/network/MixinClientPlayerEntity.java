package me.z7087.blockminer.mixin.minecraft.client.network;

import me.z7087.blockminer.BlockMinerMod;
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
        float yaw =
                //#if MC >= 11700
                player.getYaw()
                //#else
                //$$ player.yaw
                //#endif
                ;
        if (!BlockMinerMod.getInstance().getTaskManager().isEnabled()) {
            return yaw;
        }
        yaw = BlockMinerMod.getInstance().getRotationUtils().getYaw(yaw);
        BlockMinerMod.getInstance().getUncertainManager().onYawDirectionUpdate(yaw);
        return yaw;
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
        final float pitch =
                //#if MC >= 11700
                player.getPitch()
                //#else
                //$$ player.pitch
                //#endif
                ;
        if (!BlockMinerMod.getInstance().getTaskManager().isEnabled()) {
            return pitch;
        }
        return BlockMinerMod.getInstance().getRotationUtils().getPitch(pitch);
    }
}
