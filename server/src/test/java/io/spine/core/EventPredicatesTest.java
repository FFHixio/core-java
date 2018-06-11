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

package io.spine.core;

import com.google.common.base.Predicate;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Timestamp;
import io.spine.core.given.GivenEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.base.Time.getCurrentTime;
import static io.spine.test.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.TimeTests.Past.minutesAgo;
import static io.spine.test.TimeTests.Past.secondsAgo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"})
// JUnit 5 Nested classes cannot to be static.
@DisplayName("EventPredicates utility should")
class EventPredicatesTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(EventPredicates.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(Timestamp.class, getCurrentTime())
                .testAllPublicStaticMethods(EventPredicates.class);
    }

    @Nested
    @DisplayName("return false on null input")
    class ReturnFalseOnNull {

        @Test
        @DisplayName("in `IsAfter` predicate")
        void inIsAfter() {
            assertFalse(EventPredicates.isAfter(secondsAgo(5)).apply(null));
        }

        @Test
        @DisplayName("in `IsBefore` predicate")
        void inIsBefore() {
            assertFalse(EventPredicates.isBefore(secondsAgo(5)).apply(null));
        }

        @Test
        @DisplayName("in `IsBetween` predicate")
        void inIsBetween() {
            assertFalse(EventPredicates.isBetween(secondsAgo(5), secondsAgo(1)).apply(null));
        }
    }

    @Nested
    @DisplayName("create")
    class CreatePredicate {

        @Test
        @DisplayName("`IsAfter` predicate")
        void isAfter() {
            final Predicate<Event> predicate = EventPredicates.isAfter(minutesAgo(100));
            assertTrue(predicate.apply(GivenEvent.occurredMinutesAgo(20)));
            assertFalse(predicate.apply(GivenEvent.occurredMinutesAgo(360)));
        }

        @Test
        @DisplayName("`isBefore` predicate")
        void isBefore() {
            Predicate<Event> predicate = EventPredicates.isBefore(minutesAgo(100));
            assertFalse(predicate.apply(GivenEvent.occurredMinutesAgo(20)));
            assertTrue(predicate.apply(GivenEvent.occurredMinutesAgo(360)));
        }

        @Test
        @DisplayName("`isBetween` predicate")
        void isBetween() {
            final Event event = GivenEvent.occurredMinutesAgo(5);
            assertTrue(EventPredicates.isBetween(minutesAgo(10), minutesAgo(1)).apply(event));
            assertFalse(EventPredicates.isBetween(minutesAgo(2), minutesAgo(1)).apply(event));
        }
    }

    @Nested
    @DisplayName("for `inBetween` predicate, not accept range")
    class NotAcceptRange {

        @Test
        @DisplayName("with start after end")
        void withStartAfterEnd() {
            assertThrows(IllegalArgumentException.class,
                         () -> EventPredicates.isBetween(minutesAgo(2), minutesAgo(10)));
        }

        @Test
        @DisplayName("of zero length")
        void ofZeroLength() {
            Timestamp timestamp = minutesAgo(5);
            assertThrows(IllegalArgumentException.class,
                         () -> EventPredicates.isBetween(timestamp, timestamp));
        }
    }
}
