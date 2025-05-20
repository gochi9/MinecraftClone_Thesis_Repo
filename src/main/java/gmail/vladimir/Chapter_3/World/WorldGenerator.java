package gmail.vladimir.Chapter_3.World;

import gmail.vladimir.Chapter_3.World.Block.Block;
import gmail.vladimir.Chapter_3.World.Block.BlockType;
import gmail.vladimir.Chapter_3.World.Chunk.Noise.FastNoiseLite;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class WorldGenerator {

    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 320;
    public static final int CHUNK_SIZE_Z = 16;
    public static final int Y_OFFSET = -64;
    public static final int SEA_LEVEL = 75 + Math.abs(Y_OFFSET);

    private final Random rand;
    private final FastNoiseLite continentalnessNoise;
    private final FastNoiseLite elevationNoise;
    private final FastNoiseLite detailNoise;
    private final FastNoiseLite flatlandNoise;

    public WorldGenerator(long seed) {
        this.rand = new Random(seed);
        this.continentalnessNoise = new FastNoiseLite(rand.nextInt());
        continentalnessNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2S);
        continentalnessNoise.SetFrequency(0.0005f);
        continentalnessNoise.SetFractalType(FastNoiseLite.FractalType.None);

        this.elevationNoise = new FastNoiseLite(rand.nextInt());
        elevationNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        elevationNoise.SetFrequency(0.0015f);
        elevationNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        elevationNoise.SetFractalOctaves(4);

        this.detailNoise = new FastNoiseLite(rand.nextInt());
        detailNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        detailNoise.SetFrequency(0.01f);
        detailNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        detailNoise.SetFractalOctaves(3);

        flatlandNoise = new FastNoiseLite(rand.nextInt());
        flatlandNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        flatlandNoise.SetFrequency(0.0007f);
    }

    public CompletableFuture<Block[][][]> generateChunkAsync(int cx, int cz) {
        return CompletableFuture.supplyAsync(() -> generateChunk(cx, cz));
    }

    public Block[][][] generateChunk(int chunkX, int chunkZ) {
        Block[][][] blocks = new Block[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];
        int worldXOffset = chunkX * CHUNK_SIZE_X;
        int worldZOffset = chunkZ * CHUNK_SIZE_Z;

        for (int x = 0; x < CHUNK_SIZE_X; x++)
        for (int z = 0; z < CHUNK_SIZE_Z; z++) {
            float worldX = worldXOffset + x;
            float worldZ = worldZOffset + z;

            double avgHeight = (getRawHeight(worldX, worldZ) + getRawHeight(worldX + 1, worldZ) + getRawHeight(worldX, worldZ + 1) + getRawHeight(worldX + 1, worldZ + 1)) / 4.0;

            int height = (int) Math.floor(avgHeight) + Math.abs(Y_OFFSET);
            int dirtLevel = rand.nextInt(6) + 3;
            for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                if(y < 3){
                    blocks[x][y][z] = new Block(BlockType.BEDROCK, x, y, z);
                    continue;
                }

                if (y == height)
                    blocks[x][y][z] = new Block(y >= SEA_LEVEL ? BlockType.GRASS : BlockType.SAND, x, y, z);
                else if(y >= height - dirtLevel && y < height)
                    blocks[x][y][z] = new Block(y >= SEA_LEVEL ? BlockType.DIRT : BlockType.SAND, x, y, z);
                else if(y < height)
                    blocks[x][y][z] = new Block(BlockType.STONE, x, y, z);
                else if(y < SEA_LEVEL)
                    blocks[x][y][z] = new Block(BlockType.WATER, x, y, z);
            }
        }

        return blocks;
    }

    private double getRawHeight(float x, float z) {
        float cont = (continentalnessNoise.GetNoise(x, z) + 1f) / 2f;
        float elev = (elevationNoise.GetNoise(x, z) + 1f) / 2f;
        float detail = (detailNoise.GetNoise(x, z) + 1f) / 2f;
        float flat = (flatlandNoise.GetNoise(x, z) + 1f) / 2f;

        //ocean
        if (cont < 0.3f)
            return 40; //sea level

        //flat
        if (flat > 0.65f)
            return 65;

        //bias elevation
        elev = (float) Math.pow(elev, 1.6f);

        //kill bumps in low elevation zones
        if (elev < 0.2f) detail = 0.5f;

        double height = smoothInterpolate(0, 1, 60, 180, elev);
        height += (detail - 0.5f) * 20f;

        //plateaus
        if (elev > 0.6f && detail > 0.55f)
            height = Math.round(height / 12f) * 12f;

        return height;
    }


    public static double smoothInterpolate(double a, double b, double valA, double valB, double x) {
        x = Math.max(a, Math.min(b, x));

        double t = (x - a) / (b - a);
        t = t * t * (3 - 2 * t);

        return valA + (valB - valA) * t;
    }

} 