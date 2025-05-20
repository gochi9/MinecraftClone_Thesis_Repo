package gmail.vladimir.Chapter_3.World.Block;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class BlockRegistry {

    private static final Map<BlockType, Map<BlockFace, String>> blockTextures = new HashMap<>();

    static {
        registerBlock(BlockType.GRASS, "grass_top", "dirt", "grass_side");
        registerBlock(BlockType.DIRT, "dirt");
        registerBlock(BlockType.STONE, "stone");
        registerBlock(BlockType.SNOW, "snow");
        registerBlock(BlockType.SAND, "sand");
        registerBlock(BlockType.SANDSTONE, "sandstone_top", "sandstone_side", "sandstone_side");
        registerBlock(BlockType.GRAVEL, "gravel");
        registerBlock(BlockType.LOG, "log");
        registerBlock(BlockType.LEAVES, "leaves");
        registerBlock(BlockType.WATER, "water");
        registerBlock(BlockType.BEDROCK, "bedrock");
    }

    public static void registerBlock(BlockType type, String topTex, String bottomTex, String sideTex) {
        Map<BlockFace, String> faces = new EnumMap<>(BlockFace.class);
        faces.put(BlockFace.UP, topTex);
        faces.put(BlockFace.DOWN, bottomTex);
        faces.put(BlockFace.WEST, sideTex);
        faces.put(BlockFace.EAST, sideTex);
        faces.put(BlockFace.NORTH, sideTex);
        faces.put(BlockFace.SOUTH, sideTex);
        blockTextures.put(type, faces);
    }

    public static void registerBlock(BlockType type, String allFacesTex) {
        Map<BlockFace, String> faces = new EnumMap<>(BlockFace.class);

        for (BlockFace face : BlockFace.values())
            faces.put(face, allFacesTex);

        blockTextures.put(type, faces);
    }

    public static String getTextureKey(BlockType type, BlockFace face) {
        return blockTextures.getOrDefault(type, Collections.emptyMap()).get(face);
    }

    public static void addBlockType(BlockType type, Map<BlockFace, String> faceToTextureMap) {
        blockTextures.put(type, faceToTextureMap);
    }

    public static void removeBlockType(BlockType type) {
        blockTextures.remove(type);
    }

}
