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

package io.spine.system.server;

import io.grpc.stub.StreamObserver;
import io.spine.base.EventMessage;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.core.Origin;
import io.spine.core.UserId;
import io.spine.grpc.LoggingObserver;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.grpc.LoggingObserver.Level.WARN;
import static io.spine.system.server.SystemEventFactory.forMessage;

/**
 * The default implementation of {@link SystemWriteSide}.
 */
final class DefaultSystemWriteSide implements SystemWriteSide {

    /**
     * The ID of the user which is used for generating system commands and events.
     */
    static final UserId SYSTEM_USER = UserId
            .newBuilder()
            .setValue("SYSTEM")
            .build();

    private final SystemContext system;

    DefaultSystemWriteSide(SystemContext system) {
        this.system = system;
    }

    @Override
    public void postEvent(EventMessage systemEvent, Origin origin) {
        checkNotNull(systemEvent);
        checkNotNull(origin);
        Event event = event(systemEvent, origin);
        StreamObserver<Ack> loggingObserver =
                LoggingObserver.<Ack>forClass(DefaultSystemWriteSide.class, WARN);
        system.eventBus()
              .post(event, loggingObserver);
    }

    private Event event(EventMessage message, Origin origin) {
        SystemEventFactory factory = forMessage(message, origin, system.isMultitenant());
        Event event = factory.createEvent(message, null);
        return event;
    }
}
