/*
 * Copyright 2020 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.zombiesurvival;

import org.terasology.entitySystem.Component;

/**
 * Component that enables an entity that can be spawned on chunk generation.
 */
public class ChunkGenerationSpawnableComponent implements Component {

    /**
     * Percentage probability that spawning will be triggered in this chunk at the surface
     */
    public float probability = 1.0f;

    /**
     * The minimum number of entities to spawn at one time
     */
    public byte minGroupSize = 1;

    /**
     * The maximum number of entities to spawn at one time
     */
    public byte maxGroupSize = 5;

    /**
     * The minimum number of valid spawn positions that must be available per entity to spawn. E.g. if the minGroupSize
     * is 2, then there must be >= 20 valid positions in the chunk
     */
    public byte minGroundPerEntity = 10;

    //TODO add darkness level and biome when map generation has reached better level
}
