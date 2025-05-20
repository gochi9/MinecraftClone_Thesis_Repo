package gmail.vladimir.Chapter_3.World.Entity;

import gmail.vladimir.Chapter_3.World.World;

public abstract class Entity {

    protected float x, y, z;
    protected World world;
    public Entity(float x, float y, float z, World world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getZ(){
        return z;
    }

    public abstract void update(float deltaTime);

    public void teleport(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

}
