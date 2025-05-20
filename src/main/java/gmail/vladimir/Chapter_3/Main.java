package gmail.vladimir.Chapter_3;

import gmail.vladimir.Chapter_3.Display.Shader;
import gmail.vladimir.Chapter_3.Display.Texture;
import gmail.vladimir.Chapter_3.World.Block.Block;
import gmail.vladimir.Chapter_3.World.Block.BlockType;
import gmail.vladimir.Chapter_3.World.Chunk.Chunk;
import gmail.vladimir.Chapter_3.World.Chunk.ChunkMeshGenerator;
import gmail.vladimir.Chapter_3.World.ChunkSQLiteStore;
import gmail.vladimir.Chapter_3.World.Entity.Player;
import gmail.vladimir.Chapter_3.World.World;
import gmail.vladimir.Chapter_3.World.WorldInfo;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.sql.SQLException;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Main {
    private int WIDTH = 1280;
    private int HEIGHT = 720;

    private long window;

    private World world;
    private Player player;

    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean moveUp = false;
    private boolean moveDown = false;

    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;

    private static final double TICK_RATE = 60.0;
    private static final double TIME_PER_TICK = 1.0 / TICK_RATE;
    private double lastTime = 0.0;

    public static final int RENDER_DISTANCE = 20;

    private Shader shader;

    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;
    private FloatBuffer matrixBuffer;

    ChunkSQLiteStore store = new ChunkSQLiteStore();

    public Main() throws SQLException, IOException {
    }

    public static void main(String[] args) throws SQLException, IOException {
        new Main().run();
    }

    public void run() throws SQLException {
        init();
        loop();
        cleanup();
    }

    private void init() throws SQLException {
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "Minecraft Clone", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null)
            glfwSetWindowPos(window, (vidmode.width() - WIDTH) / 2, (vidmode.height() - HEIGHT) / 2);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();
        glClearColor(0.3f, 0.5f, 0.8f, 0.0f);
        glEnable(GL_DEPTH_TEST);
        glActiveTexture(GL_TEXTURE0);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        WorldInfo info = store.loadWorldInfo().orElse(new WorldInfo(System.currentTimeMillis(), 0, 100, 0));

        world  = new World(info.seed(), store);
        player = new Player(info.x(), info.y(), info.z(), world);

        store.loadAllChunks(((int)info.x()) & 15, ((int)info.z() & 15), RENDER_DISTANCE, chunk -> world.getChunkManager().addChunk(chunk));

        glfwSetFramebufferSizeCallback(window, this::framebufferSizeCallback);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glfwSetCursorPosCallback(window, this::mouseCallback);
        glfwSetKeyCallback(window, this::keyCallback);

        lastTime = glfwGetTime();

        try {
            shader = new Shader("vertex_shader.glsl", "fragment_shader.glsl");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        projectionMatrix = new Matrix4f();
        viewMatrix = new Matrix4f();
        matrixBuffer = MemoryUtil.memAllocFloat(16);

        updateProjectionMatrix();

        glfwSetMouseButtonCallback(window, this::mouseButtonCallback);
        Texture.getTexture("texture_atlas.png").bind();
    }

    private void mouseButtonCallback(long window, int button, int action, int mods) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS)
            handleBlockRemoval();
    }

    private void handleBlockRemoval() {
        if(!hasHoveredBlock || hoveredBlock == null || hoveredBlock.getType() == BlockType.AIR)
            return;

        world.setBlock(hoveredBlockX, hoveredBlockY, hoveredBlockZ, new Block(BlockType.AIR, hoveredBlockX, hoveredBlockY, hoveredBlockZ));
    }

    private void framebufferSizeCallback(long window, int width, int height) {
        WIDTH = width;
        HEIGHT = height;
        glViewport(0, 0, WIDTH, HEIGHT);
        updateProjectionMatrix();
    }

    private void mouseCallback(long window, double xpos, double ypos) {
        if (firstMouse) {
            lastMouseX = xpos;
            lastMouseY = ypos;
            firstMouse = false;
        }

        float sensitivity = 0.1f;
        float xoffset = (float) (xpos - lastMouseX) * sensitivity;
        float yoffset = (float) (ypos - lastMouseY) * sensitivity;

        lastMouseX = xpos;
        lastMouseY = ypos;

        player.rotate(-yoffset, xoffset);
    }

    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        boolean isPressed = action != GLFW_RELEASE;
        switch (key) {
            case GLFW_KEY_E:
                moveForward = isPressed;
                break;
            case GLFW_KEY_S:
                moveBackward = isPressed;
                break;
            case GLFW_KEY_A:
                moveLeft = isPressed;
                break;
            case GLFW_KEY_D:
                moveRight = isPressed;
                break;
            case GLFW_KEY_SPACE:
                if (player.isSurvivalMode()) {
                    if (isPressed)
                        player.requestJump();
                }
                else
                    moveUp = isPressed;

                break;

            case GLFW_KEY_LEFT_SHIFT:
            case GLFW_KEY_RIGHT_SHIFT:
                moveDown = isPressed;
                System.out.println(player.getY());
                break;
            case GLFW_KEY_ESCAPE:
                if (isPressed)
                    glfwSetWindowShouldClose(window, true);
                break;
            case GLFW_KEY_P:
                if (isPressed)
                    player.toggleMode();
                break;
        }
    }

    private void loop() {
        double accumulator = 0.0;

        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            double frameTime = currentTime - lastTime;
            lastTime = currentTime;
            accumulator += frameTime;

            glfwPollEvents();

            while (accumulator >= TIME_PER_TICK) {
                updateGame((float) TIME_PER_TICK);
                accumulator -= TIME_PER_TICK;
            }

            float interpolation = (float) (accumulator / TIME_PER_TICK);
            world.getChunkManager().executeMainThreadTasks();
            render(interpolation);

            glfwSwapBuffers(window);
        }
    }

    private void updateProjectionMatrix() {
        projectionMatrix.identity().perspective((float) Math.toRadians(70.0f), (float) WIDTH / HEIGHT, 0.1f, 1000.0f);
    }

    private void updateGame(double deltaTime) {
        float acceleration = 5.0f;

        float accelX = 0f;
        float accelY = 0f;
        float accelZ = 0f;

        if (moveForward)
            accelZ += 1f;
        if (moveBackward)
            accelZ -= 1f;
        if (moveLeft)
            accelX += 1f;
        if (moveRight)
            accelX -= 1f;
        if (moveUp)
            accelY += player.isSurvivalMode() && player.isOnGround() ? 0f : 1f;
        if (moveDown)
            accelY -= player.isSurvivalMode() && player.isOnGround() ? 0f : 1f;

        float length = (float) Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
        if (length != 0) {
            accelX = (accelX / length) * acceleration;
            accelY = (accelY / length) * acceleration;
            accelZ = (accelZ / length) * acceleration;

            player.applyAcceleration(accelX, accelY, accelZ, (float) deltaTime);
        }

        player.update((float) deltaTime);

        int playerChunkX = (int) Math.floor(player.getX()) >> 4;
        int playerChunkZ = (int) Math.floor(player.getZ()) >> 4;
        world.update(playerChunkX, playerChunkZ);
    }

    private void render(float interpolation) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        shader.use();

        int projectionLocation = shader.getUniformLocation("projectionMatrix");
        int viewLocation = shader.getUniformLocation("viewMatrix");

        projectionMatrix.get(matrixBuffer);
        glUniformMatrix4fv(projectionLocation, false, matrixBuffer);

        viewMatrix.identity();
        player.getViewMatrix(viewMatrix, interpolation);
        viewMatrix.get(matrixBuffer);
        glUniformMatrix4fv(viewLocation, false, matrixBuffer);

        glUniform1i(shader.getUniformLocation("textureSampler"), 0);

        renderScene();
        drawCrosshair(WIDTH, HEIGHT);
        updateHoveredBlock();
        if (hasHoveredBlock)
            renderHoveredBlockOutline(hoveredBlockX, hoveredBlockY, hoveredBlockZ);
    }

    private void renderScene() {
        for (Chunk chunk : world.getChunks()) {
            if (!isChunkInView(chunk))
                continue;

            if (chunk.needsUpdate())
                chunk.updateMesh(world);

            chunk.render();
        }
    }

    private boolean isChunkInView(Chunk chunk) {
        float dx = chunk.getChunkX() * Chunk.CHUNK_SIZE_X + 8 - player.getX();
        float dz = chunk.getChunkZ() * Chunk.CHUNK_SIZE_Z + 8 - player.getZ();
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        return distance < RENDER_DISTANCE * Chunk.CHUNK_SIZE_X;
    }

    private void cleanup() throws SQLException {
        world.getChunks().forEach(Chunk::cleanUp);
        shader.cleanup();
        try{world.shutdown();}
        catch(Exception e){e.printStackTrace();}

        world.getChunkManager().getAllChunks().forEach(c -> store.saveChunk(c));
        store.saveWorldMeta(System.currentTimeMillis(), player.getX(), player.getY(), player.getZ());
        store.close();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();

        try(GLFWErrorCallback errorCallback = glfwSetErrorCallback(null)){
            if(errorCallback != null)
                errorCallback.free();
        }

        ChunkMeshGenerator.shutdown();
    }

    private void drawCrosshair(int screenWidth, int screenHeight) {
        glUseProgram(0);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, screenWidth, screenHeight, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glLineWidth(2.5f);
        glColor3f(0, 0, 0);

        glBegin(GL_LINES);
        glVertex2f(screenWidth / 2f - 5, screenHeight / 2f);
        glVertex2f(screenWidth / 2f + 5, screenHeight / 2f);
        glVertex2f(screenWidth / 2f, screenHeight / 2f - 5);
        glVertex2f(screenWidth / 2f, screenHeight / 2f + 5);
        glEnd();

        glEnable(GL_DEPTH_TEST);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        shader.use();
    }


    private void renderHoveredBlockOutline(int blockX, int blockY, int blockZ) {
        glUseProgram(0);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadMatrixf(projectionMatrix.get(matrixBuffer));

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadMatrixf(viewMatrix.get(matrixBuffer));

        // glEnable(GL_DEPTH_TEST);
         glDisable(GL_TEXTURE_2D);
      //  glDisable(GL_LIGHTING);
        glLineWidth(3);
        glColor3f(0, 0, 0);

        float x = blockX;
        float y = blockY;
        float z = blockZ;

        glBegin(GL_LINES);
        glVertex3f(x, y, z);
        glVertex3f(x + 1, y, z);
        glVertex3f(x + 1, y, z);
        glVertex3f(x + 1, y, z + 1);
        glVertex3f(x + 1, y, z + 1);
        glVertex3f(x, y, z + 1);
        glVertex3f(x, y, z + 1);
        glVertex3f(x, y, z);

        glVertex3f(x, y + 1, z);
        glVertex3f(x + 1, y + 1, z);
        glVertex3f(x + 1, y + 1, z);
        glVertex3f(x + 1, y + 1, z + 1);
        glVertex3f(x + 1, y + 1, z + 1);
        glVertex3f(x, y + 1, z + 1);
        glVertex3f(x, y + 1, z + 1);
        glVertex3f(x, y + 1, z);

        glVertex3f(x, y, z);
        glVertex3f(x, y + 1, z);
        glVertex3f(x + 1, y, z);
        glVertex3f(x + 1, y + 1, z);
        glVertex3f(x + 1, y, z + 1);
        glVertex3f(x + 1, y + 1, z + 1);
        glVertex3f(x, y, z + 1);
        glVertex3f(x, y + 1, z + 1);
        glEnd();

        glLineWidth(1);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();

        glMatrixMode(GL_MODELVIEW);
        shader.use();
    }


    private int hoveredBlockX = -1, hoveredBlockY = -1, hoveredBlockZ = -1;
    private boolean hasHoveredBlock = false;

    private Block hoveredBlock = null;

    private void updateHoveredBlock() {
        float maxDistance = 5.0f;
        float px = player.getX();
        float py = player.getY() + Player.EYE_HEIGHT;
        float pz = player.getZ();
        float pitch = (float) Math.toRadians(player.getPitch());
        float yaw   = (float) Math.toRadians(player.getYaw());
        float dx = (float)(Math.cos(pitch) * Math.sin(yaw));
        float dy = (float)(Math.sin(pitch));
        float dz = (float)(Math.cos(pitch) * -Math.cos(yaw));
        float step = 0.1f;
        float distance = 0;
        hasHoveredBlock = false;
        hoveredBlock = null;
        while (distance <= maxDistance) {
            float cx = px + dx * distance;
            float cy = py + dy * distance;
            float cz = pz + dz * distance;
            int bx = (int) Math.floor(cx);
            int by = (int) Math.floor(cy);
            int bz = (int) Math.floor(cz);
            Block block = world.getBlockAt(bx, by, bz);

            if (block != null && block.getType() != BlockType.AIR) {
                hoveredBlockX = bx;
                hoveredBlockY = by;
                hoveredBlockZ = bz;
                hasHoveredBlock = true;
                hoveredBlock = block;
                break;
            }

            distance += step;
        }
    }

}
