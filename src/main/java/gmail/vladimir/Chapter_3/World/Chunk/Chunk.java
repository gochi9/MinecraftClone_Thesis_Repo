package gmail.vladimir.Chapter_3.World.Chunk;

import gmail.vladimir.Chapter_3.Display.TextureAtlas;
import gmail.vladimir.Chapter_3.Display.TextureRegion;
import gmail.vladimir.Chapter_3.World.Block.*;
import gmail.vladimir.Chapter_3.World.World;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Chunk {

    public static final int CHUNK_SIZE_X = 16;
    public static final int CHUNK_SIZE_Y = 255;
    public static final int CHUNK_SIZE_Z = 16;
    public static final int Y_OFFSET = 64;

    private byte[][][] blockTypes;
    private final int chunkX, chunkZ;

    private int vaoId = 0;
    private int vboVerticesId = 0;
    private int vboTexCoordsId = 0;
    private int vboBrightnessId = 0;
    private int vboTintColorId = 0;

    private int vertexCount;
    private boolean needsUpdate;

    private Future<MeshData> meshFuture = null;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blockTypes = new byte[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];
    }

    public void setBlocks(Block[][][] blocks) {
        for (int x = 0; x < CHUNK_SIZE_X; x++)
        for (int y = 0; y < CHUNK_SIZE_Y; y++)
        for (int z = 0; z < CHUNK_SIZE_Z; z++) {
            if (blocks[x][y][z] != null)
                blockTypes[x][y][z] = (byte) blocks[x][y][z].getType().ordinal();
            else
                blockTypes[x][y][z] = (byte) BlockType.AIR.ordinal();
        }
        setNeedsUpdate(true);
    }

    public Block getBlock(int x, int y, int z) {
        if (!isValidPosition(x, y, z))
            return null;

        byte blockTypeId = blockTypes[x][y][z];
        if (blockTypeId == BlockType.AIR.ordinal())
            return null;

        BlockType type = BlockType.values()[blockTypeId];
        return new Block(type, x, y, z);
    }

    public void setBlock(int x, int y, int z, Block block) {
        if (!isValidPosition(x, y, z))
            return;

        if (block == null)
            blockTypes[x][y][z] = (byte) BlockType.AIR.ordinal();
        else
            blockTypes[x][y][z] = (byte) block.getType().ordinal();

        setNeedsUpdate();
    }

    private boolean isValidPosition(int x, int y, int z) {
        return x >= 0 && x < CHUNK_SIZE_X && y >= 0 && y < CHUNK_SIZE_Y && z >= 0 && z < CHUNK_SIZE_Z;
    }

    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }

    private boolean reloadSurrounding;

    public void updateMesh(World world) {
        if (!needsUpdate)
            return;

        if (meshFuture == null) {
            meshFuture = ChunkMeshGenerator.getExecutor().submit(() -> computeMeshData(world));
            return;
        }

        if (!meshFuture.isDone())
            return;

        try {
            MeshData meshData = meshFuture.get();
            uploadToGPU(meshData.vertices, meshData.texCoords, meshData.brightness, meshData.tintColors);
            vertexCount = meshData.vertices.size() / 3;
            needsUpdate = false;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        meshFuture = null;

        if(!reloadSurrounding)
            return;

        reloadSurrounding = false;
        for(int i = -1; i <= 1; i++)
        for(int j = -1; j <= 1; j++){
            if(i == 0 && j == 0)
                continue;

            if(world.getChunk(chunkX + i, chunkZ + j) instanceof Chunk c)
                c.setNeedsUpdate(false);
        }
    }

    public boolean needsUpdate() {
        return needsUpdate;
    }

    public void setNeedsUpdate() {
        setNeedsUpdate(false);
    }

    public void setNeedsUpdate(boolean reloadSurrounding) {
        this.needsUpdate = true;
        this.reloadSurrounding = reloadSurrounding;
    }

    private boolean shouldRenderFace(World world, int x, int y, int z, BlockFace face) {
        int nx = x + face.getOffsetX();
        int ny = y + face.getOffsetY();
        int nz = z + face.getOffsetZ();

        if (nx >= 0 && nx < CHUNK_SIZE_X && ny >= 0 && ny < CHUNK_SIZE_Y && nz >= 0 && nz < CHUNK_SIZE_Z)
            return blockTypes[nx][ny][nz] == BlockType.AIR.ordinal();

        byte neighborBlockType = getNeighborBlockType(world, nx, ny, nz);
        return neighborBlockType == BlockType.AIR.ordinal();
    }

    private byte getNeighborBlockType(World world, int x, int y, int z) {
        int nx = x;
        int ny = y;
        int nz = z;

        int neighborChunkX = chunkX;
        int neighborChunkZ = chunkZ;

        if (x < 0) {
            neighborChunkX -= 1;
            nx = CHUNK_SIZE_X - 1;
        }
        else if (x >= CHUNK_SIZE_X) {
            neighborChunkX += 1;
            nx = 0;
        }

        if (z < 0) {
            neighborChunkZ -= 1;
            nz = CHUNK_SIZE_Z - 1;
        }
        else if (z >= CHUNK_SIZE_Z) {
            neighborChunkZ += 1;
            nz = 0;
        }

        if (ny < 0 || ny >= CHUNK_SIZE_Y)
            return (byte) BlockType.AIR.ordinal();

        Chunk neighborChunk = world.getChunk(neighborChunkX, neighborChunkZ);
        if (neighborChunk == null)
            return (byte) BlockType.STONE.ordinal();

        return neighborChunk.blockTypes[nx][ny][nz];
    }

    private static final float[][] FACE_VERTICES = new float[][] {
            //u
            {0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1},
            //d
            {0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1},
            //w
            {0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0},
            //e
            {1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0},
            //n
            {0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1},
            //s
            {0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0}
    };

    private static final float[] FACE_BRIGHTNESS = {
            0.9f, //u
            0.1f, //d
            0.7f, //w
            0.7f, //e
            0.7f, //n
            0.7f  //s
    };

    private void addFace(List<Float> vertices, List<Float> texCoords, List<Float> brightness, List<Float> tintColors, BlockFace face, float x, float y, float z, String textureKey) {

        float[] faceVerts = FACE_VERTICES[face.ordinal()];
        for (int i = 0; i < faceVerts.length; i += 3) {
            vertices.add(x + faceVerts[i]);
            vertices.add(y + faceVerts[i+1]);
            vertices.add(z + faceVerts[i+2]);
        }

        TextureRegion region = TextureAtlas.getRegion(textureKey);
        if (region == null)
            region = new TextureRegion(0, 0, 1, 1);

        texCoords.add(region.u());
        texCoords.add(region.v());
        texCoords.add(region.u() + region.width());
        texCoords.add(region.v());
        texCoords.add(region.u() + region.width());
        texCoords.add(region.v() + region.height());
        texCoords.add(region.u());
        texCoords.add(region.v() + region.height());

        float br = FACE_BRIGHTNESS[face.ordinal()];
        for (int i = 0; i < 4; i++)
            brightness.add(br);

        float r = 1f, g = 1f, b = 1f;
        if (textureKey.equals("grass_top")) {
            r = 0.6f;
            g = 1.0f;
            b = 0.6f;
        }

        for (int i = 0; i < 4; i++) {
            tintColors.add(r);
            tintColors.add(g);
            tintColors.add(b);
        }
    }

    private MeshData computeMeshData(World world) {
        List<Float> vertices = new ArrayList<>(4096);
        List<Float> texCoords = new ArrayList<>(2730);
        List<Float> brightness = new ArrayList<>(1365);
        List<Float> tintColors = new ArrayList<>(4096);

        int[][] minY = new int[CHUNK_SIZE_X][CHUNK_SIZE_Z];
        int[][] maxY = new int[CHUNK_SIZE_X][CHUNK_SIZE_Z];

        for (int x = 0; x < CHUNK_SIZE_X; x++)
        for (int z = 0; z < CHUNK_SIZE_Z; z++) {
            minY[x][z] = -1;
            maxY[x][z] = -1;

            for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                if (blockTypes[x][y][z] != BlockType.AIR.ordinal()) {
                    if (minY[x][z] == -1)
                        minY[x][z] = y;
                    maxY[x][z] = y;
                }
            }
        }

        for (int x = 0; x < CHUNK_SIZE_X; x++)
        for (int z = 0; z < CHUNK_SIZE_Z; z++) {
            if (minY[x][z] == -1)
                continue;

            for (int y = minY[x][z]; y <= maxY[x][z]; y++) {
                byte blockTypeId = blockTypes[x][y][z];
                if (blockTypeId == BlockType.AIR.ordinal())
                    continue;

                BlockType type = BlockType.values()[blockTypeId];

                float blockX = chunkX * CHUNK_SIZE_X + x;
                float blockY = y - Y_OFFSET;
                float blockZ = chunkZ * CHUNK_SIZE_Z + z;

                for (BlockFace face : BlockFace.values()) {
                    if (shouldRenderFace(world, x, y, z, face)) {
                        String textureKey = BlockRegistry.getTextureKey(type, face);
                        addFace(vertices, texCoords, brightness, tintColors, face, blockX, blockY, blockZ, textureKey);
                    }
                }
            }
        }

        return new MeshData(vertices, texCoords, brightness, tintColors);
    }

    protected void uploadToGPU(List<Float> vertices, List<Float> texCoords, List<Float> brightness, List<Float> tintColors) {
        FloatBuffer vertexBuffer = createFloatBuffer(vertices);
        FloatBuffer texCoordBuffer = createFloatBuffer(texCoords);
        FloatBuffer brightnessBuffer = createFloatBuffer(brightness);
        FloatBuffer tintColorBuffer = createFloatBuffer(tintColors);

        if (vertices.isEmpty()) {
            vertexCount = 0;
            return;
        }

        if (vaoId == 0) {
            vaoId = glGenVertexArrays();
            vboVerticesId = glGenBuffers();
            vboTexCoordsId = glGenBuffers();
            vboBrightnessId = glGenBuffers();
            vboTintColorId = glGenBuffers();
        }

        glBindVertexArray(vaoId);

        glBindBuffer(GL_ARRAY_BUFFER, vboVerticesId);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, vboTexCoordsId);
        glBufferData(GL_ARRAY_BUFFER, texCoordBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, vboBrightnessId);
        glBufferData(GL_ARRAY_BUFFER, brightnessBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, vboTintColorId);
        glBufferData(GL_ARRAY_BUFFER, tintColorBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private FloatBuffer createFloatBuffer(List<Float> data) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.size());
        for (Float value : data)
            buffer.put(value);

        buffer.flip();
        return buffer;
    }

    public void render() {
        if (vaoId == 0 || vertexCount == 0)
            return;

        glBindVertexArray(vaoId);
        glDrawArrays(GL_QUADS, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void cleanUp() {
        if (vaoId != 0) {
            glDeleteVertexArrays(vaoId);
            vaoId = 0;
        }
        if (vboVerticesId != 0) {
            glDeleteBuffers(vboVerticesId);
            vboVerticesId = 0;
        }
        if (vboTexCoordsId != 0) {
            glDeleteBuffers(vboTexCoordsId);
            vboTexCoordsId = 0;
        }
        if(vboBrightnessId != 0){
            glDeleteBuffers(vboBrightnessId);
            vboBrightnessId = 0;
        }
        if(vboTintColorId != 0){
            glDeleteBuffers(vboTintColorId);
            vboTintColorId = 0;
        }
    }

    private static class MeshData {
        List<Float> vertices;
        List<Float> texCoords;
        List<Float> brightness;
        List<Float> tintColors;

        MeshData(List<Float> vertices, List<Float> texCoords, List<Float> brightness, List<Float> tintColors) {
            this.vertices = vertices;
            this.texCoords = texCoords;
            this.brightness = brightness;
            this.tintColors = tintColors;
        }
    }
}







///// OLD VER
//package gmail.vladimir.Chapter_3.World.Chunk;
//
//import gmail.vladimir.Chapter_3.Display.TextureAtlas;
//import gmail.vladimir.Chapter_3.Display.TextureRegion;
//import gmail.vladimir.Chapter_3.World.Block.*;
//        import gmail.vladimir.Chapter_3.World.World;
//import org.lwjgl.BufferUtils;
//
//import java.nio.FloatBuffer;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.Future;
//
//import static org.lwjgl.opengl.GL15.*;
//        import static org.lwjgl.opengl.GL20.*;
//        import static org.lwjgl.opengl.GL30.*;
//
//public class Chunk {
//
//    public static final int CHUNK_SIZE_X = 16;
//    public static final int CHUNK_SIZE_Y = 255;
//    public static final int CHUNK_SIZE_Z = 16;
//    public static final int Y_OFFSET = 64;
//
//    private Block[][][] blocks;
//    private final int chunkX, chunkZ;
//
//    private int vaoId = 0;
//    private int vboVerticesId = 0;
//    private int vboTexCoordsId = 0;
//    private int verticesBufferCapacity = 0;
//    private int texCoordsBufferCapacity = 0;
//    private int vboBrightnessId = 0;
//    private int brightnessBufferCapacity = 0;
//    private int vboTintColorId = 0;
//    private int tintColorBufferCapacity = 0;
//
//    private int vertexCount;
//    private boolean needsUpdate;
//
//    private Future<MeshData> meshFuture = null;
//
//    public Chunk(int chunkX, int chunkZ) {
//        this.chunkX = chunkX;
//        this.chunkZ = chunkZ;
//        this.blocks = new Block[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];
//        //  setNeedsUpdate();
//    }
//
//    public void setBlocks(Block[][][] blocks) {
//        this.blocks = blocks;
//        setNeedsUpdate(true);
//    }
//
//    public Block getBlock(int x, int y, int z) {
//        return isValidPosition(x, y, z) ? blocks[x][y][z] : null;
//    }
//
//    public void setBlock(int x, int y, int z, Block block) {
//        if (!isValidPosition(x, y, z))
//            return;
//
//        blocks[x][y][z] = block;
//        setNeedsUpdate();
//    }
//
//    private boolean isValidPosition(int x, int y, int z) {
//        return x >= 0 && x < CHUNK_SIZE_X && y >= 0 && y < CHUNK_SIZE_Y && z >= 0 && z < CHUNK_SIZE_Z;
//    }
//
//    public int getChunkX() { return chunkX; }
//    public int getChunkZ() { return chunkZ; }
//
//    private boolean reloadSurrounding;
//
//    public void updateMesh(World world) {
//        if (!needsUpdate)
//            return;
//
//        if (meshFuture == null) {
//            meshFuture = ChunkMeshGenerator.getExecutor().submit(() -> computeMeshData(world));
//            return;
//        }
//
//        if (!meshFuture.isDone())
//            return;
//
//        try {
//            MeshData meshData = meshFuture.get();
//            uploadToGPU(meshData.vertices, meshData.texCoords, meshData.brightness, meshData.tintColors);
//            vertexCount = meshData.vertices.size() / 3;
//            needsUpdate = false;
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        meshFuture = null;
//
//        if(!reloadSurrounding)
//            return;
//
//        reloadSurrounding = false;
//        for(int i = -1; i <= 1; i++)
//            for(int j = -1; j <= 1; j++){
//                if(i == 0 && j == 0)
//                    continue;
//
//                if(world.getChunk(chunkX + i, chunkZ + j) instanceof Chunk c)
//                    c.setNeedsUpdate(false);
//            }
//    }
//
//    public boolean needsUpdate() {
//        return needsUpdate;
//    }
//
//    public void setNeedsUpdate() {
//        setNeedsUpdate(false);
//    }
//
//    public void setNeedsUpdate(boolean reloadSurrounding) {
//        this.needsUpdate = true;
//        this.reloadSurrounding = reloadSurrounding;
//    }
//
//    private boolean shouldRenderFace(World world, int x, int y, int z, BlockFace face) {
//        Block neighbor = getNeighborBlock(world, x + face.getOffsetX(), y + face.getOffsetY(), z + face.getOffsetZ());
//        return neighbor == null || neighbor.getType() == BlockType.AIR;
//    }
//
//    private Block getNeighborBlock(World world, int x, int y, int z) {
//        int nx = x;
//        int ny = y;
//        int nz = z;
//
//        int neighborChunkX = chunkX;
//        int neighborChunkZ = chunkZ;
//
//        if (x < 0) {
//            neighborChunkX -= 1;
//            nx = CHUNK_SIZE_X - 1;
//        }
//        else if (x >= CHUNK_SIZE_X) {
//            neighborChunkX += 1;
//            nx = 0;
//        }
//
//        if (z < 0) {
//            neighborChunkZ -= 1;
//            nz = CHUNK_SIZE_Z - 1;
//        }
//        else if (z >= CHUNK_SIZE_Z) {
//            neighborChunkZ += 1;
//            nz = 0;
//        }
//
//        if (ny < 0 || ny >= CHUNK_SIZE_Y)
//            return null;
//
//        Chunk neighborChunk = world.getChunk(neighborChunkX, neighborChunkZ);
//        if (neighborChunk == null)
//            return new Block(BlockType.STONE, nx, ny, nz);
//
//        return neighborChunk.getBlock(nx, ny, nz);
//    }
//
//    private void addFace(List<Float> vertices, List<Float> texCoords, List<Float> brightness, List<Float> tintColors, BlockFace face, float x, float y, float z, String textureKey) {
//        float[] faceVerts = getFaceVertices(face, x, y, z);
//        for (float v : faceVerts)
//            vertices.add(v);
//
//        TextureRegion region = TextureAtlas.getRegion(textureKey);
//        if (region == null)
//            region = new TextureRegion(0, 0, 1, 1);
//
//        texCoords.add(region.u());
//        texCoords.add(region.v());
//
//        texCoords.add(region.u() + region.width());
//        texCoords.add(region.v());
//
//        texCoords.add(region.u() + region.width());
//        texCoords.add(region.v() + region.height());
//
//        texCoords.add(region.u());
//        texCoords.add(region.v() + region.height());
//
//        float br = switch (face) {
//            case UP    -> 0.9f;
//            case SOUTH, EAST, NORTH, WEST -> 0.7f;
//            case DOWN  -> 0.1f;
//        };
//
//        for (int i = 0; i < 4; i++)
//            brightness.add(br);
//
//        float r = 1f, g = 1f, b = 1f;
//
//        if (textureKey.equals("grass_top")) {
//            r = 0.6f * b;
//            g = 1.0f * b;
//            b = 0.6f * b;
//        }
//
//        for (int i = 0; i < 4; i++) {
//            tintColors.add(r);
//            tintColors.add(g);
//            tintColors.add(b);
//        }
//    }
//
//    private float[] getFaceVertices(BlockFace face, float x, float y, float z) {
//        return switch (face) {
//            case UP    -> new float[]{x, y + 1, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, x, y + 1, z + 1};
//            case DOWN  -> new float[]{x, y, z, x + 1, y, z, x + 1, y, z + 1, x, y, z + 1};
//            case WEST  -> new float[]{x, y, z, x, y, z + 1, x, y + 1, z + 1, x, y + 1, z};
//            case EAST  -> new float[]{x + 1, y, z, x + 1, y, z + 1, x + 1, y + 1, z + 1, x + 1, y + 1, z};
//            case NORTH -> new float[]{x, y, z + 1, x + 1, y, z + 1, x + 1, y + 1, z + 1, x, y + 1, z + 1};
//            case SOUTH -> new float[]{x, y, z, x + 1, y, z, x + 1, y + 1, z, x, y + 1, z};
//        };
//    }
//
//
//    private MeshData computeMeshData(World world) {
//        List<Float> vertices = new ArrayList<>();
//        List<Float> texCoords = new ArrayList<>();
//        List<Float> brightness = new ArrayList<>();
//        List<Float> tintColors = new ArrayList<>();
//
//        for (int x = 0; x < CHUNK_SIZE_X; x++)
//            for (int z = 0; z < CHUNK_SIZE_Z; z++) {
//                int minY = -1, maxY = -1;
//                for (int y = 0; y < CHUNK_SIZE_Y; y++) {
//                    Block block = blocks[x][y][z];
//                    if (block == null || block.getType() == BlockType.AIR)
//                        continue;
//
//                    if (minY == -1)
//                        minY = y;
//
//                    maxY = y;
//                }
//
//                if (minY == -1)
//                    continue;
//
//                for (int y = minY; y <= maxY; y++) {
//                    Block block = blocks[x][y][z];
//                    if (block == null || block.getType() == BlockType.AIR)
//                        continue;
//
//                    float blockX = chunkX * CHUNK_SIZE_X + x;
//                    float blockY = y - Y_OFFSET;
//                    float blockZ = chunkZ * CHUNK_SIZE_Z + z;
//
//                    for (BlockFace face : BlockFace.values())
//                        if (shouldRenderFace(world, x, y, z, face))
//                            addFace(vertices, texCoords, brightness, tintColors, face, blockX, blockY, blockZ, BlockRegistry.getTextureKey(block.getType(), face));
//                }
//            }
//
//        return new MeshData(vertices, texCoords, brightness, tintColors);
//    }
//
//    protected void uploadToGPU(List<Float> vertices, List<Float> texCoords, List<Float> brightness, List<Float> tintColors) {
//        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.size());
//        for (Float v : vertices)
//            vertexBuffer.put(v);
//        vertexBuffer.flip();
//
//        FloatBuffer texCoordBuffer = BufferUtils.createFloatBuffer(texCoords.size());
//        for (Float t : texCoords)
//            texCoordBuffer.put(t);
//        texCoordBuffer.flip();
//
//        FloatBuffer brightnessBuffer = BufferUtils.createFloatBuffer(brightness.size());
//        for (Float b : brightness)
//            brightnessBuffer.put(b);
//        brightnessBuffer.flip();
//
//        FloatBuffer tintColorBuffer = BufferUtils.createFloatBuffer(tintColors.size());
//        for (Float c : tintColors)
//            tintColorBuffer.put(c);
//        tintColorBuffer.flip();
//
//        if (vaoId == 0) {
//            vaoId = glGenVertexArrays();
//            glBindVertexArray(vaoId);
//
//            vboVerticesId = glGenBuffers();
//            glBindBuffer(GL_ARRAY_BUFFER, vboVerticesId);
//            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
//            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
//            glEnableVertexAttribArray(0);
//            verticesBufferCapacity = vertexBuffer.capacity();
//
//            vboTexCoordsId = glGenBuffers();
//            glBindBuffer(GL_ARRAY_BUFFER, vboTexCoordsId);
//            glBufferData(GL_ARRAY_BUFFER, texCoordBuffer, GL_STATIC_DRAW);
//            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
//            glEnableVertexAttribArray(1);
//            texCoordsBufferCapacity = texCoordBuffer.capacity();
//
//            vboBrightnessId = glGenBuffers();
//            glBindBuffer(GL_ARRAY_BUFFER, vboBrightnessId);
//            glBufferData(GL_ARRAY_BUFFER, brightnessBuffer, GL_STATIC_DRAW);
//            glVertexAttribPointer(2, 1, GL_FLOAT, false, 0, 0);
//            glEnableVertexAttribArray(2);
//            brightnessBufferCapacity = brightnessBuffer.capacity();
//
//            vboTintColorId = glGenBuffers();
//            glBindBuffer(GL_ARRAY_BUFFER, vboTintColorId);
//            glBufferData(GL_ARRAY_BUFFER, tintColorBuffer, GL_STATIC_DRAW);
//            glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
//            glEnableVertexAttribArray(3);
//            tintColorBufferCapacity = tintColorBuffer.capacity();
//
//            glBindBuffer(GL_ARRAY_BUFFER, 0);
//            glBindVertexArray(0);
//            return;
//        }
//
//        glBindVertexArray(vaoId);
//
//        glBindBuffer(GL_ARRAY_BUFFER, vboVerticesId);
//        if (vertexBuffer.capacity() <= verticesBufferCapacity)
//            glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);
//
//        else {
//            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
//            verticesBufferCapacity = vertexBuffer.capacity();
//        }
//
//        glBindBuffer(GL_ARRAY_BUFFER, vboTexCoordsId);
//        if (texCoordBuffer.capacity() <= texCoordsBufferCapacity)
//            glBufferSubData(GL_ARRAY_BUFFER, 0, texCoordBuffer);
//
//        else {
//            glBufferData(GL_ARRAY_BUFFER, texCoordBuffer, GL_STATIC_DRAW);
//            texCoordsBufferCapacity = texCoordBuffer.capacity();
//        }
//
//        glBindBuffer(GL_ARRAY_BUFFER, vboBrightnessId);
//        if (brightnessBuffer.capacity() <= brightnessBufferCapacity)
//            glBufferSubData(GL_ARRAY_BUFFER, 0, brightnessBuffer);
//        else {
//            glBufferData(GL_ARRAY_BUFFER, brightnessBuffer, GL_STATIC_DRAW);
//            brightnessBufferCapacity = brightnessBuffer.capacity();
//        }
//
//        glBindBuffer(GL_ARRAY_BUFFER, vboTintColorId);
//        if (tintColorBuffer.capacity() <= tintColorBufferCapacity)
//            glBufferSubData(GL_ARRAY_BUFFER, 0, tintColorBuffer);
//        else {
//            glBufferData(GL_ARRAY_BUFFER, tintColorBuffer, GL_STATIC_DRAW);
//            tintColorBufferCapacity = tintColorBuffer.capacity();
//        }
//
//        glBindBuffer(GL_ARRAY_BUFFER, 0);
//        glBindVertexArray(0);
//    }
//
//    public void render() {
//        if (vaoId == 0 || vertexCount == 0)
//            return;
//
//        glBindVertexArray(vaoId);
//        glEnableVertexAttribArray(0);
//        glEnableVertexAttribArray(1);
//
//        glDrawArrays(GL_QUADS, 0, vertexCount);
//
//        glDisableVertexAttribArray(1);
//        glDisableVertexAttribArray(0);
//        glBindVertexArray(0);
//    }
//
//    public void cleanUp() {
//        if (vaoId != 0) {
//            glDeleteVertexArrays(vaoId);
//            vaoId = 0;
//        }
//        if (vboVerticesId != 0) {
//            glDeleteBuffers(vboVerticesId);
//            vboVerticesId = 0;
//        }
//        if (vboTexCoordsId != 0) {
//            glDeleteBuffers(vboTexCoordsId);
//            vboTexCoordsId = 0;
//        }
//        if(vboBrightnessId != 0){
//            glDeleteBuffers(vboBrightnessId);
//            vboBrightnessId = 0;
//        }
//        if(vboTintColorId != 0){
//            glDeleteBuffers(vboTintColorId);
//            vboTintColorId = 0;
//        }
//    }
//
//    private static class MeshData {
//        List<Float> vertices;
//        List<Float> texCoords;
//        List<Float> brightness;
//        List<Float> tintColors;
//        MeshData(List<Float> vertices, List<Float> texCoords, List<Float> brightness, List<Float> tintColors) {
//            this.vertices = vertices;
//            this.texCoords = texCoords;
//            this.brightness = brightness;
//            this.tintColors = tintColors;
//        }
//    }
//
//}
