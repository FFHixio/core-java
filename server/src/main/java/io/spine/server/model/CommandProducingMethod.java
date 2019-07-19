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

package io.spine.server.model;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Immutable;
import io.spine.base.CommandMessage;
import io.spine.client.ActorRequestFactory;
import io.spine.client.CommandFactory;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.server.dispatch.ProducedCommands;
import io.spine.server.dispatch.Success;
import io.spine.server.type.CommandClass;
import io.spine.server.type.MessageEnvelope;
import io.spine.type.MessageClass;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link HandlerMethod} which produces commands in response to a signal.
 *
 * @param <T>
 *         the type of the target object
 * @param <C>
 *         the type of the incoming message class
 * @param <E>
 *         the type of the {@link MessageEnvelope} wrapping the method arguments
 */
@Immutable
public interface CommandProducingMethod<T,
                                        C extends MessageClass,
                                        E extends MessageEnvelope<?, ?, ?>>
        extends HandlerMethod<T, C, E, CommandClass> {

    /**
     * Creates success result from the empty result of the method execution.
     *
     * @param handledSignal
     *          the signal passed to the method
     * @throws IllegalOutcomeException
     *           if the method is not allowed to return empty result
     */
    Success fromEmpty(E handledSignal);

    @Override
    default Success toSuccessfulOutcome(@Nullable Object rawResult, T target, E handledSignal) {
        MethodResult result = MethodResult.from(rawResult);
        if (result.isEmpty()) {
            return fromEmpty(handledSignal);
        }
        ActorContext actorContext =
                handledSignal.asMessageOrigin()
                             .getActorContext();
        CommandFactory commandFactory = ActorRequestFactory
                .fromContext(actorContext)
                .command();
        ProducedCommands.Builder signals = ProducedCommands.newBuilder();
        ImmutableList<CommandMessage> messages = result.messages(CommandMessage.class);
        for (CommandMessage msg : messages) {
            Command command = commandFactory.create(msg);
            signals.addCommand(command);
        }
        return Success
                .newBuilder()
                .setProducedCommands(signals)
                .vBuild();
    }
}
