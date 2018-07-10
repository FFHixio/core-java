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

package io.spine.server.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.integration.EmittedEventsVerifier.emitted;
import static io.spine.server.integration.given.EmittedEventsTestEnv.event;
import static io.spine.server.integration.given.EmittedEventsTestEnv.projectCreated;
import static io.spine.server.integration.given.EmittedEventsTestEnv.taskAdded;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mykhailo Drachuk
 */
@DisplayName("Emitted Events Verifier should")
class EmittedEventsVerifierTest {

    private EmittedEvents emittedEvents;

    @BeforeEach
    void setUp() {
        emittedEvents = new EmittedEvents(asList(
                event(projectCreated()),
                event(taskAdded()),
                event(taskAdded())
        ));
    }

    @Test
    @DisplayName("verify count")
    void count() {
        emitted(3).verify(emittedEvents);

        assertThrows(AssertionError.class, () -> verify(emitted(2)));
        assertThrows(AssertionError.class, () -> verify(emitted(4)));
    }

    @Test
    @DisplayName("verify contains classes")
    void containsClasses() {
        verify(emitted(IntProjectCreated.class, IntTaskAdded.class));

        assertThrows(AssertionError.class, () -> verify(emitted(IntProjectStarted.class)));
        assertThrows(AssertionError.class, () -> {
            verify(emitted(IntTaskAdded.class, IntProjectCreated.class, IntProjectStarted.class));
        });
    }

    @Test
    @DisplayName("verify contains classes represented by list")
    void verifyNumberOfEvents() {
        verify(emitted(0, IntProjectStarted.class));
        verify(emitted(1, IntProjectCreated.class));
        verify(emitted(2, IntTaskAdded.class));

        assertThrows(AssertionError.class,
                     () -> verify(emitted(1, IntProjectStarted.class)));
        assertThrows(AssertionError.class,
                     () -> verify(emitted(3, IntTaskAdded.class)));
    }

    private void verify(EmittedEventsVerifier verifier) {
        verifier.verify(emittedEvents);
    }
}
