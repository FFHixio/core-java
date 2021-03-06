/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.projection;

import io.spine.base.EntityState;
import io.spine.server.delivery.event.CatchUpStarted;
import io.spine.server.entity.Repository;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeName;

/**
 * Dispatches an event to projections during the catch-up.
 *
 * <p>Handles the special {@link CatchUpStarted} event by deleting the state of the target
 * projection instance.
 */
final class CatchUpEndpoint<I, P extends Projection<I, S, ?>, S extends EntityState>
        extends ProjectionEndpoint<I, P, S> {

    private static final TypeName CATCH_UP_STARTED = TypeName.from(CatchUpStarted.getDescriptor());

    private CatchUpEndpoint(Repository<I, P> repository, EventEnvelope event) {
        super(repository, event);
    }

    static <I, P extends Projection<I, S, ?>, S extends EntityState>
    CatchUpEndpoint<I, P, S> of(ProjectionRepository<I, P, ?> repository, EventEnvelope event) {
        return new CatchUpEndpoint<>(repository, event);
    }

    /**
     * Does nothing, as no lifecycle events should be emitted during the catch-up.
     */
    @Override
    protected void afterDispatched(I entityId) {
        // do nothing.
    }

    @Override
    public void dispatchTo(I entityId) {
        TypeName actualTypeName = envelope().messageTypeName();
        if (CATCH_UP_STARTED.equals(actualTypeName)) {
            ProjectionRepository<I, P, ?> repository = repository();
            repository.recordStorage()
                      .delete(entityId);
        } else {
            super.dispatchTo(entityId);
        }
    }
}
