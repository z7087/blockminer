package me.z7087.blockminer.mixin.minecraft.network;

import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientConnection.class)
public interface ClientConnectionAccessor {
    @Invoker
    void invokeHandleQueuedTasks();
}
