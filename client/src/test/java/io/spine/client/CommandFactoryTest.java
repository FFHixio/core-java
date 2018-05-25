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

package io.spine.client;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.core.given.GivenTenantId;
import io.spine.core.given.GivenUserId;
import io.spine.test.TimeTests;
import io.spine.test.commands.RequiredFieldCommand;
import io.spine.time.Timestamps2;
import io.spine.time.ZoneOffset;
import io.spine.time.ZoneOffsets;
import io.spine.validate.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Command factory should")
class CommandFactoryTest extends ActorRequestFactoryTest {

    @Test
    @DisplayName("create command context for given parameters")
    void createCommandContext() {
        final TenantId tenantId = GivenTenantId.newUuid();
        final UserId userId = GivenUserId.newUuid();
        final ZoneOffset zoneOffset = ZoneOffsets.ofHours(-3);
        final int targetVersion = 100500;

        final CommandContext commandContext = CommandFactory.createContext(tenantId,
                                                                           userId,
                                                                           zoneOffset,
                                                                           targetVersion);

        final ActorContext actorContext = commandContext.getActorContext();

        assertEquals(tenantId, actorContext.getTenantId());
        assertEquals(userId, actorContext.getActor());
        assertEquals(zoneOffset, actorContext.getZoneOffset());
        assertEquals(targetVersion, commandContext.getTargetVersion());
    }

    @Nested
    @DisplayName("on command instance creation")
    class CreateCommandTest {

        @Test
        @DisplayName("assign current time to command")
        void createWithTimestamp() {
            // We are creating a range of +/- second between the call to make sure the timestamp
            // would fit into this range. The purpose of this test is to make sure it works with
            // this precision and to add coverage.
            final Timestamp beforeCall = TimeTests.Past.secondsAgo(1);
            final Command command = factory().command()
                                             .create(StringValue.getDefaultInstance());
            final Timestamp afterCall = TimeTests.Future.secondsFromNow(1);

            assertTrue(Timestamps2.isBetween(
                    command.getContext()
                           .getActorContext()
                           .getTimestamp(), beforeCall, afterCall));
        }

        @Test
        @DisplayName("assign given entity version to command")
        void createWithEntityVersion() {
            final Command command = factory().command()
                                             .create(StringValue.getDefaultInstance(), 2);

            assertEquals(2, command.getContext()
                                   .getTargetVersion());
        }

        @Test
        @DisplayName("assign own tenant ID to command")
        void createWithTenantID() {
            final TenantId tenantId = TenantId.newBuilder()
                                              .setValue(getClass().getSimpleName())
                                              .build();
            final ActorRequestFactory mtFactory = ActorRequestFactory.newBuilder()
                                                                     .setTenantId(tenantId)
                                                                     .setActor(getActor())
                                                                     .setZoneOffset(getZoneOffset())
                                                                     .build();
            final Command command = mtFactory.command()
                                             .create(StringValue.getDefaultInstance());

            assertEquals(tenantId, command.getContext()
                                          .getActorContext()
                                          .getTenantId());
        }

        @Test
        @DisplayName("throw ValidationException if supplied with invalid Message")
        void failForInvalidMessage() {
            final RequiredFieldCommand invalidCommand =
                    RequiredFieldCommand.getDefaultInstance();
            assertThrows(ValidationException.class, () -> factory().command()
                                                                   .create(invalidCommand));
        }

        @Test
        @DisplayName("throw ValidationException if supplied with invalid Message with version")
        void failForInvalidMessageWithVersion() {
            final RequiredFieldCommand invalidCommand =
                    RequiredFieldCommand.getDefaultInstance();
            assertThrows(ValidationException.class, () -> factory().command()
                                                                   .create(invalidCommand, 42));
        }
    }
}
