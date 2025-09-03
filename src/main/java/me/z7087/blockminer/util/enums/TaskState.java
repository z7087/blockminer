package me.z7087.blockminer.util.enums;

public enum TaskState {
    Start,
    WaitForPistonPlaceRotate(true),
    PlaceBlocksWithChecks,
    PlaceBlocksWithoutChecks,
    SelectPickaxeAndReadyMine,
    WaitForPistonExtend(true),
    Execute,
    WaitForPistonClear(true),
    ClearPiston,
    Finished;

    private final boolean waiting;

    TaskState() {
        waiting = false;
    }

    TaskState(boolean waiting) {
        this.waiting = waiting;
    }

    public boolean isWaiting() {
        return waiting;
    }
}
