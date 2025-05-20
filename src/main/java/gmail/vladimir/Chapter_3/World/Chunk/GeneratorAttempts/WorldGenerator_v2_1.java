package gmail.vladimir.Chapter_3.World.Chunk.GeneratorAttempts;

import gmail.vladimir.Chapter_3.World.Block.Block;
import gmail.vladimir.Chapter_3.World.Block.BlockType;
import gmail.vladimir.Chapter_3.World.Chunk.Noise.FastNoiseLite;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

//Introducing octaves. Basically sampeling multiple responses from perlin noise and adding them together for more variation
public class WorldGenerator_v2_1 {

    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 320;
    public static final int CHUNK_SIZE_Z = 16;
    public static final int Y_OFFSET = -64;

    private final double amplitude = 20;
    private final int worldHeight = 100;
    private final float[] frequencies = {0.01f, 0.05f, 0.15f};
    private final float[] amplitudes = {20.0f, 10.0f, 5.0f};
    private final FastNoiseLite noise;

    public WorldGenerator_v2_1(long seed) {
        this.noise = new FastNoiseLite(new Random(seed).nextInt());
    }

    public CompletableFuture<Block[][][]> generateChunkAsync(int cx, int cz) {
        return CompletableFuture.supplyAsync(() -> generateChunk(cx, cz));
    }

    public Block[][][] generateChunk(int chunkX, int chunkZ) {
        Block[][][] blocks = new Block[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];
        chunkX = chunkX * CHUNK_SIZE_X;
        chunkZ = chunkZ * CHUNK_SIZE_Z;

        for (int x = 0; x < CHUNK_SIZE_X; x++) {
            for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                float worldX = chunkX + x;
                float worldZ = chunkZ + z;

                double totalNoise = 0.0;
                double totalAmplitude = 0.0;

                for (int i = 0; i < 3; i++) {
                    float nx = worldX * frequencies[i];
                    float nz = worldZ * frequencies[i];
                    totalNoise += noise.GetNoise(worldX + nx, worldZ + nz) * amplitudes[i];
                    totalAmplitude += amplitudes[i];
                }

                double normalizedNoise = totalNoise / totalAmplitude;
                double terrainHeight = worldHeight + (normalizedNoise * amplitude);

                int height = (int) (Y_OFFSET + terrainHeight);

                for (int y = 0; y < CHUNK_SIZE_Y; y++)
                    if (y <= height)
                        blocks[x][y][z] = new Block(BlockType.STONE, x, y, z);
            }
        }

        return blocks;
    }

}