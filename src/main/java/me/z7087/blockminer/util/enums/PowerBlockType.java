package me.z7087.blockminer.util.enums;

public enum PowerBlockType {
    RedstoneTorch("redstone-torch"),
    Lever("lever"),
    Both("both");

    private final String name;

    PowerBlockType(String name) {
        this.name = name;
    }

    public static PowerBlockType of(String name) {
        if (name == null)
            throw new NullPointerException("Name is null");
        switch (name) {
            case "redstone-torch":
                return RedstoneTorch;
            case "lever":
                return Lever;
            case "both":
                return Both;
        }
        throw new IllegalArgumentException(
                "No enum constant " + PowerBlockType.class.getCanonicalName() + "." + name);
    }

    public boolean isRedstoneTorch() {
        return this != Lever;
    }

    public boolean isLever() {
        return this != RedstoneTorch;
    }

    public static PowerBlockType merge(PowerBlockType type1, PowerBlockType type2) {
        switch (type1) {
            case RedstoneTorch: {
                if (type2.isLever())
                    return Both;
                return RedstoneTorch;
            }
            case Lever: {
                if (type2.isRedstoneTorch())
                    return Both;
                return Lever;
            }
            default: {
                return Both;
            }
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
