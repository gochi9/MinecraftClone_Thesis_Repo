package gmail.vladimir.Chapter_3.World;

import gmail.vladimir.Chapter_3.World.Block.*;
import gmail.vladimir.Chapter_3.World.Chunk.Chunk;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class ChunkSQLiteStore implements AutoCloseable {

    private static final Path DB_FOLDER = Paths.get("file", "world");
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FOLDER.resolve("world.db");
    private static final int CHUNK_BYTES = Chunk.CHUNK_SIZE_X * Chunk.CHUNK_SIZE_Y * Chunk.CHUNK_SIZE_Z;

    private final Connection conn;
    private final PreparedStatement psPut;
    private final PreparedStatement psGetWorldInfo;

    public ChunkSQLiteStore() throws SQLException, IOException {
        Files.createDirectories(DB_FOLDER);
        conn = DriverManager.getConnection(JDBC_URL);

        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute("PRAGMA synchronous=NORMAL;");
            st.execute("PRAGMA temp_store=MEMORY;");
        }

        conn.setAutoCommit(false);

        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS chunks (
                    key  INTEGER PRIMARY KEY,
                    data BLOB NOT NULL
                );""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS meta (
                    id   INTEGER PRIMARY KEY CHECK (id = 0),
                    seed INTEGER NOT NULL,
                    x    REAL,
                    y    REAL,
                    z    REAL
                );""");
        }

        psPut = conn.prepareStatement("INSERT OR REPLACE INTO chunks(key,data) VALUES(?,?)");
        psGetWorldInfo = conn.prepareStatement("SELECT seed, x, y, z FROM meta WHERE id = 0");
    }

    public void loadAllChunks(int playerChunkX, int playerChunkZ, int LOAD_DISTANCE, Consumer<Chunk> consumer) throws SQLException {
        Set<Integer> requiredKeys = new HashSet<>();

        for (int dx = -LOAD_DISTANCE; dx <= LOAD_DISTANCE; dx++) {
            for (int dz = -LOAD_DISTANCE; dz <= LOAD_DISTANCE; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) > LOAD_DISTANCE)
                    continue;
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                requiredKeys.add(getChunkKey(chunkX, chunkZ));
            }
        }

        if (requiredKeys.isEmpty())
            return;

        StringBuilder sqlBuilder = new StringBuilder("SELECT key, data FROM chunks WHERE key IN (");
        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < requiredKeys.size(); i++)
            placeholders.add("?");

        sqlBuilder.append(placeholders).append(")");
        String sql = sqlBuilder.toString();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (int key : requiredKeys)
                ps.setInt(index++, key);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int key = rs.getInt(1);
                    byte[] blob = rs.getBytes(2);
                    int x = (short) (key >> 16);
                    int z = (short) key;
                    Block[][][] blocks = new Block[Chunk.CHUNK_SIZE_X][Chunk.CHUNK_SIZE_Y][Chunk.CHUNK_SIZE_Z];
                    fromByteArray(blocks, blob);
                    Chunk chunk = new Chunk(x, z);
                    chunk.setBlocks(blocks);
                    consumer.accept(chunk);
                }
            }
        }
    }

    public void saveChunk(Chunk chunk) {
        try{
            byte[] payload = toByteArray(chunk);
            int key = getChunkKey(chunk.getChunkX(), chunk.getChunkZ());
            psPut.setInt(1, key);
            psPut.setBytes(2, payload);
            psPut.addBatch();
        }
        catch (Throwable e){
            e.printStackTrace();
        }
    }

    public CompletableFuture<Block[][][]> loadChunkAsync(int x, int z){
        return CompletableFuture.supplyAsync(() -> loadChunk(x, z));
    }

    public Block[][][] loadChunk(int x, int z) {
        int key = getChunkKey(x, z);
        String sql = "SELECT data FROM chunks WHERE key = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                byte[] data = rs.getBytes(1);
                if (data == null) return null;
                Block[][][] blocks = new Block[Chunk.CHUNK_SIZE_X][Chunk.CHUNK_SIZE_Y][Chunk.CHUNK_SIZE_Z];
                fromByteArray(blocks, data);
                return blocks;
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public void flush() throws SQLException {
        psPut.executeBatch();
        conn.commit();
    }

    public Optional<WorldInfo> loadWorldInfo() throws SQLException {
        try (ResultSet rs = psGetWorldInfo.executeQuery()) {
            if (rs.next()) {
                long seed = rs.getLong("seed");
                float x = rs.getFloat("x");
                float y = rs.getFloat("y");
                float z = rs.getFloat("z");
                return Optional.of(new WorldInfo(seed, x, y, z));
            }
            else
                return Optional.empty();
        }
    }

    public void saveWorldMeta(long seed, double x, double y, double z) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO meta(id,seed,x,y,z) VALUES(0,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET seed=?,x=?,y=?,z=?""")) {
            ps.setLong(1, seed); ps.setDouble(2, x); ps.setDouble(3, y); ps.setDouble(4, z);
            ps.setLong(5, seed); ps.setDouble(6, x); ps.setDouble(7, y); ps.setDouble(8, z);
            ps.executeUpdate();
            conn.commit();
        }
    }

    private static int getChunkKey(int x, int z) {
        return (x << 16) | (z & 0xFFFF);
    }

    private static byte[] toByteArray(Chunk chunk) {
        byte[] out = new byte[CHUNK_BYTES];
        int i = 0;
        for (int y = 0; y < Chunk.CHUNK_SIZE_Y; y++)
        for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++)
        for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
            Block b = chunk.getBlock(x, y, z);
            out[i++] = (byte) (b == null ? BlockType.AIR.ordinal() : b.getType().ordinal());
        }

        return out;
    }

    private static void fromByteArray(Block[][][] blocks, byte[] in) {
        int i = 0;
        for (int y = 0; y < Chunk.CHUNK_SIZE_Y; y++)
        for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++)
        for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
            int ordinal = in[i++] & 0xFF;

            if (ordinal == BlockType.AIR.ordinal())
                continue;

            blocks[x][y][z] = new Block(BlockType.values()[ordinal], x, y, z);
        }
    }

    @Override
    public void close() throws SQLException {
        flush();
        conn.close();
    }
}
