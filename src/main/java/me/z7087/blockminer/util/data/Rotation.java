package me.z7087.blockminer.util.data;

public abstract class Rotation {
    public abstract boolean hasYaw();

    public float getYaw() {
        if (hasYaw())
            return getYaw(0F);
        throw new IllegalStateException("cannot get yaw from RotationUtils.Rotation." + this.getClass().getSimpleName());
    }

    public abstract float getYaw(float defaultYaw);

    public abstract boolean hasPitch();

    public float getPitch() {
        if (hasPitch())
            return getPitch(0F);
        throw new IllegalStateException("cannot get pitch from RotationUtils.Rotation." + this.getClass().getSimpleName());
    }

    public abstract float getPitch(float defaultPitch);

    public static class None extends Rotation {
        public static final None INSTANCE = new None();

        @Override
        public boolean hasYaw() {
            return false;
        }

        @Override
        public float getYaw(float defaultYaw) {
            return defaultYaw;
        }

        @Override
        public boolean hasPitch() {
            return false;
        }

        @Override
        public float getPitch(float defaultPitch) {
            return defaultPitch;
        }
    }

    public static class YawOnly extends Rotation {
        private final float yaw;

        public YawOnly(float yaw) {
            this.yaw = yaw;
        }

        @Override
        public boolean hasYaw() {
            return true;
        }

        @Override
        public float getYaw() {
            return yaw;
        }

        @Override
        public float getYaw(float defaultYaw) {
            return yaw;
        }

        @Override
        public boolean hasPitch() {
            return false;
        }

        @Override
        public float getPitch(float defaultPitch) {
            return defaultPitch;
        }
    }

    public static class PitchOnly extends Rotation {
        private final float pitch;

        public PitchOnly(float pitch) {
            this.pitch = pitch;
        }

        @Override
        public boolean hasYaw() {
            return false;
        }

        @Override
        public float getYaw(float defaultYaw) {
            return defaultYaw;
        }

        @Override
        public boolean hasPitch() {
            return true;
        }

        @Override
        public float getPitch() {
            return pitch;
        }

        @Override
        public float getPitch(float defaultPitch) {
            return pitch;
        }
    }

    public static class Full extends Rotation {
        private final float yaw;
        private final float pitch;

        public Full(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public boolean hasYaw() {
            return true;
        }

        @Override
        public float getYaw() {
            return yaw;
        }

        @Override
        public float getYaw(float defaultYaw) {
            return yaw;
        }

        @Override
        public boolean hasPitch() {
            return true;
        }

        @Override
        public float getPitch() {
            return pitch;
        }

        @Override
        public float getPitch(float defaultPitch) {
            return pitch;
        }
    }
}
