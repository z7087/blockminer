package me.z7087.blockminer.util;

import me.z7087.blockminer.mixin.ClientPlayerEntityAccessor;
import me.z7087.blockminer.util.data.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.Deque;
import java.util.LinkedList;

public final class RotationUtils {
    private final Deque<Rotation> rotations = new LinkedList<>();
    {
        rotations.add(Rotation.None.INSTANCE);
    }
    //private Rotation rotation = Rotation.None.INSTANCE;

    public Rotation getRotation() {
        return rotations.getFirst();
    }

    private void setRotation(Rotation rotation) {
        rotations.removeFirst();
        rotations.addFirst(rotation);
    }

    private void pushRotation(Rotation rotation) {
        rotations.addFirst(rotation);
    }

    private void popRotation() {
        if (rotations.size() <= 1)
            throw new IllegalStateException("cannot pop rotation for size <=1 stack");
        rotations.removeFirst();
    }

    public boolean hasYaw() {
        return getRotation().hasYaw();
    }

    public float getYaw(float defaultYaw) {
        return getRotation().getYaw(defaultYaw);
    }

    public boolean hasPitch() {
        return getRotation().hasPitch();
    }

    public float getPitch(float defaultPitch) {
        return getRotation().getPitch(defaultPitch);
    }

    public boolean trySetRotation(float yaw, float pitch) {
        final Rotation oldRotation = getRotation();
        if (oldRotation.hasYaw() || oldRotation.hasPitch())
            return false;
        setRotation(new Rotation.Full(yaw, pitch));
        return true;
    }

    public boolean trySetYaw(float yaw) {
        final Rotation oldRotation = getRotation();
        if (oldRotation.hasYaw())
            return false;
        else if (oldRotation.hasPitch())
            setRotation(new Rotation.Full(yaw, oldRotation.getPitch()));
        else
            setRotation(new Rotation.YawOnly(yaw));
        return true;
    }

    public boolean trySetPitch(float pitch) {
        Rotation oldRotation = getRotation();
        if (oldRotation.hasPitch())
            return false;
        if (oldRotation.hasYaw())
            setRotation(new Rotation.Full(oldRotation.getYaw(), pitch));
        else
            setRotation(new Rotation.PitchOnly(pitch));
        return true;
    }

    public void useLocationDuring(double x, double y, double z, float yaw, float pitch, boolean onGround, Runnable runnable) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null
                && MinecraftClient.getInstance().getCameraEntity() == player
        ) {
            final double originX = player.getX(), originY = player.getY(), originZ = player.getZ();
            final boolean originOnGround = player.isOnGround();
            final boolean positionChanged = originX != x || originY != y || originZ != z;
            final boolean onGroundStateChanged = originOnGround != onGround;
            final ClientPlayerEntityAccessor playerAccessor = (ClientPlayerEntityAccessor) player;
            Box originBoundingBox = null;
            if (positionChanged) {
                originBoundingBox = player.getBoundingBox();
                player.setPosition(x, y, z);
            }
            pushRotation(new Rotation.Full(yaw, pitch));
            if (onGroundStateChanged)
                player.setOnGround(onGround);
            playerAccessor.invokeSendMovementPackets();
            runnable.run();
            if (positionChanged) {
                player.setPosition(originX, originY, originZ);
                player.setBoundingBox(originBoundingBox);
            }
            popRotation();
            if (onGroundStateChanged)
                player.setOnGround(originOnGround);
            playerAccessor.invokeSendMovementPackets();
            return;
        }
        runnable.run();
    }

    public void usePositionDuring(double x, double y, double z, boolean onGround, Runnable runnable) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null
                && MinecraftClient.getInstance().getCameraEntity() == player
        ) {
            final double originX = player.getX(), originY = player.getY(), originZ = player.getZ();
            final boolean originOnGround = player.isOnGround();
            final boolean positionChanged = originX != x || originY != y || originZ != z;
            final boolean onGroundStateChanged = originOnGround != onGround;
            final ClientPlayerEntityAccessor playerAccessor = (ClientPlayerEntityAccessor) player;
            Box originBoundingBox = null;
            if (positionChanged) {
                originBoundingBox = player.getBoundingBox();
                player.setPosition(x, y, z);
            }
            if (onGroundStateChanged)
                player.setOnGround(onGround);
            playerAccessor.invokeSendMovementPackets();
            runnable.run();
            if (positionChanged) {
                player.setPosition(originX, originY, originZ);
                player.setBoundingBox(originBoundingBox);
            }
            if (onGroundStateChanged)
                player.setOnGround(originOnGround);
            playerAccessor.invokeSendMovementPackets();
            return;
        }
        runnable.run();
    }

    public void useRotationDuring(float yaw, float pitch, Runnable runnable) {
        final ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null
                && MinecraftClient.getInstance().getCameraEntity() == player
        ) {
            final ClientPlayerEntityAccessor playerAccessor = (ClientPlayerEntityAccessor) player;
            pushRotation(new Rotation.Full(yaw, pitch));
            playerAccessor.invokeSendMovementPackets();
            runnable.run();
            popRotation();
            playerAccessor.invokeSendMovementPackets();
            return;
        }
        runnable.run();
    }

    public void resetRotation() {
        if (rotations.size() > 1) {
            throw new IllegalStateException("called resetRotation() during calling useRotationDuring()");
        }
        setRotation(Rotation.None.INSTANCE);
    }

}
