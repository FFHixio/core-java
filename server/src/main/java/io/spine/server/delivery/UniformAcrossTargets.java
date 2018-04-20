/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.abs;

/**
 * The strategy of splitting the entities into a number of shards uniformly.
 *
 * <p>Uses a hash code of the entity identifier and the remainder of its division by the total
 * number of shards to determine a shard index. E.g.
 *
 * <pre>
 *     {@code
 *
 *     final UserAggregate entity = ...
 *
 *     final int hashValue =  entity.getId().hashCode();
 *     final int numberOfShards = 4;
 *
 *      // possible values are 0, 1, 2, and 3.
 *     final int shardIndexValue = Math.abs(hashValue % numberOfShards);
 *     final ShardIndex shardIndex = newIndex(shardIndexValue);
 *     }
 * </pre>
 *
 *  <p>Such an approach isn't truly uniform — as long as the ID nature may be very specific,
 *  making the {@code hashCode()} value distribution uneven. However, it's a good enough choice
 *  in a general case
 *
 * @author Alex Tymchenko
 */
public class UniformAcrossTargets implements ShardingStrategy {

    private static final long serialVersionUID = 0L;

    private final int numberOfShards;

    /**
     * Creates an instance of this strategy.
     *
     * @param numberOfShards a number of shards; must be greater than zero
     */
    private UniformAcrossTargets(int numberOfShards) {
        checkArgument(numberOfShards > 0, "Number of shards must be positive");
        this.numberOfShards = numberOfShards;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfShards() {
        return numberOfShards;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ShardIndex indexForTarget(Object targetId) {
        final int hashValue = targetId.hashCode();
        final int totalShards = getNumberOfShards();
        final int indexValue = abs(hashValue % totalShards);
        final ShardIndex result = newIndex(indexValue, totalShards);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ShardIndex> allIndexes() {
        final ImmutableSet.Builder<ShardIndex> resultBuilder = ImmutableSet.builder();
        final int totalShards = getNumberOfShards();
        for (int indexValue = 0; indexValue < totalShards; indexValue++) {
            final ShardIndex shardIndex = newIndex(indexValue, totalShards);
            resultBuilder.add(shardIndex);
        }
        return resultBuilder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UniformAcrossTargets that = (UniformAcrossTargets) o;
        return numberOfShards == that.numberOfShards;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfShards);
    }

    private enum SingleShard {
        INSTANCE;
        private final UniformAcrossTargets strategy = new UniformAcrossTargets(1);
    }

    /**
     * Returns a pre-defined strategy instance, which defines a single shard and puts all
     * the targets into it.
     *
     * @return a strategy that puts all entities in a single shard
     */
    public static ShardingStrategy singleShard() {
        return SingleShard.INSTANCE.strategy;
    }

    /**
     * Creates a strategy of uniform target distribution across shards, for a given shard number.
     *
     * @param totalShards a number of shards
     * @return a uniform distribution strategy instance for a given shard number
     */
    public static ShardingStrategy forNumber(int totalShards) {
        final UniformAcrossTargets result = new UniformAcrossTargets(totalShards);
        return result;
    }

    private static ShardIndex newIndex(int indexValue, int totalShards) {
        return ShardIndex.newBuilder()
                         .setIndex(indexValue)
                         .setOfTotal(totalShards)
                         .build();
    }
}
