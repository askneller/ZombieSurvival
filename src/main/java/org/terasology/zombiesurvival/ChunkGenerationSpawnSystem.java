/*
 * Copyright 2020 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.zombiesurvival;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.chunks.ChunkConstants;
import org.terasology.world.chunks.event.OnChunkGenerated;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

@Share(value = ChunkGenerationSpawnSystem.class)
@RegisterSystem(RegisterMode.AUTHORITY)
public class ChunkGenerationSpawnSystem extends BaseComponentSystem {

    private static final Logger logger = LoggerFactory.getLogger(ChunkGenerationSpawnSystem.class);

    @In
    private EntityManager entityManager;

    @In
    private PrefabManager prefabManager;

    @In
    private WorldProvider worldProvider;

    @In
    private BlockManager blockManager;

    private final Random random = new Random();

    private Block airBlock;

    private Collection<Prefab> spawnablePrefabs;

    /**
     * Check blocks at and around the target position and check if it's a valid spawning spot
     */
    private Function<Vector3i, Boolean> isValidSpawnPosition;


    /**
     * Readies the spawning system by defining blocks for identification and obtaining prefabs of animals.
     */
    @Override
    public void initialise() {
        airBlock = blockManager.getBlock(BlockManager.AIR_ID);

        if (isValidSpawnPosition == null) {
            isValidSpawnPosition = pos -> {
                org.joml.Vector3i below = new org.joml.Vector3i(pos.x, pos.y - 1, pos.z);
                Block blockBelow = worldProvider.getBlock(below);
                if (blockBelow.isPenetrable()) {
                    return false;
                }
                Block blockAtPosition = worldProvider.getBlock(pos);
                if (!blockAtPosition.isPenetrable()) {
                    return false;
                }

                org.joml.Vector3i above = new org.joml.Vector3i(pos.x, pos.y + 1, pos.z);
                Block blockAbove = worldProvider.getBlock(above);
                return blockAbove.equals(airBlock);
            };
        }

        // Find all spawnable prefabs
        spawnablePrefabs = prefabManager.listPrefabs(ChunkGenerationSpawnableComponent.class);
        logger.info("Found {} prefabs with ChunkGenerationSpawnableComponent", spawnablePrefabs.size());
    }

    /**
     * Runs upon a chunk being generated to see whether a deer should be spawned
     *
     * @param event       The event which the method will run upon receiving
     * @param worldEntity The world that the chunk is in
     */
    @ReceiveEvent
    public void onChunkGenerated(OnChunkGenerated event, EntityRef worldEntity) {
        PerformanceMonitor.startActivity("ChunkGenerationSpawnSystem");

        for (Prefab prefab : spawnablePrefabs) {
            ChunkGenerationSpawnableComponent spawnableComponent =
                    prefab.getComponent(ChunkGenerationSpawnableComponent.class);
            boolean trySpawn = spawnableComponent.probability > random.nextFloat() * 100;
            if (!trySpawn) {
                continue;
            }
            Vector3i chunkPos = event.getChunkPos();
            trySpawn(chunkPos, prefab, spawnableComponent);
        }
        PerformanceMonitor.endActivity();
    }

    /**
     * Attempts to spawn the prefab on the specified chunk. The number of entities spawned will depend on probabiliy
     * configurations defined earlier.
     *  @param chunkPos The chunk which the game will try to spawn entities on
     * @param prefab
     * @param spawnableComponent
     */
    private void trySpawn(Vector3i chunkPos, Prefab prefab, ChunkGenerationSpawnableComponent spawnableComponent) {
        List<Vector3i> foundPositions = findSpawnPositions(chunkPos);

        if (foundPositions.size() < spawnableComponent.minGroupSize * spawnableComponent.minGroundPerEntity) {
            return;
        }

        int maxCount = foundPositions.size() / spawnableComponent.minGroupSize;
        if (maxCount > spawnableComponent.maxGroupSize) {
            maxCount = spawnableComponent.maxGroupSize;
        }
        int count = spawnableComponent.maxGroupSize == 1 ? 1 :
                random.nextInt(maxCount - spawnableComponent.minGroupSize) + spawnableComponent.maxGroupSize;

        for (int i = 0; i < count; i++) {
            int randomIndex = random.nextInt(foundPositions.size());
            Vector3i randomSpawnPosition = foundPositions.remove(randomIndex);
            spawnEntity(randomSpawnPosition, prefab);
        }
    }

    /**
     * Checks each block of the chunk specified for valid spawning points.
     *
     * @param chunkPos The chunk that is being checked for valid spawnpoints
     * @return a list of positions of potential spawnpoints
     */
    private List<Vector3i> findSpawnPositions(Vector3i chunkPos) {
        Vector3i worldPos = new Vector3i(chunkPos);
        worldPos.mul(ChunkConstants.SIZE_X, ChunkConstants.SIZE_Y, ChunkConstants.SIZE_Z);
        List<Vector3i> foundPositions = Lists.newArrayList();
        Vector3i blockPos = new Vector3i();
        for (int y = ChunkConstants.SIZE_Y - 1; y >= 0; y--) {
            for (int z = 0; z < ChunkConstants.SIZE_Z; z++) {
                for (int x = 0; x < ChunkConstants.SIZE_X; x++) {
                    blockPos.set(x + worldPos.x, y + worldPos.y, z + worldPos.z);
                    if (isValidSpawnPosition.apply(blockPos)) {
                        foundPositions.add(new Vector3i(blockPos));
                    }
                }
            }
        }
        return foundPositions;
    }

    /**
     * Spawns the entity at the location specified by the parameter.
     *
     * @param location The location where the deer is to be spawned
     * @param prefab The prefab from which the entity will be created
     */
    private void spawnEntity(Vector3i location, Prefab prefab) {
        Vector3f floatVectorLocation = location.toVector3f();
        Vector3f yAxis = new Vector3f(0, 1, 0);
        float randomAngle = (float) (random.nextFloat() * Math.PI * 2);
        Quat4f rotation = new Quat4f(yAxis, randomAngle);
        entityManager.create(prefab, floatVectorLocation, rotation);
    }

}
