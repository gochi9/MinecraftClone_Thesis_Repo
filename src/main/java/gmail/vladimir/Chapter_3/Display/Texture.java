package gmail.vladimir.Chapter_3.Display;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

public class Texture {

    private static final Map<String, Texture> CACHE = new ConcurrentHashMap<>();
    private static final String TEXTURE_PATH = "textures/";

    private static final int DEFAULT_WRAP_S = GL_REPEAT;
    private static final int DEFAULT_WRAP_T = GL_REPEAT;
    private static final int DEFAULT_MIN_FILTER = GL_NEAREST_MIPMAP_NEAREST;
    private static final int DEFAULT_MAG_FILTER = GL_NEAREST;

    private final int id;

    public Texture(String fileName) {
        this.id = loadTexture(fileName);
    }

    public static Texture getTexture(String fileName) {
        return CACHE.computeIfAbsent(fileName, Texture::new);
    }

    private int loadTexture(String fileName) {
        int width, height;
        ByteBuffer imageData;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            IntBuffer channelsBuffer = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(true);
            imageData = STBImage.stbi_load(TEXTURE_PATH + fileName, widthBuffer, heightBuffer, channelsBuffer, 4);

            if (imageData == null)
                throw new RuntimeException("Failed to load texture: " + fileName + " - " + STBImage.stbi_failure_reason());

            width = widthBuffer.get();
            height = heightBuffer.get();
        }

        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, DEFAULT_WRAP_S);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, DEFAULT_WRAP_T);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, DEFAULT_MIN_FILTER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, DEFAULT_MAG_FILTER);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        glGenerateMipmap(GL_TEXTURE_2D);

        STBImage.stbi_image_free(imageData);
        return texId;
    }

    public void bind() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void cleanup() {
        glDeleteTextures(id);
    }

    public int getId() {
        return id;
    }

}