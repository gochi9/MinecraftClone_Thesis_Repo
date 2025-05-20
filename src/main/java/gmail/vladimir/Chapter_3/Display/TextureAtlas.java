package gmail.vladimir.Chapter_3.Display;

import java.util.HashMap;
import java.util.Map;

public class TextureAtlas {

    private static final Map<String, TextureRegion> regions = new HashMap<>();
    public static final int ATLAS_SIZE = 512;
    public static final int TILE_SIZE = 16;
    public static final float TILE_UV = (float) TILE_SIZE / ATLAS_SIZE; //

    static {
        regions.put("grass_top", createRegion(15, 21));
        regions.put("dirt", createRegion(8, 25));
        regions.put("grass_side", createRegion(12, 21));
        regions.put("stone", createRegion(20, 22));
        regions.put("snow", createRegion(20, 26));
        regions.put("sand", createRegion(19, 24));
        regions.put("sandstone_top", createRegion(19, 19));
        regions.put("sandstone_side", createRegion(19, 21));
        regions.put("gravel", createRegion(0, 20));
        regions.put("log", createRegion(3, 17));
        regions.put("leaves", createRegion(3, 18));
        regions.put("water", createRegion(2, 31));
        regions.put("bedrock", createRegion(4, 28));
    }

    public static TextureRegion createRegion(int col, int row) {
        float pixel = 1.0f / ATLAS_SIZE;
        float padding = 0.5f * pixel;

        float u = col * TILE_UV + padding;
        float v = row * TILE_UV + padding;
        float size = TILE_UV - 3 * padding;

        return new TextureRegion(u, v, size, size);
    }

    public static TextureRegion getRegion(String textureKey) {
        return regions.get(textureKey);
    }

}
