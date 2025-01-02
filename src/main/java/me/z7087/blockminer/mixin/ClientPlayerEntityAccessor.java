package me.z7087.blockminer.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerEntity.class)
public interface ClientPlayerEntityAccessor {
    /*
    @Accessor
    double getLastX();
    @Accessor
    void setLastX(double lastX);

    @Accessor
    double getLastBaseY();
    @Accessor
    void setLastBaseY(double lastBaseY);

    @Accessor
    double getLastZ();
    @Accessor
    void setLastZ(double lastZ);

    @Accessor
    float getLastYaw();
    @Accessor
    void setLastYaw(float lastYaw);

    @Accessor
    float getLastPitch();
    @Accessor
    void setLastPitch(float lastPitch);

    @Accessor
    boolean getLastOnGround();
    @Accessor
    void setLastOnGround(boolean lastOnGround);

    @Accessor
    int getTicksSinceLastPositionPacketSent();
    @Accessor
    void setTicksSinceLastPositionPacketSent(int ticksSinceLastPositionPacketSent);
     */

    @Invoker
    void invokeSendMovementPackets();
}
