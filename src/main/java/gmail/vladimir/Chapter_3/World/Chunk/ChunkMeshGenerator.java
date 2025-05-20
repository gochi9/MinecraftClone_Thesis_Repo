package gmail.vladimir.Chapter_3.World.Chunk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChunkMeshGenerator {

    private static final int CORE_COUNT = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
    private static final int MAX_QUEUE_SIZE = 1000;

    private static final ExecutorService executor = new ThreadPoolExecutor(
            CORE_COUNT,
            CORE_COUNT,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void shutdown() {
        executor.shutdownNow();
    }

}
