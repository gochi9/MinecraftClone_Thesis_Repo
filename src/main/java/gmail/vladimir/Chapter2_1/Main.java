package gmail.vladimir.Chapter2_1;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

public class Main {

    private long window; //stores the pointer to the GLFW window instance

    public void run() {
        init();
        loop();
        onExit();
    }

    //called once for the very first tick to set up GLFW, create the window, and initialize OpenGL context
    private void init() {
        //initialize GLFW's internal systems and native bindings
        if (!GLFW.glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        //set default window properties
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        int WIDTH = 800;
        int HEIGHT = 600;

        //create the actual window requesting a graphicscapable context from the OS
        window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, "Minecraft Clone", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        //get the primary monitor’s resolution and center the window on the screen
        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        GLFW.glfwSetWindowPos(window, (vidmode.width() - WIDTH) / 2, (vidmode.height() - HEIGHT) / 2);

        //make the OpenGL context current (required before using any OpenGL functions
        GLFW.glfwMakeContextCurrent(window);

        //enable v-sync
        GLFW.glfwSwapInterval(1);

        //window is ready to be presented
        GLFW.glfwShowWindow(window);
    }

    //this is the main game and render loop.
    //it is the beating heart of the game and runs once per frame until the user closes the window (or until it crashes_
    private void loop() {
        //initialize OpenGL bindings so that LWJGL can access GPU functions
        GL.createCapabilities();

        //set a background color, blue
        GL11.glClearColor(0.3f, 0.5f, 0.8f, 0.0f);

        //continue rendering frames until GLFW signals that the window should close
        while (!GLFW.glfwWindowShouldClose(window)) {
            //clear the color and depth buffers at the beginning of each frame
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            //swap the front and back buffers  to present the frame
            GLFW.glfwSwapBuffers(window);

            //poll for keyboard, mouse, and window events
            GLFW.glfwPollEvents();
        }
    }

    //called after the window is closed to release all native resources
    private void onExit() {
        //free any GLFW callbacks like input or resize listeners)
        Callbacks.glfwFreeCallbacks(window);

        //kill the window and its OpenGL context
        GLFW.glfwDestroyWindow(window);

        //terminate GLFW and clean up internal native resources
        GLFW.glfwTerminate();

        //free the error callback if one was set
        //in our case, this would throw a null pointer exception
        try(GLFWErrorCallback errCallBack = GLFW.glfwSetErrorCallback(null)){errCallBack.free();}
        catch(Throwable ignored){}
    }

    public static void main(String[] args) {
        new Main().run(); //start the game
    }
}