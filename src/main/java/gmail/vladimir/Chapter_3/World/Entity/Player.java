package gmail.vladimir.Chapter_3.World.Entity;

import gmail.vladimir.Chapter_3.World.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Player extends LivingEntity {

    private float prevX, prevY, prevZ;
    private boolean survivalMode = false;
    private boolean onGround = true;
    private boolean jumpRequested = false;

    public static float EYE_HEIGHT = 1.8f;

    public Player(float x, float y, float z, World world) {
        super(x, y, z, world);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
    }

    @Override
    public void update(float dt) {
        prevX = x;  prevY = y;  prevZ = z;

        if (survivalMode) {
            final float GRAVITY = 20.0f;
            velocityY -= GRAVITY * dt;
        }

        super.update(dt);
        onGround = false;
        if (survivalMode)
            CollisionDetector.resolveCollisions(this, world);

        if (jumpRequested && onGround) {
            velocityY     = JUMP_VELOCITY;
            jumpRequested = false;
            onGround      = false;
        }
    }

    public void setOnGround(boolean value) {
        this.onGround = value;
    }

    public boolean isOnGround(){
        return onGround;
    }

    public boolean isSurvivalMode() {
        return survivalMode;
    }

    public void requestJump() {
        jumpRequested = isOnGround();
    }

    public void toggleMode() {
        survivalMode = !survivalMode;
        if (survivalMode)
            System.out.println("Switched to Survival Mode (gravity & collision enabled)");

        else
            System.out.println("Switched to Spectator Mode (no gravity or collision)");
    }

    public void rotate(float pitchDelta, float yawDelta) {
        pitch += pitchDelta;
        yaw += yawDelta;

        if (pitch > 89.0f)
            pitch = 89.0f;
        if (pitch < -89.0f)
            pitch = -89.0f;

        yaw = yaw % 360.0f;
        if (yaw < 0)
            yaw += 360.0f;
    }

    public void getViewMatrix(Matrix4f viewMatrix, float interpolation) {
        float renderX = prevX + (x - prevX) * interpolation;
        float renderY = prevY + (y - prevY) * interpolation;
        float renderZ = prevZ + (z - prevZ) * interpolation;
        Vector3f position = new Vector3f(renderX, renderY + EYE_HEIGHT, renderZ);

        float radPitch = (float) Math.toRadians(pitch);
        float radYaw   = (float) Math.toRadians(yaw);
        Vector3f direction = new Vector3f((float)(Math.cos(radPitch) * Math.sin(radYaw)), (float)(Math.sin(radPitch)), (float)(Math.cos(radPitch) * -Math.cos(radYaw)));
        direction.normalize();
        Vector3f up = new Vector3f(0, 1, 0);

        viewMatrix.identity().lookAt(position, position.add(direction, new Vector3f()), up);
    }

}
