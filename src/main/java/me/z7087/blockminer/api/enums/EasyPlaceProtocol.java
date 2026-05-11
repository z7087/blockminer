package me.z7087.blockminer.api.enums;

public enum EasyPlaceProtocol {
    None,
    V2,
    V3;

    public static EasyPlaceProtocol of(String name) {
        if (name == null)
            throw new NullPointerException("Name is null");
        switch (name) {
            case "none":
                return None;
            case "v2":
                return V2;
            case "v3":
                return V3;
        }
        throw new IllegalArgumentException(
                "No enum constant " + EasyPlaceProtocol.class.getCanonicalName() + "." + name);
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    /*
    None("none"),
    V2("v2"),
    V3("v3");

    private final String name;

    EasyPlaceProtocol(String name) {
        this.name = name;
    }

    public static EasyPlaceProtocol of(String name) {
        if (name == null)
            throw new NullPointerException("Name is null");
        switch (name) {
            case "none":
                return None;
            case "v2":
                return V2;
            case "v3":
                return V3;
        }
        throw new IllegalArgumentException(
                "No enum constant " + EasyPlaceProtocol.class.getCanonicalName() + "." + name);
    }

    @Override
    public String toString() {
        return name;
    }
     */
}
