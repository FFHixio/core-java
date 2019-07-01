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

package io.spine.server.procman;

import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.PropagationOutcome;
import io.spine.server.type.ActorMessageEnvelope;

/**
 * Common base message for endpoints of Process Managers.
 *
 * @param <I> the type of process manager IDs
 * @param <P> the type of process managers
 * @param <M> the type of message envelopes processed by the endpoint
 */
abstract class PmEndpoint<I,
                          P extends ProcessManager<I, ?, ?>,
                          M extends ActorMessageEnvelope<?, ?, ?>>
        extends EntityMessageEndpoint<I, P, M> {

    PmEndpoint(ProcessManagerRepository<I, P, ?> repository, M envelope) {
        super(repository, envelope);
    }

    @Override
    protected boolean isModified(P processManager) {
        boolean result = processManager.changed();
        return result;
    }

    @Override
    protected void onModified(P processManager) {
        repository().store(processManager);
    }

    @Override
    protected ProcessManagerRepository<I, P, ?> repository() {
        return (ProcessManagerRepository<I, P, ?>) super.repository();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This method works differently to its analogues as it saves the entity
     *          state even if a rejection is thrown. It is done so because the process manager
     *          {@linkplain ProcessManagerRepository#lifecycle() lifecycle rules} may demand that
     *          entity becomes archived/deleted upon emitting certain rejection types.
     */
    @SuppressWarnings("UnnecessaryInheritDoc") // IDEA bug.
    @Override
    protected void dispatchInTx(I id) {
        P manager = repository().findOrCreate(id);
        tryDispatchAndSave(manager);
    }

    /**
     * Dispatches the message to a process manager and saves the entity state regardless of
     * successful delivery.
     */
    private void tryDispatchAndSave(P manager) {
        try {
            PropagationOutcome events = runTransactionFor(manager);
            store(manager);
            // TODO:2019-07-01:dmytro.dashenkov: Handle error case.
            repository().postEvents(events.getSuccess().getProducedEvents().getEventList());
        } catch (RuntimeException ex) {
            store(manager);
            throw ex;
        }
    }

    protected PropagationOutcome runTransactionFor(P processManager) {
        PmTransaction<?, ?, ?> tx = repository().beginTransactionFor(processManager);
        PropagationOutcome outcome = invokeDispatcher(processManager, envelope());
        tx.commit();
        return outcome;
    }
}
