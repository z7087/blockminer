package me.z7087.blockminer.util.data;

public final class Rotation {
    public static final Rotation NONE = new Rotation(false, false, 0, 0);

    public static Rotation ofYawOnly(float yaw) {
        return new Rotation(true, false, yaw, 0);
    }

    public static Rotation ofPitchOnly(float pitch) {
        return new Rotation(false, true, 0, pitch);
    }

    public static Rotation ofFull(float yaw, float pitch) {
        return new Rotation(true, true, yaw, pitch);
    }

    private final boolean hasYaw;
    private final boolean hasPitch;
    private final float yaw;
    private final float pitch;
    private Rotation(boolean hasYaw, boolean hasPitch, float yaw, float pitch) {
        this.hasYaw = hasYaw;
        this.hasPitch = hasPitch;
        this.yaw = hasYaw ? yaw : 0;
        this.pitch = hasPitch ? pitch : 0;
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
}
