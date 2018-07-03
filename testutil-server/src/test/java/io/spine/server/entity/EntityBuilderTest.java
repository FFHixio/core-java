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

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alexander Yevsyukov
 * @author Dmytro Dashenkov
 */
@SuppressWarnings({"ConstantConditions" /* Some of the methods test `null` arguments. */,
        "ResultOfMethodCallIgnored" /* We ignore when we test for `null`s. */})
@DisplayName("EntityBuilder should")
class EntityBuilderTest {

    /**
     * Convenience method that mimics the way tests would call creation of an entity.
     */
    private static EntityBuilder<TestEntity, Long, StringValue> givenEntity() {
        final EntityBuilder<TestEntity, Long, StringValue> builder = new EntityBuilder<>();
        return builder.setResultClass(TestEntity.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(EntityBuilder.class);
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("not accept null ID")
    void notAcceptNullID() {
        assertThrows(NullPointerException.class, () -> givenEntity().withId(null));
    }

    @Test
    @DisplayName("not accept null state")
    void notAcceptNullState() {
        assertThrows(NullPointerException.class, () -> givenEntity().withState(null));
    }

    @Test
    @DisplayName("not accept null timestamp")
    void notAcceptNullTimestamp() {
        assertThrows(NullPointerException.class, () -> givenEntity().modifiedOn(null));
    }

    @Test
    @DisplayName("obtain entity ID class")
    void getEntityIdClass() {
        assertEquals(Long.class, givenEntity().getIdClass());
    }

    @Test
    @DisplayName("create entity")
    void createEntity() {
        final long id = 1024L;
        final int version = 100500;
        final StringValue state = toMessage(getClass().getName());
        final Timestamp timestamp = Time.getCurrentTime();

        final VersionableEntity entity = givenEntity()
                .withId(id)
                .withVersion(version)
                .withState(state)
                .modifiedOn(timestamp)
                .build();

        assertEquals(TestEntity.class, entity.getClass());
        assertEquals(id, entity.getId());
        assertEquals(state, entity.getState());
        assertEquals(version, entity.getVersion().getNumber());
        assertEquals(timestamp, entity.getVersion().getTimestamp());
    }

    @Test
    @DisplayName("create entity with default values")
    void createWithDefaultValues() {
        final VersionableEntity entity = givenEntity().build();

        assertEquals(TestEntity.class, entity.getClass());
        assertEquals(0L, entity.getId());
        assertEquals(toMessage(""), entity.getState());
        assertEquals(0, entity.getVersion().getNumber());
    }

    private static class TestEntity extends AbstractVersionableEntity<Long, StringValue> {
        protected TestEntity(Long id) {
            super(id);
        }
    }
}
