/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.asList;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A verifier working with the events emitted in the {@link BlackBoxBoundedContext Bounded Context}.
 *
 * <p> Contains static factory methods for creating emitted events verifiers, checking that specific
 * amounts of some event types were emitted by entities inside of the Bounded Context.
 *
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
public abstract class EmittedEventsVerifier {

    abstract void verify(EmittedEvents events);

    /**
     * Verifies that there was an expected amount of events of any type emitted 
     * in the Bounded Context.
     *
     * @return new {@link EmittedEventsVerifier emitted events verifier} instance
     */
    public static EmittedEventsVerifier emitted(int expectedCount) {
        checkArgument(expectedCount >= 0, "0 or more emitted events must be expected.");

        return new EmittedEventsVerifier() {

            @Override
            public void verify(EmittedEvents events) {
                int actualCount = events.count();
                String moreOrLess = compare(actualCount, expectedCount);
                assertEquals(expectedCount, actualCount,
                             "Bounded Context emitted " + moreOrLess + " events than expected");
            }
        };
    }

    /**
     * Verifies that there were events of each of the provided event types emitted
     * in the Bounded Context.
     *
     * @return new {@link EmittedEventsVerifier emitted events verifier} instance
     */
    @SafeVarargs
    public static EmittedEventsVerifier
    emitted(Class<? extends Message> firstEventType, Class<? extends Message>... otherEventTypes) {
        return emitted(asList(firstEventType, otherEventTypes));
    }

    /**
     * Verifies that there were events of each of the provided event types emitted
     * in the Bounded Context.
     *
     * @param eventTypes a list of class of a domain event message
     * @return new {@link EmittedEventsVerifier emitted events verifier} instance
     */

    private static EmittedEventsVerifier emitted(List<Class<? extends Message>> eventTypes) {
        checkArgument(eventTypes.size() > 0,
                      "At least one event must be provided to emitted events verifier in a list.");
        return new EmittedEventsVerifier() {

            @Override
            public void verify(EmittedEvents events) {
                for (Class<? extends Message> eventType : eventTypes) {
                    String eventName = eventType.getName();
                    assertTrue(events.contain(eventType),
                               format("Bounded Context did not emit %s event", eventName));
                }
            }
        };
    }

    /**
     * Verifies that there wes a specific number of events of the provided event type emitted
     * in the Bounded Context.
     *
     * @param expectedCount an amount of events of a provided event type that should
     *                      be emitted in the Bounded Context
     * @param eventType     a class of a domain event message
     * @return new {@link EmittedEventsVerifier emitted events verifier} instance
     */
    public static EmittedEventsVerifier
    emitted(int expectedCount, Class<? extends Message> eventType) {
        checkArgument(expectedCount >= 0);
        return new EmittedEventsVerifier() {

            @Override
            public void verify(EmittedEvents events) {
                String eventName = eventType.getName();
                int actualCount = events.count(eventType);
                String moreOrLess = compare(actualCount, expectedCount);
                assertEquals(expectedCount, actualCount,
                             format("Bounded Context emitted %s %s events than expected",
                                    moreOrLess, eventName));
            }
        };
    }

    /**
     * Compares two integers returning a string stating if the first value is less, more or
     * same number as the second.
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static String compare(int firstValue, int secondValue) {
        if (firstValue > secondValue) {
            return "more";
        }
        if (firstValue < secondValue) {
            return "less";
        }
        return "same number";
    }
}
