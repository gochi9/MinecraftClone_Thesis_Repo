package gmail.vladimir.Chapter_3.World.Block;

public enum BlockFace {

    UP(0, 1, 0),
    DOWN(0, -1, 0),
    WEST(-1, 0, 0),
    EAST(1, 0, 0),
    NORTH(0, 0, 1),
    SOUTH(0, 0, -1);

    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    BlockFace(int offsetX, int offsetY, int offsetZ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getOffsetZ() {
        return offsetZ;
    }

}
