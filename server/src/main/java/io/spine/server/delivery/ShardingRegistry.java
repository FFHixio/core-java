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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.spine.core.MessageEnvelope;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import static com.google.common.collect.Multimaps.synchronizedMultimap;

/**
 * The registry of sharded message streams.
 *
 * @author Alex Tymchenko
 */
final class ShardingRegistry {

    private final Multimap<DeliveryTag, Entry> entries =
            synchronizedMultimap(HashMultimap.<DeliveryTag, Entry>create());

    void register(ShardingStrategy strategy, Set<ShardedStream<?, ?, ?>> streams) {
        for (ShardedStream<?, ?, ?> stream : streams) {
            final Entry entry = new Entry(strategy, stream);
            final DeliveryTag<?> tag = stream.getTag();
            entries.put(tag, entry);
        }
    }

    void unregister(ShardedStreamConsumer streamConsumer) {
        final DeliveryTag tag = streamConsumer.getTag();
        final Collection<Entry> entriesForTag = entries.get(tag);
        for (Entry entry : entriesForTag) {
            entry.stream.close();
        }
        entries.removeAll(tag);
    }

    <I, E extends MessageEnvelope<?, ?, ?>> Set<ShardedStream<I, ?, E>>
    find(final DeliveryTag<E> tag, final I targetId) {

        final Collection<Entry> entriesForTag = entries.get(tag);

        final ImmutableSet.Builder<ShardedStream<I, ?, E>> builder = ImmutableSet.builder();
        for (Entry entry : entriesForTag) {

            final ShardIndex shardIndex = entry.strategy.indexForTarget(targetId);
            if(shardIndex.equals(entry.stream.getKey().getIndex())) {
                builder.add(((ShardedStream<I, ?, E>)entry.stream));
            }
        }

        return builder.build();
    }

    private static class Entry {
        private final ShardingStrategy strategy;
        private final ShardedStream<?, ?, ?> stream;

        private Entry(ShardingStrategy strategy, ShardedStream<?, ?, ?> stream) {
            this.strategy = strategy;
            this.stream = stream;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Entry entry = (Entry) o;
            return Objects.equals(strategy, entry.strategy) &&
                    Objects.equals(stream, entry.stream);
        }

        @Override
        public int hashCode() {
            return Objects.hash(strategy, stream);
        }
    }
}
