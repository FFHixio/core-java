/*
 * Copyright 2019, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.delivery;

import com.google.errorprone.annotations.Immutable;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;

/**
 * The strategy of splitting the entities into a number of shards uniformly.
 *
 * <p>Uses a hash code of the entity identifier and the remainder of its division by the total
 * number of shards to determine the index of a shard, at which the modification is allowed.
 */
@Immutable
public final class UniformAcrossAllShards implements DeliveryStrategy, Serializable {

    private static final long serialVersionUID = 0L;

    private final int numberOfShards;

    /**
     * Creates an instance of this strategy.
     *
     * @param numberOfShards
     *         a number of shards; must be greater than zero
     */
    private UniformAcrossAllShards(int numberOfShards) {
        checkArgument(numberOfShards > 0, "Number of shards must be positive");
        this.numberOfShards = numberOfShards;
    }

    /**
     * Creates a strategy of uniform target distribution across shards, for a given shard number.
     *
     * @param totalShards
     *         a number of shards
     * @return a uniform distribution strategy instance for a given shard number
     */
    public static DeliveryStrategy forNumber(int totalShards) {
        UniformAcrossAllShards result = new UniformAcrossAllShards(totalShards);
        return result;
    }

    @Override
    public ShardIndex indexFor(Object entityId) {
        if (1 == numberOfShards) {
            return newIndex(0);
        }
        int hashValue = entityId.hashCode();
        int totalShards = shardCount();
        int indexValue = abs(hashValue % totalShards);
        ShardIndex result = newIndex(indexValue);
        return result;
    }

    @Override
    public int shardCount() {
        return numberOfShards;
    }

    private ShardIndex newIndex(int indexValue) {
        return ShardIndex.newBuilder()
                         .setIndex(indexValue)
                         .setOfTotal(shardCount())
                         .build();
    }

    private enum SingleShard {
        INSTANCE;
        private final UniformAcrossAllShards strategy = new UniformAcrossAllShards(1);
    }

    /**
     * Returns a pre-defined strategy instance, which defines a single shard and puts all
     * the targets into it.
     *
     * @return a strategy that puts all entities in a single shard
     */
    @SuppressWarnings("WeakerAccess")   // a part of the public API.
    public static DeliveryStrategy singleShard() {
        return SingleShard.INSTANCE.strategy;
    }
}
