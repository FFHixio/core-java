/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.StringValue;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.command.TestEventFactory;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.integration.ExternalMessages;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.test.TestValues.newUuidValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("DelegatingEventDispatcher should")
class DelegatingEventDispatcherTest {

    private EmptyEventDispatcherDelegate delegate;
    private DelegatingEventDispatcher<String> delegatingDispatcher;

    @BeforeEach
    void setUp() {
        delegate = new EmptyEventDispatcherDelegate();
        delegatingDispatcher = DelegatingEventDispatcher.of(delegate);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(EventDispatcherDelegate.class, new EmptyEventDispatcherDelegate())
                .testAllPublicStaticMethods(DelegatingEventDispatcher.class);
    }

    @Test
    @DisplayName("delegate `onError`")
    void delegateOnError() {
        final TestEventFactory factory = TestEventFactory.newInstance(getClass());
        final EventEnvelope envelope = EventEnvelope.of(factory.createEvent(newUuidValue()));

        final RuntimeException exception = new RuntimeException("test delegating onError");
        delegatingDispatcher.onError(envelope, exception);

        assertTrue(delegate.onErrorCalled());
        assertEquals(exception, delegate.getLastException());
    }

    @Test
    @DisplayName("expose external dispatcher that delegates `onError`")
    void exposeDispatcherDelegatingOnError() {
        final ExternalMessageDispatcher<String> extMessageDispatcher =
                delegatingDispatcher.getExternalDispatcher();

        final TestEventFactory factory = TestEventFactory.newInstance(getClass());
        final StringValue eventMsg = newUuidValue();
        final Event event = factory.createEvent(eventMsg);
        final ExternalMessage externalMessage =
                ExternalMessages.of(event, BoundedContext.newName(getClass().getName()));

        final ExternalMessageEnvelope externalMessageEnvelope =
                ExternalMessageEnvelope.of(externalMessage, eventMsg);

        final RuntimeException exception =
                new RuntimeException("test external dispatcher delegating onError");
        extMessageDispatcher.onError(externalMessageEnvelope,exception);

        assertTrue(delegate.onErrorCalled());
    }

    /*
     * Test environment
     ********************/

    private static final class EmptyEventDispatcherDelegate
            implements EventDispatcherDelegate<String> {

        private boolean onErrorCalled;

        private @Nullable RuntimeException lastException;

        @Override
        public Set<EventClass> getEventClasses() {
            return ImmutableSet.of();
        }

        @Override
        public Set<EventClass> getExternalEventClasses() {
            return ImmutableSet.of();
        }

        @Override
        public Set<String> dispatchEvent(EventEnvelope envelope) {
            // Do nothing.
            return ImmutableSet.of();
        }

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            onErrorCalled = true;
            lastException = exception;
        }

        private boolean onErrorCalled() {
            return onErrorCalled;
        }

        private @Nullable RuntimeException getLastException() {
            return lastException;
        }
    }
}
