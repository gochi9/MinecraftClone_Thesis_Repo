package gmail.vladimir.Chapter2_2;

import static org.lwjgl.opengl.GL11.*;

public class Camera {
    private float x, y, z; //position
    private float pitch, yaw; //rotation

    public Camera(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = 0.0f;
        this.yaw = 0.0f; //changed from -90.0f to 0.0f
    }

    public void useView() {
        //apply camera rotation first (note negative signs to rotate world opposite to camera)
        glRotatef(-pitch, 1, 0, 0);
        glRotatef(-yaw, 0, 1, 0);
        //then apply translation to move the scene based on the camera's position
        glTranslatef(-x, -y, -z);
    }

    //instead of just relying on yaw directly, it uses forward and right vectors to separate directions
    public void move(float dx, float dy, float dz) {
        float radYaw = (float) Math.toRadians(yaw);

        //forward direction
        float forwardX = (float) Math.sin(radYaw);
        float forwardZ = (float) Math.cos(radYaw);

        //right direction
        float rightX = (float) Math.sin(radYaw - Math.PI / 2.0);
        float rightZ = (float) Math.cos(radYaw - Math.PI / 2.0);

        //combine movement based on both vectors
        x += dx * rightX + dz * forwardX;
        y += dy;
        z += dx * rightZ + dz * forwardZ;
    }

    public void rotate(float pitchDelta, float yawDelta) {
        pitch += pitchDelta;
        yaw += yawDelta;

        //clamp pitch to avoid flipping the camera upside down
        if (pitch > 89.0f)
            pitch = 89.0f;
        if (pitch < -89.0f)
            pitch = -89.0f;
    }

    /*
    //original move(float dx, float dy, float dz)
    //this version moved based only on yaw, which worked for forward/backward when looking in the right but gets inverted at certain angles
    public void move(float dx, float dy, float dz) {
        float radYaw = (float) Math.toRadians(yaw);

        x += dx * (float) Math.cos(radYaw) - dz * (float) Math.sin(radYaw);
        z += dx * (float) Math.sin(radYaw) + dz * (float) Math.cos(radYaw);
        y += dy;
    }
    */
}
