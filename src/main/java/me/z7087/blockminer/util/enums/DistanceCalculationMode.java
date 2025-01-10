package me.z7087.blockminer.util.enums;

public enum DistanceCalculationMode {
    Old("old"),
    V1_19("1.19"),
    V1_20_6("1.20.6");

    private final String name;

    DistanceCalculationMode(String name) {
        this.name = name;
    }

    public static final DistanceCalculationMode currentClientVersion =
            //#if MC >= 12006
            V1_20_6
            //#elseif MC >= 11900
            //$$ V1_19
            //#else
            //$$ Old
            //#endif
            ;

    public static DistanceCalculationMode of(String name) {
        if (name == null)
            throw new NullPointerException("Name is null");
        switch (name) {
            case "old":
                return Old;
            case "1.19":
                return V1_19;
            case "1.20.6":
                return V1_20_6;
        }
        throw new IllegalArgumentException(
                "No enum constant " + DistanceCalculationMode.class.getCanonicalName() + "." + name);
    }

    @Override
    public String toString() {
        return name;
    }
}
