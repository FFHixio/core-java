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

package io.spine.server.aggregate;

import com.google.protobuf.Timestamp;
import io.spine.core.Rejection;
import io.spine.server.aggregate.given.AggregateCommandTestShouldEnv.CommandHandlingAggregate;
import io.spine.server.aggregate.given.AggregateCommandTestShouldEnv.RejectionCommandHandlerTest;
import io.spine.server.aggregate.given.AggregateCommandTestShouldEnv.CommandHandlingTest;
import io.spine.server.expected.CommandHandlerExpected;
import io.spine.testutil.server.aggregate.TestUtilProjectAggregate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.server.aggregate.given.AggregateCommandTestShouldEnv.CommandHandlingTest.TEST_COMMAND;
import static io.spine.server.aggregate.given.AggregateCommandTestShouldEnv.aggregate;
import static io.spine.validate.Validate.isNotDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vladyslav Lubenskyi
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@DisplayName("AggregateCommandTest should")
class AggregateCommandTestShould {

    private CommandHandlingTest aggregateCommandTest;
    private RejectionCommandHandlerTest aggregateRejectionCommandTest;

    @BeforeEach
    void setUp() {
        aggregateCommandTest = new CommandHandlingTest();
        aggregateRejectionCommandTest = new RejectionCommandHandlerTest();
    }

    @Test
    @DisplayName("store tested command")
    void shouldStoreCommand() {
        aggregateCommandTest.setUp();
        assertEquals(aggregateCommandTest.storedMessage(), TEST_COMMAND);
    }

    @Test
    @DisplayName("dispatch tested command")
    void shouldDispatchCommand() {
        aggregateCommandTest.setUp();
        CommandHandlingAggregate testAggregate = aggregate();
        aggregateCommandTest.expectThat(testAggregate);
        Timestamp newState = testAggregate.getState()
                                          .getTimestamp();
        assertTrue(isNotDefault(newState));
    }

    @Test
    @DisplayName("not fail when rejected")
    void shouldHandleRejection() {
        aggregateRejectionCommandTest.setUp();
        CommandHandlingAggregate testAggregate = aggregate();
        CommandHandlerExpected<TestUtilProjectAggregate> expected =
                aggregateRejectionCommandTest.expectThat(testAggregate);
        expected.throwsRejection(Rejection.class);
    }
}
