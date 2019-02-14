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

package io.spine.core;

import io.spine.base.EnrichmentContainer;
import io.spine.base.EnrichmentMessage;

import java.util.Optional;

/**
 * A common interface for message contexts that hold enrichments.
 */
public interface EnrichableMessageContext extends EnrichmentContainer, MessageContext {

    @SuppressWarnings("override") // in generated code.
    Enrichment getEnrichment();

    @Override
    default <E extends EnrichmentMessage> Optional<E> find(Class<E> cls) {
        Enrichment enrichment = getEnrichment();
        if (enrichment.getModeCase() != Enrichment.ModeCase.CONTAINER) {
            return Optional.empty();
        }
        Enrichment.Container container = enrichment.getContainer();
        Optional<E> result = Enrichments.getFromContainer(cls, container);
        return result;
    }
}