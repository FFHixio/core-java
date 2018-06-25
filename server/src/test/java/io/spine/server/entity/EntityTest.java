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

package io.spine.server.entity;

import com.google.protobuf.StringValue;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.entity.given.EntityTestEnv.BareBonesEntity;
import io.spine.server.entity.given.EntityTestEnv.EntityWithMessageId;
import io.spine.server.entity.given.EntityTestEnv.TestEntityWithIdInteger;
import io.spine.server.entity.given.EntityTestEnv.TestEntityWithIdLong;
import io.spine.server.entity.given.EntityTestEnv.TestEntityWithIdMessage;
import io.spine.server.entity.given.EntityTestEnv.TestEntityWithIdString;
import io.spine.server.entity.rejection.CannotModifyArchivedEntity;
import io.spine.server.entity.rejection.CannotModifyDeletedEntity;
import io.spine.test.Tests;
import io.spine.test.TimeTests;
import io.spine.test.entity.Project;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.entity.given.EntityTestEnv.isBetween;
import static io.spine.test.Tests.assertSecondsEqual;
import static io.spine.test.Tests.nullRef;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit 5 Nested classes cannot to be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("Entity should")
class EntityTest {

    private Project state = Sample.messageOfType(Project.class);
    private TestEntity entityNew;
    private TestEntity entityWithState;

    @BeforeEach
    void setUp() {
        state = Sample.messageOfType(Project.class);
        entityNew = TestEntity.newInstance(newUuid());
        entityWithState = TestEntity.withState();
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored") // Because we expect the exception.
    @Test
    @DisplayName("not accept null ID")
    void notAcceptNullId() {
        assertThrows(NullPointerException.class, () -> new BareBonesEntity(Tests.<Long>nullRef()));
    }

    @Nested
    @DisplayName("return default state")
    class ReturnDefaultState {

        @Test
        @DisplayName("for single entity")
        void forSingleEntity() {
            final Project state = entityNew.getDefaultState();
            assertEquals(Project.getDefaultInstance(), state);
        }

        @Test
        @DisplayName("for different entities")
        void forDifferentEntities() {
            assertEquals(Project.getDefaultInstance(), entityNew.getDefaultState());

            final EntityWithMessageId entityWithMessageId = new EntityWithMessageId();
            final StringValue expected = StringValue.getDefaultInstance();
            assertEquals(expected, entityWithMessageId.getDefaultState());
        }
    }

    @Nested
    @DisplayName("in constructor, accept ID of type")
    class AcceptId {

        @Test
        @DisplayName("String")
        void ofStringType() {
            final String stringId = "stringId";
            final TestEntityWithIdString entityWithStringId = new TestEntityWithIdString(stringId);

            assertEquals(stringId, entityWithStringId.getId());
        }

        @Test
        @DisplayName("Long")
        void ofLongType() {
            final Long longId = 12L;
            final TestEntityWithIdLong entityWithLongId = new TestEntityWithIdLong(longId);

            assertEquals(longId, entityWithLongId.getId());
        }

        @Test
        @DisplayName("Integer")
        void ofIntegerType() {
            final Integer integerId = 12;
            final TestEntityWithIdInteger entityWithIntegerId =
                    new TestEntityWithIdInteger(integerId);

            assertEquals(integerId, entityWithIntegerId.getId());
        }

        @Test
        @DisplayName("Message")
        void ofMessageType() {
            final StringValue messageId = toMessage("messageId");
            final TestEntityWithIdMessage entityWithMessageID =
                    new TestEntityWithIdMessage(messageId);

            assertEquals(messageId, entityWithMessageID.getId());
        }
    }

    @Test
    @DisplayName("have state")
    void haveState() {
        final Version ver = Versions.newVersion(3, getCurrentTime());

        entityNew.updateState(state, ver);

        assertEquals(state, entityNew.getState());
        assertEquals(ver, entityNew.getVersion());
    }

    @Test
    @DisplayName("check entity state when setting it")
    void checkStateWhenUpdating() {
        final TestEntity spyEntityNew = spy(entityNew);
        spyEntityNew.updateState(state, Versions.zero());
        verify(spyEntityNew).checkEntityState(eq(state));
    }

    @Test
    @DisplayName("throw exception if try to set null state")
    void throwOnSettingNullState() {
        assertThrows(NullPointerException.class,
                     () -> entityNew.updateState(Tests.nullRef(), Versions.zero()));
    }

    @Test
    @DisplayName("update state")
    void updateState() {
        entityNew.incrementState(state);

        assertEquals(state, entityNew.getState());
    }

    @Test
    @DisplayName("have zero version by default")
    void haveZeroVersionByDefault() {
        assertEquals(0, entityNew.getVersion()
                                 .getNumber());
    }

    @Nested
    @DisplayName("increment version")
    class IncrementVersion {

        @Test
        @DisplayName("when told to do so")
        void whenAsked() {
            final int version = entityNew.incrementVersion();
            assertEquals(1, version);
        }

        @Test
        @DisplayName("when updating state")
        void whenUpdatingState() {
            entityNew.incrementState(state);

            assertEquals(1, entityNew.getVersion()
                                     .getNumber());
        }
    }

    @Nested
    @DisplayName("record modification time")
    class RecordModificationTime {

        @SuppressWarnings("CheckReturnValue")
        // New entity version number can be ignored in this test.
        @Test
        @DisplayName("when incrementing version")
        void onVersionIncrement() {
            final long timeBeforeIncrement = TimeTests.currentTimeSeconds();
            entityNew.incrementVersion();
            final long timeAfterIncrement = TimeTests.currentTimeSeconds();
            assertThat(entityNew.whenModified()
                                .getSeconds(),
                       isBetween(timeBeforeIncrement, timeAfterIncrement));
        }

        @Test
        @DisplayName("when updating state")
        void onStateUpdate() {
            entityNew.incrementState(state);
            final long expectedTimeSec = TimeTests.currentTimeSeconds();

            assertSecondsEqual(expectedTimeSec, entityNew.whenModified()
                                                         .getSeconds(), 1);
        }
    }

    @Nested
    @DisplayName("provide `equals` method such that")
    class ProvideEqualsSuchThat {

        @Test
        @DisplayName("same entities are equal")
        void equalToSame() {
            final TestEntity another = TestEntity.withStateOf(entityWithState);

            assertEquals(entityWithState, another);
        }

        @SuppressWarnings("EqualsWithItself") // Is the purpose of this method.
        @Test
        @DisplayName("entity is equal to itself")
        void equalToItself() {
            assertEquals(entityWithState, entityWithState);
        }

        @Test
        @DisplayName("entity is not equal to null")
        void notEqualToNull() {
            assertNotEquals(entityWithState, nullRef());
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes") // Is the purpose of this method.
        @Test
        @DisplayName("entity is not equal to object of another class")
        void notEqualToOtherClass() {
            assertNotEquals(entityWithState, newUuid());
        }

        @Test
        @DisplayName("entities with different IDs are not equal")
        void notEqualToDifferentId() {
            final TestEntity another = TestEntity.newInstance(newUuid());

            assertNotEquals(entityWithState.getId(), another.getId());
            assertNotEquals(entityWithState, another);
        }

        @Test
        @DisplayName("entities with different states are not equal")
        void notEqualToDifferentState() {
            final TestEntity another = TestEntity.withStateOf(entityWithState);
            another.updateState(Sample.messageOfType(Project.class), another.getVersion());

            assertNotEquals(entityWithState.getState(), another.getState());
            assertNotEquals(entityWithState, another);
        }

        @SuppressWarnings("CheckReturnValue") // The entity version can be ignored in this test.
        @Test
        @DisplayName("entities with different versions are not equal")
        void notEqualToDifferentVersion() {
            final TestEntity another = TestEntity.withStateOf(entityWithState);
            another.incrementVersion();

            assertNotEquals(entityWithState, another);
        }
    }

    @Nested
    @DisplayName("provide `hashCode` method such that")
    class ProvideHashCode {

        @Test
        @DisplayName("for entity with non-empty ID and state, non-zero hash code is generated")
        void nonZeroForNonEmptyEntity() {
            assertFalse(entityWithState.getId()
                                       .trim()
                                       .isEmpty());

            final int hashCode = entityWithState.hashCode();

            assertTrue(hashCode != 0);
        }

        @Test
        @DisplayName("for same instances, same hash code is generated")
        void sameForSameInstances() {
            assertEquals(entityWithState.hashCode(), entityWithState.hashCode());
        }

        @Test
        @DisplayName("for different instances, unique hash code is generated")
        void uniqueForDifferentInstances() {
            final TestEntity another = TestEntity.withState();

            assertNotEquals(entityWithState.hashCode(), another.hashCode());
        }
    }

    @Nested
    @DisplayName("have visibility status such that")
    class HaveVisibilityStatus {

        @Test
        @DisplayName("entity has default status after construction")
        void defaultOnCreation() {
            assertEquals(LifecycleFlags.getDefaultInstance(), entityNew.getLifecycleFlags());
        }

        @Test
        @DisplayName("entity is not archived when created")
        void notArchivedOnCreation() {
            assertFalse(entityNew.isArchived());
        }

        @Test
        @DisplayName("entity supports archiving")
        void supportingArchiving() {
            entityNew.setArchived(true);

            assertTrue(entityNew.isArchived());
        }

        @Test
        @DisplayName("entity supports unarchiving")
        void supportingUnarchiving() {
            entityNew.setArchived(true);
            entityNew.setArchived(false);

            assertFalse(entityNew.isArchived());
        }

        @Test
        @DisplayName("entity is not deleted when created")
        void notDeletedOnCreation() {
            assertFalse(entityNew.isDeleted());
        }

        @Test
        @DisplayName("entity supports deletion")
        void supportingDeletion() {
            entityNew.setDeleted(true);

            assertTrue(entityNew.isDeleted());
        }

        @Test
        @DisplayName("entity supports restoration")
        void supportingRestoration() {
            entityNew.setDeleted(true);
            entityNew.setDeleted(false);
            assertFalse(entityNew.isDeleted());
        }

        @Test
        @DisplayName("entities with different status are not equal")
        void consideredForEquality() {
            // Create an entity with the same ID and the same (default) state.
            final AbstractVersionableEntity another = new TestEntityWithIdString(entityNew.getId());

            another.setArchived(true);

            assertFalse(entityNew.equals(another));
        }

        @Test
        @DisplayName("status can be assigned")
        void supportingAssignment() {
            final LifecycleFlags status = LifecycleFlags.newBuilder()
                                                        .setArchived(true)
                                                        .setDeleted(false)
                                                        .build();
            entityNew.setLifecycleFlags(status);
            assertEquals(status, entityNew.getLifecycleFlags());
        }

        @Test
        @DisplayName("entity can be checked for not being archived")
        void supportingNotArchivedCheck() throws Throwable {
            entityNew.setArchived(true);

            // This should pass.
            entityNew.checkNotDeleted();

            assertThrows(CannotModifyArchivedEntity.class, () -> entityNew.checkNotArchived());
        }

        @Test
        @DisplayName("entity can be checked for not being deleted")
        void supportingNotDeletedCheck() throws Throwable {
            entityNew.setDeleted(true);

            // This should pass.
            entityNew.checkNotArchived();

            assertThrows(CannotModifyDeletedEntity.class, () -> entityNew.checkNotDeleted());
        }
    }
}

