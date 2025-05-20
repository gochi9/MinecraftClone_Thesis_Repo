package gmail.vladimir.Chapter_3.World.Block;

public enum BlockType {

    GRASS,
    DIRT,
    STONE,
    SNOW,
    SAND,
    SANDSTONE,
    GRAVEL,
    LOG,
    LEAVES,
    WATER,
    BEDROCK,
    AIR;

    public static BlockType type(short val){
        return values()[val];
    }

}