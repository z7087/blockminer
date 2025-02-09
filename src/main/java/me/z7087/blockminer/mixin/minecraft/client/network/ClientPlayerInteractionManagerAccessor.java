package me.z7087.blockminer.mixin.minecraft.client.network;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker
    void invokeSyncSelectedSlot();
    @Invoker
    boolean invokeIsCurrentlyBreaking(BlockPos pos);
}
