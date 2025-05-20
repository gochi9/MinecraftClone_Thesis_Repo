package gmail.vladimir.Chapter_3.World.Chunk;

import gmail.vladimir.Chapter_3.Main;
import gmail.vladimir.Chapter_3.World.Block.Block;
import gmail.vladimir.Chapter_3.World.ChunkSQLiteStore;
import gmail.vladimir.Chapter_3.World.WorldGenerator;

import java.util.*;
import java.util.concurrent.*;

public class ChunkManager {

    private static final int LOAD_DISTANCE = Main.RENDER_DISTANCE;

    private final ChunkSQLiteStore chunkSQLiteStore;
    private final WorldGenerator worldGenerator;
    private final ExecutorService chunkThreadPool;
    private final Queue<ChunkLoadRequest> loadQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentMap<Integer, Chunk> activeChunks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Chunk> cachedChunks = new ConcurrentHashMap<>();

    public Collection<Chunk> getActiveChunks() {
        return activeChunks.values();
    }

    private static final Queue<Runnable> mainThreadTasks = new ConcurrentLinkedQueue<>();

    private final Set<Integer> processing = ConcurrentHashMap.newKeySet();

    private final long seed;
    public ChunkManager(long seed, ChunkSQLiteStore chunkSQLiteStore) {
        this.seed = seed;
        this.chunkSQLiteStore = chunkSQLiteStore;

        this.worldGenerator = new WorldGenerator(seed);
        this.chunkThreadPool = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors() - 1));
        startLoadConsumer();
    }

    Thread consumerThread;
    private void startLoadConsumer() {
        consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if(loadQueue.isEmpty())
                    continue;

                ChunkLoadRequest request = loadQueue.poll();

                int chunkKey = getChunkKey(request.chunkX, request.chunkZ);
                if(processing.contains(chunkKey))
                    continue;

                processing.add(chunkKey);
                chunkSQLiteStore.loadChunkAsync(request.chunkX, request.chunkZ).thenAcceptAsync(generatedChunk -> {
                    if (generatedChunk != null) {
                        mainThreadTasks.add(() -> {
                            Chunk chunk = new Chunk(request.chunkX, request.chunkZ);
                            chunk.setBlocks(generatedChunk);
                            activeChunks.put(chunkKey, chunk);
                            processing.remove(chunkKey);
                        });
                        return;
                    }

                    request.generate(worldGenerator).thenAcceptAsync(freshChunk -> {
                        mainThreadTasks.add(() -> {
                            Chunk chunk = new Chunk(request.chunkX, request.chunkZ);
                            chunk.setBlocks(freshChunk);
                            activeChunks.put(chunkKey, chunk);
                            processing.remove(chunkKey);
                        });
                    });
                });
            }
        }, "ChunkLoadConsumer");

        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    public static void addMainThreadTask(Runnable run){
        mainThreadTasks.add(run);
    }

    public void executeMainThreadTasks(){
        while (!mainThreadTasks.isEmpty())
            mainThreadTasks.poll().run();
    }

    private int getChunkKey(int x, int z) {
        return (x << 16) | (z & 0xFFFF);
    }

    public void requestChunkLoad(int chunkX, int chunkZ) {
        int key = getChunkKey(chunkX, chunkZ);

        if (!activeChunks.containsKey(key) && !cachedChunks.containsKey(key))
            loadQueue.offer(new ChunkLoadRequest(chunkX, chunkZ));

        else if (cachedChunks.containsKey(key))
            activeChunks.put(key, cachedChunks.remove(key));
    }

    public void addChunk(Chunk c){
        cachedChunks.put(getChunkKey(c.getChunkX(), c.getChunkZ()), c);
    }

    public void updatePlayerPosition(int playerChunkX, int playerChunkZ) {
        Set<Integer> requiredChunks = new HashSet<>();
        for (int dx = -LOAD_DISTANCE; dx <= LOAD_DISTANCE; dx++)
        for (int dz = -LOAD_DISTANCE; dz <= LOAD_DISTANCE; dz++) {
            if (Math.sqrt(dx * dx + dz * dz) > LOAD_DISTANCE)
                continue;

            int cx = playerChunkX + dx;
            int cz = playerChunkZ + dz;
            int key = getChunkKey(cx, cz);

            requiredChunks.add(key);
            requestChunkLoad(cx, cz);
        }


        for (Iterator<Map.Entry<Integer, Chunk>> it = activeChunks.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, Chunk> entry = it.next();
            if (requiredChunks.contains(entry.getKey()))
                continue;

            cachedChunks.put(entry.getKey(), entry.getValue());
            chunkSQLiteStore.saveChunk(entry.getValue());
            it.remove();
        }
    }

    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return activeChunks.containsKey(getChunkKey(chunkX, chunkZ));
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        return activeChunks.get(getChunkKey(chunkX, chunkZ));
    }

    public Collection<Chunk> getAllChunks(){
        Set<Chunk> chunk = new HashSet<>(activeChunks.values());
        chunk.addAll(cachedChunks.values());
        return chunk;
    }

    public void shutdown() {
        chunkThreadPool.shutdown();
        consumerThread.interrupt();

    }

    private static class ChunkLoadRequest {
        int chunkX, chunkZ;
        ChunkLoadRequest(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        CompletableFuture<Block[][][]> generate(WorldGenerator gen) {
            return gen.generateChunkAsync(chunkX, chunkZ);
        }
    }

}