package gmail.vladimir.Chapter_3.World;

import gmail.vladimir.Chapter_3.World.Block.Block;
import gmail.vladimir.Chapter_3.World.Chunk.Chunk;
import gmail.vladimir.Chapter_3.World.Chunk.ChunkManager;

import java.util.Collection;

public class World {

    private final long seed;
    private final ChunkManager chunkManager;

    public World(long seed, ChunkSQLiteStore sqLiteStore) {
        this.seed = seed;
        this.chunkManager = new ChunkManager(seed, sqLiteStore);
    }

    public void update(int playerChunkX, int playerChunkZ) {
        chunkManager.updatePlayerPosition(playerChunkX, playerChunkZ);
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunkManager.getChunk(chunkX, chunkZ);
    }

    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public boolean canMoveTo(float x, float y, float z) {
        int chunkX = (int) Math.floor(x) >> 4;
        int chunkZ = (int) Math.floor(z) >> 4;
        return chunkManager.isChunkLoaded(chunkX, chunkZ);
    }

    public Collection<Chunk> getChunks() {
        return chunkManager.getActiveChunks();
    }

    public void shutdown() {
        chunkManager.shutdown();
    }

    public Block getBlockAt(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) return null;

        int lx = x & 15;
        int lz = z & 15;
        int ly = y + Chunk.Y_OFFSET;

        if (ly < 0 || ly >= Chunk.CHUNK_SIZE_Y) return null;

        return chunk.getBlock(lx, ly, lz);
    }

    public void setBlock(int x, int y, int z, Block block) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) return;

        int lx = x & 15;
        int lz = z & 15;
        int ly = y + Chunk.Y_OFFSET;  // Use the same conversion as in getBlockAt

        if (ly < 0 || ly >= Chunk.CHUNK_SIZE_Y) return;
        chunk.setBlock(lx, ly, lz, block);
        chunk.setNeedsUpdate();

        if (lx == 0) {
            Chunk leftNeighbor = getChunk(chunkX - 1, chunkZ);
            if (leftNeighbor != null) leftNeighbor.setNeedsUpdate();
        }
        if (lx == Chunk.CHUNK_SIZE_X - 1) {
            Chunk rightNeighbor = getChunk(chunkX + 1, chunkZ);
            if (rightNeighbor != null) rightNeighbor.setNeedsUpdate();
        }
        if (lz == 0) {
            Chunk backNeighbor = getChunk(chunkX, chunkZ - 1);
            if (backNeighbor != null) backNeighbor.setNeedsUpdate();
        }
        if (lz == Chunk.CHUNK_SIZE_Z - 1) {
            Chunk frontNeighbor = getChunk(chunkX, chunkZ + 1);
            if (frontNeighbor != null) frontNeighbor.setNeedsUpdate();
        }
    }

}