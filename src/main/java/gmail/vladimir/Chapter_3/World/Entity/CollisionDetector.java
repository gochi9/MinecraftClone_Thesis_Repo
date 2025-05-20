package gmail.vladimir.Chapter_3.World.Entity;

import gmail.vladimir.Chapter_3.World.Block.Block;
import gmail.vladimir.Chapter_3.World.Block.BlockType;
import gmail.vladimir.Chapter_3.World.World;

public class CollisionDetector {

    public static void resolveCollisions(Player player, World world) {
        AABB playerBB = player.getBoundingBox();
        int minX = (int) Math.floor(playerBB.minX);
        int minY = (int) Math.floor(playerBB.minY);
        int minZ = (int) Math.floor(playerBB.minZ);
        int maxX = (int) Math.floor(playerBB.maxX);
        int maxY = (int) Math.floor(playerBB.maxY);
        int maxZ = (int) Math.floor(playerBB.maxZ);

        for (int x = minX; x <= maxX; x++)
        for (int y = minY; y <= maxY; y++)
        for (int z = minZ; z <= maxZ; z++) {
            Block block = world.getBlockAt(x, y, z);
            if (block == null || block.getType() == BlockType.AIR)
                continue;

            AABB blockBB = new AABB(x, y, z, x + 1, y + 1, z + 1);
            if (!playerBB.intersects(blockBB))
                continue;

            float overlapX = Math.min(playerBB.maxX, blockBB.maxX) - Math.max(playerBB.minX, blockBB.minX);
            float overlapY = Math.min(playerBB.maxY, blockBB.maxY) - Math.max(playerBB.minY, blockBB.minY);
            float overlapZ = Math.min(playerBB.maxZ, blockBB.maxZ) - Math.max(playerBB.minZ, blockBB.minZ);

            if (overlapX <= overlapY && overlapX <= overlapZ) {
                if (playerBB.minX < blockBB.minX)
                    player.x -= overlapX;
                else
                    player.x += overlapX;
            }
            else if (overlapY <= overlapX && overlapY <= overlapZ) {
                if (player.velocityY < 0) {
                    player.y += overlapY;
                    player.setOnGround(true);
                }
                else
                    player.y -= overlapY;

                player.velocityY = 0;
            }
            else {
                if (playerBB.minZ < blockBB.minZ)
                    player.z -= overlapZ;
                else
                    player.z += overlapZ;
            }
            playerBB = player.getBoundingBox();
        }
    }
}
