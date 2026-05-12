package me.z7087.blockminer.util;

import me.z7087.blockminer.mixin.minecraft.client.network.ClientPlayerEntityAccessor;
import me.z7087.blockminer.task.Task;
import me.z7087.blockminer.util.data.Pair;
import me.z7087.blockminer.util.data.SingleAxisRotation;
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
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class RotationUtils {
    private RotationUtils() {}

    private static final MethodHandle CONSTRUCTOR;
    static {
        final String[] immutableNames, immutableDescriptors, mutableNames, mutableDescriptors;
        try {
            final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                    MethodHandles.lookup(),
                    (Function<RotationUtils, Deque<SingleAxisRotation>> & Serializable) RotationUtils::yawRotations,
                    (Function<RotationUtils, Deque<SingleAxisRotation>> & Serializable) RotationUtils::pitchRotations
            );
            final String[][] mutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                    MethodHandles.lookup(),
                    (Function<RotationUtils, Boolean> & Serializable) RotationUtils::keepYawToNextTick
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
        final Deque<SingleAxisRotation> yawRotations = new ArrayDeque<>();
        final Deque<SingleAxisRotation> pitchRotations = new ArrayDeque<>();
        try {
            return (RotationUtils) CONSTRUCTOR.invokeExact(
                    yawRotations,
                    pitchRotations
            );
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    abstract Deque<SingleAxisRotation> yawRotations();
    abstract Deque<SingleAxisRotation> pitchRotations();

    abstract boolean keepYawToNextTick();
    abstract void keepYawToNextTick(boolean value);

    private Pair<SingleAxisRotation, SingleAxisRotation> pushRotation(float yaw, float pitch) {
        final Pair<SingleAxisRotation, SingleAxisRotation> rotations =
                SingleAxisRotation.ofLinked(yaw, pitch);
        yawRotations().addFirst(rotations.first());
        pitchRotations().addFirst(rotations.second());
        return rotations;
    }

    private void testAndPopRotation(SingleAxisRotation yawRotation, SingleAxisRotation pitchRotation) {
        if (yawRotations().isEmpty() || pitchRotations().isEmpty())
            throw new IllegalStateException("cannot pop rotation for empty stacks");
        if (yawRotations().getFirst() == yawRotation && pitchRotations().getFirst() == pitchRotation) {
            yawRotations().removeFirst();
            pitchRotations().removeFirst();
        }
        throw new IllegalStateException("the rotation to pop is not on top of the stack");
    }

    public boolean hasYaw() {
        return !yawRotations().isEmpty();
    }

    public float getYaw(float defaultYaw) {
        if (!hasYaw()) {
            return defaultYaw;
        }
        return yawRotations().getFirst().angle;
    }

    public boolean hasPitch() {
        return !pitchRotations().isEmpty();
    }

    public float getPitch(float defaultPitch) {
        if (!hasPitch()) {
            return defaultPitch;
        }
        return pitchRotations().getFirst().angle;
    }

    // 需要假设没有为none的Rotation在stack里
    // 重构前可能出现 之后就不允许了
    public boolean canPushRotation(float yaw, float pitch) {
        return canPushYaw(yaw);
    }

    public boolean tryPushRotation(float yaw, float pitch, Task.Lookup ownerTask) {
        if (!hasYaw()) {
            final Pair<SingleAxisRotation, SingleAxisRotation> rotations = SingleAxisRotation.ofLinked(yaw, pitch, ownerTask);
            yawRotations().addFirst(rotations.first());
            pitchRotations().addFirst(rotations.second());
        } else {
            SingleAxisRotation firstYaw = yawRotations().getFirst();
            if (firstYaw.angle != yaw) {
                return false;
            }
            firstYaw.addDependTask(ownerTask);
            pitchRotations().addFirst(SingleAxisRotation.of(pitch, ownerTask));
        }
        return true;
    }

    public boolean canPushYaw(float yaw) {
        return !hasYaw() || yawRotations().getFirst().angle == yaw;
    }

    public boolean tryPushYaw(float yaw, Task.Lookup ownerTask) {
        final SingleAxisRotation firstYaw;
        if (!hasYaw()) {
            yawRotations().addFirst(SingleAxisRotation.of(yaw, ownerTask));
            return true;
        } else if ((firstYaw = yawRotations().getFirst()).angle == yaw) {
            firstYaw.addDependTask(ownerTask);
            return true;
        } else {
            return false;
        }
    }

    public void popYaw(Task.Lookup ownerTask) {
        final SingleAxisRotation firstYaw;
        if (!hasYaw()) {
            throw new IllegalStateException("popping yaw while no yaw pushed");
        } else if ((firstYaw = yawRotations().getFirst()).isOwner(ownerTask.instance) || firstYaw.inDependTasks(ownerTask.instance)) {
            if (!firstYaw.popOwnerOrDependTask(ownerTask)) {
                yawRotations().removeFirst();
            }
        }
        throw new IllegalStateException("not the owner");
    }

    // 现在没有canPushPitch 因为注意到pitch是瞬时到位 用时可以立即设置并使用

    @Deprecated
    public void pushPitch(float pitch, Task.Lookup ownerTask) {
        pitchRotations().addFirst(SingleAxisRotation.of(pitch, ownerTask));
    }

    // 记得updateLocation。
    public <T> T pushPitchDuring(float pitch, Task.Lookup ownerTask, Supplier<T> supplier) {
        final SingleAxisRotation pitchRotation = SingleAxisRotation.of(pitch, ownerTask);
        pitchRotations().addFirst(pitchRotation);
        T result = supplier.get();
        if (pitchRotation != pitchRotations().removeFirst()) {
            throw new IllegalStateException("concurrent modification");
        }
        return result;
    }

    public <T> T pushPitchAndUpdateLocationDuring(ClientPlayerEntity player, float pitch, Task.Lookup ownerTask, Supplier<T> supplier) {
        final SingleAxisRotation pitchRotation = SingleAxisRotation.of(pitch, ownerTask);
        pitchRotations().addFirst(pitchRotation);
        updateLocation(player);
        T result = supplier.get();
        if (pitchRotation != pitchRotations().removeFirst()) {
            throw new IllegalStateException("concurrent modification");
        }
        return result;
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
            final Pair<SingleAxisRotation, SingleAxisRotation> rotations = pushRotation(yaw, pitch);
            if (onGroundStateChanged)
                player.setOnGround(onGround);
            playerAccessor.invokeSendMovementPackets();
            runnable.run();
            if (positionChanged) {
                player.setPosition(originX, originY, originZ);
                player.setBoundingBox(originBoundingBox);
            }
            testAndPopRotation(rotations.first(), rotations.second());
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
            final Pair<SingleAxisRotation, SingleAxisRotation> rotations = pushRotation(yaw, pitch);
            playerAccessor.invokeSendMovementPackets();
            runnable.run();
            testAndPopRotation(rotations.first(), rotations.second());
            playerAccessor.invokeSendMovementPackets();
            return;
        }
        runnable.run();
    }

    public <T> T useServerSideRotationDuring(ClientPlayerEntity player, Supplier<T> supplier) {
        final ClientPlayerEntityAccessor playerAccessor = (ClientPlayerEntityAccessor) player;
        return fakeClientSideRotationDuring(player, playerAccessor.getLastYaw(), playerAccessor.getLastPitch(), supplier);
    }

    public <T> T fakeClientSideRotationDuring(ClientPlayerEntity player, float yaw, float pitch, Supplier<T> supplier) {
        final T result;
        float originYaw, originPitch;
        //#if MC >= 11700
        originYaw = player.getYaw();
        originPitch = player.getPitch();
        player.setYaw(yaw);
        player.setPitch(pitch);
        result = supplier.get();
        player.setYaw(originYaw);
        player.setPitch(originPitch);
        //#else
        //$$ originYaw = player.yaw;
        //$$ originPitch = player.pitch;
        //$$ player.yaw = yaw;
        //$$ player.pitch = pitch;
        //$$ result = supplier.get();
        //$$ player.yaw = originYaw;
        //$$ player.pitch = originPitch;
        //#endif
        return result;
    }

    public void markKeepYaw(Task.Lookup ownerTask) {
        if (yawRotations().isEmpty()) {
            throw new IllegalStateException("Cannot mark keep yaw when no rotations have been set!");
        }
        final SingleAxisRotation firstYaw = yawRotations().getFirst();
        if (firstYaw.isOwner(ownerTask.instance) || firstYaw.inDependTasks(ownerTask.instance)) {
            keepYawToNextTick(true);
            return;
        }
        throw new IllegalStateException("Only the owner task or its depend tasks can mark keep yaw!");
    }

    public void forceClearRotations() {
        yawRotations().clear();
        pitchRotations().clear();
        keepYawToNextTick(false);
    }

    // 实际上stack上最多只有一个任务使用的yaw... 所有权机制似乎没什么用了 每个需要yaw的任务必定需求-
    // 下一tick相同yaw 能想到的好处只有依赖此yaw的任务全部完成后在同tick立即清除yaw并进入另一个yaw的使用流程
    public void resetYawRotationIfNoKeepYaw() {
        if (yawRotations().size() > 1) {
            throw new IllegalStateException("called resetRotation() during calling useRotationDuring()");
        }
        if (!pitchRotations().isEmpty()) {
            throw new IllegalStateException("pitch rotation stack should be empty when resetting yaw rotation");
        }
        if (keepYawToNextTick())
            keepYawToNextTick(false);
        else
            yawRotations().clear();
    }
}
