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

package io.spine.server.entity;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.server.command.DispatchCommand;

import java.util.List;

/**
 * A phase that dispatched a command to the entity in transaction.
 *
 * <p>The result of such dispatch is always a {@link List} of {@linkplain Event events} as
 * described in the {@code CommandHandlingEntity}
 * {@linkplain io.spine.server.command.CommandHandlingEntity#dispatchCommand(CommandEnvelope)
 * contract}.
 *
 * @param <I>
 *         the type of entity ID
 */
@Internal
public class CommandDispatchingPhase<I> extends Phase<I, List<Event>> {

    private final DispatchCommand<I> dispatch;

    public CommandDispatchingPhase(DispatchCommand<I> dispatch,
                                   VersionIncrement versionIncrement) {
        super(versionIncrement);
        this.dispatch = dispatch;
    }

    @Override
    protected List<Event> performDispatch() {
        List<Event> events = dispatch.perform();
        return events;
    }

    @Override
    public I getEntityId() {
        return dispatch.entity()
                       .getId();
    }

    @Override
    public Message getMessageId() {
        return dispatch.command()
                       .getId();
    }
}