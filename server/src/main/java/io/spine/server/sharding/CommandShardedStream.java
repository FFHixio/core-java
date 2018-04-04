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
package io.spine.server.sharding;

import com.google.protobuf.Any;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.protobuf.AnyPacker;

/**
 * The stream of commands sent to a specific shard.
 *
 * @author Alex Tymchenko
 */
public class CommandShardedStream<I> extends ShardedStream<I, Command, CommandEnvelope> {

    private CommandShardedStream(Builder<I> builder) {
        super(builder);
    }

    public static <I> Builder<I> newBuilder() {
        return new Builder<>();
    }

    @Override
    protected ShardedMessageConverter<I, Command, CommandEnvelope> newConverter() {
        return new Converter<>();
    }

    /**
     * The converter of {@link CommandEnvelope} into {@link ShardedMessage} instances
     * and vice versa.
     *
     * @param <I> the identifier of the command targets.
     */
    private static class Converter<I> extends ShardedMessageConverter<I, Command, CommandEnvelope> {

        @Override
        protected CommandEnvelope toEnvelope(Any packedCommand) {
            final Command command = AnyPacker.unpack(packedCommand);
            final CommandEnvelope result = CommandEnvelope.of(command);
            return result;
        }
    }

    public static class Builder<I> extends AbstractBuilder<I, Builder<I>, CommandShardedStream<I>> {
        @Override
        protected CommandShardedStream<I> createStream() {
            return new CommandShardedStream<>(this);
        }
    }
}
