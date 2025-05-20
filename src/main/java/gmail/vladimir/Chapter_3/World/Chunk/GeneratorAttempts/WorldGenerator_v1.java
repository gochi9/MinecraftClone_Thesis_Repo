package gmail.vladimir.Chapter_3.World.Chunk.GeneratorAttempts;

import gmail.vladimir.Chapter_3.World.Block.Block;
import gmail.vladimir.Chapter_3.World.Block.BlockType;
import gmail.vladimir.Chapter_3.World.Chunk.Chunk;

import java.util.concurrent.CompletableFuture;

//Extremely rudimentary world generator. It uses sin with certain settings that create endless repeating 'hills'
//No variation
public class WorldGenerator_v1 {

    public static final int CHUNK_SIZE_X = Chunk.CHUNK_SIZE_X;
    public static final int CHUNK_SIZE_Y = Chunk.CHUNK_SIZE_Y;
    public static final int CHUNK_SIZE_Z = Chunk.CHUNK_SIZE_Z;
    public static final int Y_OFFSET = Chunk.Y_OFFSET;

    private final double frequency = 0.1;
    private final double amplitude = 10;
    private final int worldHeight = 100;

    public WorldGenerator_v1() {
    }

    public CompletableFuture<Block[][][]> generateChunkAsync(int cx, int cz) {
        return CompletableFuture.supplyAsync(() -> generateChunk(cx, cz));
    }

    public Block[][][] generateChunk(int chunkX, int chunkZ) {
        Block[][][] blocks = new Block[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];

        chunkX = chunkX * CHUNK_SIZE_X;
        chunkZ = chunkZ * CHUNK_SIZE_Z;

        for (int x = 0; x < CHUNK_SIZE_X; x++)
        for (int z = 0; z < CHUNK_SIZE_Z; z++) {
            double terrainHeight = worldHeight + (Math.sin(chunkX + x * frequency) * amplitude) + (Math.sin(chunkZ + z * frequency) * amplitude);

            int height = (int) (Y_OFFSET + terrainHeight);

            for (int y = 0; y < CHUNK_SIZE_Y; y++)
                if (y <= height)
                    blocks[x][y][z] = new Block(BlockType.STONE, x, y, z);
        }

        return blocks;
    }

}