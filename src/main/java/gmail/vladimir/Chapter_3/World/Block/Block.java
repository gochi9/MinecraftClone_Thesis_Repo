package gmail.vladimir.Chapter_3.World.Block;

public class Block {

    public static final Block AIR = new Block(BlockType.AIR, 0, 0, 0);

    private final short key;
    private final BlockType type;

    public Block(BlockType type, int x, int y, int z) {
        x = x & 15;
        z = z & 15;

        if (y < 0 || y > 255)
            throw new IllegalArgumentException(y + "must be in [0, 255]");

        this.key = (short) ((x << 12) | (y << 4) | z);
        this.type = type;
    }

    public int getX() {
        return (key >> 12) & 0xF;
    }

    public int getY() {
        return (key >> 4) & 0xFF;
    }

    public int getZ() {
        return key & 0xF;
    }

    public BlockType getType() {
        return type;
    }
}