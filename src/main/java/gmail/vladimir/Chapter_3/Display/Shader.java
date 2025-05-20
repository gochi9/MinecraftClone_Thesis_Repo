package gmail.vladimir.Chapter_3.Display;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.lwjgl.opengl.GL20.*;

public class Shader {

    private final int programId;

    public Shader(String vertexShaderFile, String fragmentShaderFile) throws IOException {
        int vertexShaderId = createShader(loadResource(vertexShaderFile), GL_VERTEX_SHADER);
        int fragmentShaderId = createShader(loadResource(fragmentShaderFile), GL_FRAGMENT_SHADER);

        programId = glCreateProgram();
        if (programId == 0)
            throw new RuntimeException("Failed to create shader program");

        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);

        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Program linking failed: " + glGetProgramInfoLog(programId));

        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE)
            throw new RuntimeException("Program validation failed: " + glGetProgramInfoLog(programId));

        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }

    private String loadResource(String fileName) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/shaders/" + fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");

            return sb.toString();
        }
    }

    private int createShader(String source, int type) {
        int shaderId = glCreateShader(type);
        if (shaderId == 0)
            throw new RuntimeException("Error creating shader of type " + type);

        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String err = glGetShaderInfoLog(shaderId);
            throw new RuntimeException("Failed to compile shader: " + err);
        }

        return shaderId;
    }

    public void use() {
        glUseProgram(programId);
    }

    public int getUniformLocation(String name) {
        return glGetUniformLocation(programId, name);
    }

    public void cleanup() {
        glUseProgram(0);
        if (programId != 0)
            glDeleteProgram(programId);
    }
}
