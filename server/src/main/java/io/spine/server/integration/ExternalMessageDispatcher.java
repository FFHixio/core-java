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
package io.spine.server.integration;

import io.spine.server.bus.MulticastDispatcher;

/**
 * Delivers messages produced outside of the current {@code BoundedContext} to dispatchers
 * that are {@linkplain io.spine.core.Subscribe#external() configured} to receive external events.
 *
 * @apiNote An {@code ExternalMessageDispatcher} does not dispatch any messages. It merely tells
 *         which message types are required by the associated event dispatcher.
 */
public interface ExternalMessageDispatcher
        extends MulticastDispatcher<ExternalMessageClass, ExternalMessageEnvelope> {

    @Override
    default void dispatch(ExternalMessageEnvelope envelope) {
        throw new UnsupportedOperationException("External messages cannot be dispatched directly.");
    }
}
