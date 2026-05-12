package me.z7087.blockminer.util.data;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.z7087.blockminer.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Function;

public final class SingleAxisRotation {
    public final float angle;
    @Nullable
    private final WeakReference<SingleAxisRotation> linkedOtherAngle; // 应该是不可能被回收的 这里尝试防止循环引用 虽然jvm不用引用计数
    @Nullable
    private Task ownerTask;
    private final ObjectOpenHashSet<Task> dependTasks;

    private SingleAxisRotation(float angle, @Nullable Task ownerTask) {
        this.angle = angle;
        this.linkedOtherAngle = null;
        this.dependTasks = new ObjectOpenHashSet<>();
        this.ownerTask = ownerTask;
    }

    private SingleAxisRotation(float angle, SingleAxisRotation linkedOtherAngle, @Nullable Task ownerTask) {
        this.angle = angle;
        this.linkedOtherAngle = new WeakReference<>(linkedOtherAngle);
        this.dependTasks = new ObjectOpenHashSet<>();
        this.ownerTask = ownerTask;
    }

    private SingleAxisRotation(float angle, Function<SingleAxisRotation, SingleAxisRotation> linkedPitchSupplier, @Nullable Task ownerTask) {
        this.angle = angle;
        final SingleAxisRotation linkedOtherAngle = linkedPitchSupplier.apply(this);
        this.linkedOtherAngle = new WeakReference<>(linkedOtherAngle);
        this.dependTasks = new ObjectOpenHashSet<>();
        this.ownerTask = ownerTask;
    }

    @NotNull
    public Task getOwnerTask() {
        return Objects.requireNonNull(ownerTask);
    }

    public boolean isOwner(Task task) {
        return ownerTask == task;
    }

    public boolean addDependTask(Task.Lookup dependTask) {
        return isOwner(dependTask.instance) || dependTasks.add(dependTask.instance);
    }

    public boolean inDependTasks(Task task) {
        return dependTasks.contains(task);
    }

    public boolean popOwnerOrDependTask(Task.Lookup task) {
        if (isOwner(task.instance)) {
            if (!dependTasks.isEmpty()) {
                ObjectIterator<Task> it = dependTasks.iterator();
                this.ownerTask = it.next();
                it.remove();
                return true;
            }
            return false;
        }
        if (dependTasks.remove(task.instance)) {
            return true;
        }
        throw new IllegalArgumentException("Task is not a owner or depend of this rotation");
    }

    public static SingleAxisRotation of(float angle) {
        return new SingleAxisRotation(angle, null);
    }

    public static SingleAxisRotation of(float angle, Task.Lookup ownerTask) {
        return new SingleAxisRotation(angle, ownerTask.instance);
    }

    public static Pair<SingleAxisRotation, SingleAxisRotation> ofLinked(float yaw, float pitch) {
        LinkedPitchSupplier linkedPitchSupplier = new LinkedPitchSupplier(pitch, null);
        SingleAxisRotation yawRotation = new SingleAxisRotation(yaw, linkedPitchSupplier, null);
        SingleAxisRotation pitchRotation = linkedPitchSupplier.getLinkedPitch();
        return new Pair<>(yawRotation, pitchRotation);
    }

    public static Pair<SingleAxisRotation, SingleAxisRotation> ofLinked(float yaw, float pitch, Task.Lookup ownerTask) {
        LinkedPitchSupplier linkedPitchSupplier = new LinkedPitchSupplier(pitch, ownerTask.instance);
        SingleAxisRotation yawRotation = new SingleAxisRotation(yaw, linkedPitchSupplier, ownerTask.instance);
        SingleAxisRotation pitchRotation = linkedPitchSupplier.getLinkedPitch();
        return new Pair<>(yawRotation, pitchRotation);
    }

    static final class LinkedPitchSupplier implements Function<SingleAxisRotation, SingleAxisRotation> {
        private final float pitch;
        private final Task ownerTask;

        LinkedPitchSupplier(float pitch, Task ownerTask) {
            this.pitch = pitch;
            this.ownerTask = ownerTask;
        }

        private SingleAxisRotation linkedPitch;

        @Override
        public SingleAxisRotation apply(SingleAxisRotation singleAxisRotation) {
            linkedPitch = new SingleAxisRotation(pitch, singleAxisRotation, ownerTask);
            return linkedPitch;
        }

        SingleAxisRotation getLinkedPitch() {
            return linkedPitch;
        }
    }
}
