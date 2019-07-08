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

package io.spine.server.event;

import com.google.common.base.MoreObjects;
import io.spine.annotation.Internal;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link EventDispatcher} which delegates the responsibilities to an aggregated {@link
 * EventDispatcherDelegate delegate instance}.
 *
 * @see EventDispatcherDelegate
 */
@Internal
public final class DelegatingEventDispatcher<I> implements EventDispatcher<I> {

    /**
     * A target delegate.
     */
    private final EventDispatcherDelegate delegate;

    /**
     * Creates a new instance of {@code DelegatingCommandDispatcher}, proxying the calls
     * to the passed {@code delegate}.
     *
     * @param delegate a delegate to pass the dispatching duties to
     * @return new instance
     */
    public static <I> DelegatingEventDispatcher<I> of(EventDispatcherDelegate delegate) {
        checkNotNull(delegate);
        return new DelegatingEventDispatcher<>(delegate);
    }

    private DelegatingEventDispatcher(EventDispatcherDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<EventClass> messageClasses() {
        return delegate.domesticEvents();
    }

    @Override
    public Set<EventClass> externalEventClasses() {
        return delegate.externalEvents();
    }

    @Override
    public void dispatch(EventEnvelope event) {
        delegate.dispatchEvent(event);
    }

    /**
     * Wraps this dispatcher to an external event dispatcher.
     *
     * @return the external rejection dispatcher proxying calls to the underlying instance
     */
    @Override
    public Optional<ExternalMessageDispatcher<I>> createExternalDispatcher() {
        return dispatchesExternalEvents()
               ? Optional.of(new ExternalDispatcher<>(delegate))
               : Optional.empty();
    }

    /**
     * Returns the string representation of this dispatcher.
     *
     * <p>Includes an FQN of the {@code delegate} in order to allow distinguish
     * {@code DelegatingEventDispatcher} instances with different delegates.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("eventDelegate", delegate.getClass())
                          .toString();
    }

    /**
     * An implementation of {@link ExternalMessageDispatcher} which delegates all its calls to
     * a given {@link EventDispatcherDelegate}.
     *
     * @param <I> the type of the dispatcher ID
     */
    private static final class ExternalDispatcher<I>
            implements ExternalMessageDispatcher<I> {

        private final EventDispatcherDelegate delegate;

        private ExternalDispatcher(EventDispatcherDelegate delegate) {
            this.delegate = delegate;
        }

        @Override
        public Set<ExternalMessageClass> messageClasses() {
            Set<EventClass> eventClasses = delegate.externalEvents();
            return ExternalMessageClass.fromEventClasses(eventClasses);
        }

        @Override
        public void dispatch(ExternalMessageEnvelope envelope) {
            EventEnvelope eventEnvelope = envelope.toEventEnvelope();
            delegate.dispatchEvent(eventEnvelope);
        }
    }
}
