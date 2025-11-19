package me.z7087.blockminer.util;

import me.z7087.blockminer.mixin.minecraft.client.network.ClientPlayerEntityAccessor;
import me.z7087.blockminer.util.data.Rotation;
import me.z7087.final2constant.Constant;
import me.z7087.final2constant.util.JavaHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public abstract class RotationUtils {
    private RotationUtils() {}

    private static final MethodHandle CONSTRUCTOR;
    static {
        final String[] immutableNames, immutableDescriptors, mutableNames, mutableDescriptors;
        try {
            RotationUtils rotationUtilsEmptyImpl = Constant.factory.ofEmptyAbstractImplInstance(
                    MethodHandles.lookup(),
                    RotationUtils.class
            );
            final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                    MethodHandles.lookup(),
                    (Supplier<Deque<Rotation>> & Serializable) rotationUtilsEmptyImpl::rotations
            );
            final String[][] mutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                    MethodHandles.lookup(),
                    (BooleanSupplier & Serializable) rotationUtilsEmptyImpl::keepRotationToNextTick
            );
            immutableNames = immutableNamesAndDescriptors[0];
            immutableDescriptors = immutableNamesAndDescriptors[1];
            mutableNames = mutableNamesAndDescriptors[0];
            mutableDescriptors = mutableNamesAndDescriptors[1];
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        CONSTRUCTOR = Constant.factory.ofRecordConstructor(
                MethodHandles.lookup(),
                RotationUtils.class,
                false,
                immutableNames,
                immutableDescriptors,
                mutableNames,
                mutableDescriptors,
                true,
                false
        );
    }

    public static RotationUtils createInstance() {
        final Deque<Rotation> rotations = new ArrayDeque<>();
        rotations.add(Rotation.NONE);
        try {
            return (RotationUtils) CONSTRUCTOR.invokeExact(
                    rotations
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    abstract Deque<Rotation> rotations();

    abstract boolean keepRotationToNextTick();
    abstract void keepRotationToNextTick(boolean value);

    public Rotation getRotation() {
        return rotations().getFirst();
    }

    private void setRotation(Rotation rotation) {
        rotations().removeFirst();
        rotations().addFirst(rotation);
    }

    private void pushRotation(Rotation rotation) {
        rotations().addFirst(rotation);
    }

    private void popRotation() {
        if (rotations().size() <= 1)
            throw new IllegalStateException("cannot pop rotation for size <=1 stack");
        rotations().removeFirst();
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

    public boolean canSetRotation(float yaw, float pitch) {
        final Rotation oldRotation = getRotation();
        return (!oldRotation.hasYaw() || oldRotation.getYaw() == yaw)
                && (!oldRotation.hasPitch() || oldRotation.getPitch() == pitch);
    }

    public boolean trySetRotation(float yaw, float pitch) {
        final Rotation oldRotation = getRotation();
        if ((oldRotation.hasYaw() && oldRotation.getYaw() != yaw)
                || (oldRotation.hasPitch() && oldRotation.getPitch() != pitch))
            return false;
        setRotation(Rotation.ofFull(yaw, pitch));
        return true;
    }

    public boolean canSetYaw(float yaw) {
        final Rotation oldRotation = getRotation();
        return !oldRotation.hasYaw() || oldRotation.getYaw() == yaw;
    }

    public boolean trySetYaw(float yaw) {
        final Rotation oldRotation = getRotation();
        if (oldRotation.hasYaw() && oldRotation.getYaw() != yaw)
            return false;
        else if (oldRotation.hasPitch())
            setRotation(Rotation.ofFull(yaw, oldRotation.getPitch()));
        else
            setRotation(Rotation.ofYawOnly(yaw));
        return true;
    }

    public boolean canSetPitch(float pitch) {
        final Rotation oldRotation = getRotation();
        return !oldRotation.hasPitch() || oldRotation.getPitch() == pitch;
    }

    public boolean trySetPitch(float pitch) {
        Rotation oldRotation = getRotation();
        if (oldRotation.hasPitch() && oldRotation.getPitch() != pitch)
            return false;
        if (oldRotation.hasYaw())
            setRotation(Rotation.ofFull(oldRotation.getYaw(), pitch));
        else
            setRotation(Rotation.ofPitchOnly(pitch));
        return true;
    }

    public void updateLocation(ClientPlayerEntity player) {
        final ClientPlayerEntityAccessor playerAccessor = (ClientPlayerEntityAccessor) player;
        playerAccessor.invokeSendMovementPackets();
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
            pushRotation(Rotation.ofFull(yaw, pitch));
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
            pushRotation(Rotation.ofFull(yaw, pitch));
            playerAccessor.invokeSendMovementPackets();
            runnable.run();
            popRotation();
            playerAccessor.invokeSendMovementPackets();
            return;
        }
        runnable.run();
    }

    public <T> T useServerSideRotationDuring(ClientPlayerEntity player, Supplier<T> supplier) {
        final ClientPlayerEntityAccessor playerAccessor = (ClientPlayerEntityAccessor) player;
        T result;
        float originYaw, originPitch;
        //#if MC >= 11700
        originYaw = player.getYaw();
        originPitch = player.getPitch();
        player.setYaw(playerAccessor.getLastYaw());
        player.setPitch(playerAccessor.getLastPitch());
        result = supplier.get();
        player.setYaw(originYaw);
        player.setPitch(originPitch);
        //#else
        //$$ originYaw = player.yaw;
        //$$ originPitch = player.pitch;
        //$$ player.yaw = playerAccessor.getLastYaw();
        //$$ player.pitch = playerAccessor.getLastPitch();
        //$$ result = supplier.get();
        //$$ player.yaw = originYaw;
        //$$ player.pitch = originPitch;
        //#endif
        return result;
    }

    public void markKeepRotation() {
        keepRotationToNextTick(true);
    }

    public void forceClearRotations() {
        rotations().clear();
        rotations().add(Rotation.NONE);
    }

    public void resetRotationIfNoKeepRotation() {
        if (rotations().size() > 1) {
            throw new IllegalStateException("called resetRotation() during calling useRotationDuring()");
        }
        if (keepRotationToNextTick())
            keepRotationToNextTick(false);
        else
            setRotation(Rotation.NONE);
    }
}
