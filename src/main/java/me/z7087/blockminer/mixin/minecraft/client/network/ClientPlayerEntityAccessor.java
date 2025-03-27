package me.z7087.blockminer.mixin.minecraft.client.network;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor {
    @Accessor(
            //#if MC >= 12105
            "lastXClient"
            //#endif
    )
    double getLastX();
    @Accessor(
            //#if MC >= 12105
            "lastXClient"
            //#endif
    )
    void setLastX(double lastX);

    @Accessor(
            //#if MC >= 12105
            "lastYClient"
            //#else
            //$$ "lastBaseY"
            //#endif
    )
    double getLastY();
    @Accessor(
            //#if MC >= 12105
            "lastYClient"
            //#else
            //$$ "lastBaseY"
            //#endif
    )
    void setLastY(double lastY);

    @Accessor(
            //#if MC >= 12105
            "lastZClient"
            //#endif
    )
    double getLastZ();
    @Accessor(
            //#if MC >= 12105
            "lastZClient"
            //#endif
    )
    void setLastZ(double lastZ);

    @Accessor(
            //#if MC >= 12105
            "lastYawClient"
            //#endif
    )
    float getLastYaw();
    @Accessor(
            //#if MC >= 12105
            "lastYawClient"
            //#endif
    )
    void setLastYaw(float lastYaw);

    @Accessor(
            //#if MC >= 12105
            "lastPitchClient"
            //#endif
    )
    float getLastPitch();
    @Accessor(
            //#if MC >= 12105
            "lastPitchClient"
            //#endif
    )
    void setLastPitch(float lastPitch);

    @Accessor
    boolean getLastOnGround();
    @Accessor
    void setLastOnGround(boolean lastOnGround);

    @Accessor
    int getTicksSinceLastPositionPacketSent();
    @Accessor
    void setTicksSinceLastPositionPacketSent(int ticksSinceLastPositionPacketSent);

    @Invoker
    void invokeSendMovementPackets();
}
