package me.z7087.blockminer.util;

import net.minecraft.network.ClientConnection;

import java.util.concurrent.atomic.AtomicReference;

public final class BlinkUtils {
    private BlinkUtils() {}

    public static final AtomicReference<ClientConnection> blinkingConnection = new AtomicReference<>();

    public static boolean tryStartBlinking(ClientConnection connection) {
        return blinkingConnection.compareAndSet(null, connection);
    }

    public static boolean tryStopBlinking(ClientConnection connection) {
        final boolean result = blinkingConnection.compareAndSet(connection, null);
        //#if MC >= 12002
        connection.flush();
        //#else
        //$$ ((me.z7087.blockminer.mixin.minecraft.network.ClientConnectionAccessor) connection).invokeHandleQueuedTasks();
        //#endif
        return result;
    }
}
