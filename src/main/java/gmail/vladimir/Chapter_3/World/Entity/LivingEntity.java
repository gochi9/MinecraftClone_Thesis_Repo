package gmail.vladimir.Chapter_3.World.Entity;

import gmail.vladimir.Chapter_3.World.World;
import org.joml.Vector3f;

public abstract class LivingEntity extends Entity {

    protected float pitch, yaw;
    protected float velocityX, velocityY, velocityZ;
    protected float friction = 4.0f;
    protected float flightSpeed = 40f;

    protected float WIDTH = 0.6f;
    protected float HEIGHT = 1.8f;
    protected final float DEPTH = 0.6f;
    protected final float JUMP_VELOCITY = 10.0f;

    public LivingEntity(float x, float y, float z, World world) {
        super(x, y, z, world);
    }

    public float getPitch(){
        return pitch;
    }

    public float getYaw(){
        return yaw;
    }

    @Override
    public void update(float deltaTime) {
        float oldX = x, oldZ = z;
        float newX = x + velocityX * deltaTime;
        float newY = y + velocityY * deltaTime;
        float newZ = z + velocityZ * deltaTime;

        if (world.canMoveTo(newX, newY, newZ)) {
            x = newX;
            y = newY;
            z = newZ;
        }
        else {
            velocityX = 0;
            velocityZ = 0;
            x = oldX;
            z = oldZ;
        }

        applyFriction(deltaTime);
    }

    protected void applyFriction(float deltaTime) {
        float factor = 1 - friction * deltaTime;
        velocityX *= (factor);
        velocityZ *= (factor);

        if (!(this instanceof Player p) || !p.isSurvivalMode())
            velocityY *= factor;
    }

    public void applyAcceleration(float ax, float ay, float az, float dt) {
        boolean survival = this instanceof Player p && p.isSurvivalMode();

        float sin = (float) Math.sin(Math.toRadians(yaw));
        float cos = (float) Math.cos(Math.toRadians(yaw));
        float cosPitch = (float) Math.cos(pitch);
        float sinPitch = (float) Math.sin(pitch);
        Vector3f forward = survival ? new Vector3f( sin, 0, -cos) : new Vector3f( cosPitch*sin, sinPitch, -cosPitch*cos);

        Vector3f right = new Vector3f( cos, 0,  sin);
        Vector3f up = new Vector3f( 0, 1,   0);

        if (survival)
            ay = 0;

        Vector3f acc = new Vector3f().fma(az, forward).fma(-ax, right).fma(ay, up);

        if (acc.lengthSquared() == 0)
            return;

        acc.normalize().mul(flightSpeed * dt);
        velocityX += acc.x;
        velocityY += acc.y;
        velocityZ += acc.z;
    }

    public AABB getBoundingBox() {
        float halfWidth = WIDTH / 2.0f;
        return new AABB(x - halfWidth, y, z - halfWidth, x + halfWidth, y + HEIGHT, z + halfWidth);
    }

}
