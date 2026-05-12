package me.z7087.blockminer.util.data;

/*
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.z7087.blockminer.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Function;

public final class Rotation {
    public static final Rotation NONE = new Rotation();

    public static Rotation ofYawOnly(float yaw, Task.Lookup ownerTask) {
        return new Rotation(true, false, yaw, 0, ownerTask.instance);
    }

    public static Rotation ofPitchOnly(float pitch, Task.Lookup ownerTask) {
        return new Rotation(false, true, 0, pitch, ownerTask.instance);
    }

    public static Rotation ofFull(float yaw, float pitch, Task.Lookup ownerTask) {
        return new Rotation(true, true, yaw, pitch, ownerTask.instance);
    }

    private final boolean hasYaw;
    private final boolean hasPitch;
    private final float yaw;
    private final float pitch;
    private Task ownerTask;
    private final ObjectOpenHashSet<Task> dependTasks = new ObjectOpenHashSet<>();

    private Rotation() {
        // should be called only from Rotation.NONE
        this.hasYaw = false;
        this.hasPitch = false;
        this.yaw = 0;
        this.pitch = 0;
    }

    @Deprecated
    private Rotation(boolean hasYaw, boolean hasPitch, float yaw, float pitch) {
        this.hasYaw = hasYaw;
        this.hasPitch = hasPitch;
        this.yaw = hasYaw ? yaw : 0;
        this.pitch = hasPitch ? pitch : 0;
    }

    private Rotation(boolean hasYaw, boolean hasPitch, float yaw, float pitch, Task ownerTask) {
        this.hasYaw = hasYaw;
        this.hasPitch = hasPitch;
        this.yaw = hasYaw ? yaw : 0;
        this.pitch = hasPitch ? pitch : 0;
        this.ownerTask = Objects.requireNonNull(ownerTask);
    }


    public boolean hasYaw() {
        return hasYaw;
    }

    public float getYaw() {
        if (hasYaw())
            return yaw;
        throw new IllegalStateException("cannot get yaw from rotation that doesn't have yaw");
    }

    public float getYaw(float defaultYaw) {
        if (hasYaw())
            return yaw;
        return defaultYaw;
    }

    public boolean hasPitch() {
        return hasPitch;
    }

    public float getPitch() {
        if (hasPitch())
            return pitch;
        throw new IllegalStateException("cannot get pitch from rotation that doesn't have pitch");
    }

    public float getPitch(float defaultPitch) {
        if (hasPitch())
            return pitch;
        return defaultPitch;
    }

    @NotNull
    public Task getOwnerTask() {
        return Objects.requireNonNull(ownerTask);
    }

    public boolean isOwner(Task task) {
        return ownerTask == task;
    }

    ObjectOpenHashSet<Task> getDependTasks() {
        return dependTasks;
    }

}
*/
