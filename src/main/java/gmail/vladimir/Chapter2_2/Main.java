package gmail.vladimir.Chapter2_2;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private static int WIDTH = 800;
    private static int HEIGHT = 600;
    private static float ASPECT_RATIO = (float) WIDTH / (float) HEIGHT;

    private long window;

    private Camera camera;

    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;

    public void run() {
        init();
        loop();
        onExit();
    }

    private void init() {
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "Minecraft Clone", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window, (vidmode.width() - WIDTH) / 2, (vidmode.height() - HEIGHT) / 2);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        //moved from loop as they only need to get called once
        GL.createCapabilities();
        glClearColor(0.3f, 0.5f, 0.8f, 0.0f);

        //depth testing allows OpenGL to determine which objects are in front of others when multiple things are being drawn in 3D space.
        //without it, OpenGL will just draw everything in the order you send it, regardless of whether it’s supposed to be behind something else.
        glEnable(GL_DEPTH_TEST);

        //creating the camera a bit above the platform
        camera = new Camera(0, 1, 6);

        //update OpenGL viewport when window is resized
        glfwSetFramebufferSizeCallback(window, (window, width, height) -> {WIDTH = width;
            HEIGHT = height;
            glViewport(0, 0, WIDTH, HEIGHT);
            ASPECT_RATIO = (float) WIDTH / (float) HEIGHT;
        });

        //lock and hide the cursor
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        //raw mouse movement used to rotate the camera
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            if (firstMouse) {
                //skip the first frame to avoid a large jump in view direction
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }

            float sensitivity = 0.1f;
            //calculate how far the mouse moved this frame
            float xoffset = (float) (xpos - lastMouseX) * sensitivity;
            float yoffset = (float) (lastMouseY - ypos) * sensitivity;

            lastMouseX = xpos;
            lastMouseY = ypos;

            //without inverting xoffset, horizontal movement is reversed, this fixes that
            camera.rotate(yoffset, -xoffset);
        });

        //kill
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS)
                glfwSetWindowShouldClose(window, true);
        });
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            //clear screen and depth buffer at the start of each frame
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            processInput();

            //set up the perspective projection matrix
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            gluPerspective(45.0f, ASPECT_RATIO, 0.1f, 100.0f);

            //prepare model-view transformations
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();

            camera.useView();

            //draw a simple flat platform using immediate mode
            glBegin(GL_QUADS);
            glColor3f(0.6f, 0.8f, 0.2f);
            glVertex3f(-2.0f, 0.0f, -2.0f);
            glVertex3f(-2.0f, 0.0f, 2.0f);
            glVertex3f(2.0f, 0.0f, 2.0f);
            glVertex3f(2.0f, 0.0f, -2.0f);
            glEnd();

            //display the rendered frame
            glfwSwapBuffers(window);

            //handle inputs
            glfwPollEvents();
        }
    }

    private void onExit() {
        Callbacks.glfwFreeCallbacks(window);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        try(GLFWErrorCallback errCallBack = GLFW.glfwSetErrorCallback(null)){errCallBack.free();}
        catch(Throwable ignored){}
    }

    private void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
        float fH = (float) Math.tan(Math.toRadians(fovY / 2)) * zNear;
        float fW = fH * aspect;
        glFrustum(-fW, fW, -fH, fH, zNear, zFar);
    }

    //movement is accumulated as a directional vector, normalized, and scaled for consistent speed
    private void processInput() {
        float cameraSpeed = 0.05f;

        float dx = 0f;
        float dy = 0f;
        float dz = 0f;

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
            dz -= 1f;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
            dz += 1f;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
            dx -= 1f;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
            dx += 1f;
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS)
            dy += 1f;
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS)
            dy -= 1f;

        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0)
            return;

        dx = (dx / length) * cameraSpeed;
        dy = (dy / length) * cameraSpeed;
        dz = (dz / length) * cameraSpeed;

        camera.move(dx, dy, dz);
    }

    public static void main(String[] args) {
        new Main().run();
    }
}
