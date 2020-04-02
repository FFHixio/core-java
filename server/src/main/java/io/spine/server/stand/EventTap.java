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

package io.spine.server.stand;

import io.spine.server.bus.Listener;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An Event {@link Listener} which propagates events into to subscribers of
 * {@link io.spine.server.SubscriptionService}.
 */
final class EventTap implements Listener<EventEnvelope> {

    /**
     * Manages the subscriptions for this instance of {@code Stand}.
     */
    private final SubscriptionRegistry subscriptionRegistry;

    EventTap(SubscriptionRegistry subscriptionRegistry) {
        this.subscriptionRegistry = checkNotNull(subscriptionRegistry);
    }

    @Override
    public void accept(EventEnvelope event) {
        TypeUrl typeUrl = event.typeUrl();
        if (subscriptionRegistry.hasType(typeUrl)) {
            subscriptionRegistry.byType(typeUrl)
                                .stream()
                                .filter(SubscriptionRecord::isActive)
                                .forEach(record -> record.update(event));
        }
    }
}
