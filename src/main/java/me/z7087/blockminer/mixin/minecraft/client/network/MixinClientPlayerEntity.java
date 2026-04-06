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
        if (!BlockMinerMod.getInstance().getTaskManager().isEnabled()) {
            //#if MC >= 11700
            return player.getYaw();
            //#else
            //$$ return player.yaw;
            //#endif
        }
        final Rotation rotation = BlockMinerMod.getInstance().getRotationUtils().getRotation();
        final float yaw;
        if (rotation.hasYaw()) {
            yaw = rotation.getYaw();
        } else {
            //#if MC >= 11700
            yaw = player.getYaw();
            //#else
            //$$ yaw = player.yaw;
            //#endif
        }
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
        final Rotation rotation;
        if (BlockMinerMod.getInstance().getTaskManager().isEnabled()
                && (rotation = BlockMinerMod.getInstance().getRotationUtils().getRotation()).hasPitch()
        )
            return rotation.getPitch();
        //#if MC >= 11700
        return player.getPitch();
        //#else
        //$$ return player.pitch;
        //#endif
    }
}
