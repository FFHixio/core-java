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

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.Version;
import io.spine.server.entity.given.TransactionalEntityTestEnv.TeEntity;
import io.spine.validate.ValidatingBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.test.TestValues.newUuidValue;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Alex Tymchenko
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("TransactionalEntity should")
class TransactionalEntityTest {

    protected TransactionalEntity newEntity() {
        return new TeEntity(1L);
    }

    @Test
    @DisplayName("have private constructor for TypeInfo")
    void havePrivateTypeInfoCtor() {
        assertHasPrivateParameterlessCtor(TransactionalEntity.TypeInfo.class);
    }

    @Nested
    @DisplayName("be non-changed")
    class BeNonChanged {

        @Test
        @DisplayName("once created")
        void onCreation() {
            assertFalse(newEntity().isChanged());
        }

        @Test
        @DisplayName("if transaction isn't changed")
        void withUnchangedTx() {
            final TransactionalEntity entity = entityWithActiveTx(false);

            assertFalse(entity.isChanged());
        }
    }

    @Nested
    @DisplayName("become changed")
    class BecomeChanged {

        @Test
        @DisplayName("if transaction state changed")
        void ifTxStateChanged() {
            final TransactionalEntity entity = entityWithActiveTx(true);

            assertTrue(entity.isChanged());
        }

        @Test
        @DisplayName("once `lifecycleFlags` are updated")
        void onLifecycleFlagsUpdated() {
            final TransactionalEntity entity = newEntity();
            entity.setLifecycleFlags(LifecycleFlags.newBuilder()
                                                   .setDeleted(true)
                                                   .build());
            assertTrue(entity.isChanged());
        }
    }

    @Test
    @DisplayName("have null transaction by default")
    void haveNullTxByDefault() {
        assertNull(newEntity().getTransaction());
    }

    @Nested
    @DisplayName("have no transaction in progress")
    class HaveNoTxInProgress {

        @Test
        @DisplayName("by default")
        void byDefault() {
            assertFalse(newEntity().isTransactionInProgress());
        }

        @Test
        @DisplayName("until transaction started")
        void untilTxStarted() {
            final TransactionalEntity entity = entityWithInactiveTx();

            assertFalse(entity.isTransactionInProgress());
        }
    }

    @Test
    @DisplayName("have transaction in progress when transaction is active")
    void haveTxInProgress() {
        final TransactionalEntity entity = entityWithActiveTx(false);

        assertTrue(entity.isTransactionInProgress());
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    @Test
    @DisplayName("allow injecting transaction")
    void allowInjectingTx() {
        final TransactionalEntity entity = newEntity();
        final Transaction tx = mock(Transaction.class);
        when(tx.getEntity()).thenReturn(entity);
        entity.injectTransaction(tx);

        assertEquals(tx, entity.getTransaction());
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    @Test
    @DisplayName("disallow injecting transaction wrapped around another entity instance")
    void disallowOtherInstanceTx() {
        final TransactionalEntity entity = newEntity();
        final Transaction tx = mock(Transaction.class);
        when(tx.getEntity()).thenReturn(newEntity());
        assertThrows(IllegalStateException.class, () -> entity.injectTransaction(tx));
    }

    @Nested
    @DisplayName("fail to archive")
    class FailToArchive {

        @Test
        @DisplayName("with no transaction")
        void withNoTx() {
            assertThrows(IllegalStateException.class, () -> newEntity().setArchived(true));
        }

        @Test
        @DisplayName("with inactive transaction")
        void withInactiveTx() {
            final TransactionalEntity entity = entityWithInactiveTx();
            assertThrows(IllegalStateException.class, () -> entity.setArchived(true));
        }
    }

    @Nested
    @DisplayName("fail to delete")
    class FailToDelete {

        @Test
        @DisplayName("with no transaction")
        void withNoTx() {
            assertThrows(IllegalStateException.class, () -> newEntity().setDeleted(true));
        }

        @Test
        @DisplayName("with inactive transaction")
        void withInactiveTx() {
            final TransactionalEntity entity = entityWithInactiveTx();

            assertThrows(IllegalStateException.class, () -> entity.setDeleted(true));
        }
    }

    @Test
    @DisplayName("return transaction `lifecycleFlags` if transaction is active")
    void returnActiveTxFlags() {
        final TransactionalEntity entity = entityWithInactiveTx();
        final LifecycleFlags originalFlags = entity.getLifecycleFlags();

        final LifecycleFlags modifiedFlags = originalFlags.toBuilder()
                                                          .setDeleted(true)
                                                          .build();

        assertNotEquals(originalFlags, modifiedFlags);

        final Transaction txMock = entity.getTransaction();
        assertNotNull(txMock);
        when(txMock.isActive()).thenReturn(true);
        when(txMock.getLifecycleFlags()).thenReturn(modifiedFlags);

        final LifecycleFlags actual = entity.getLifecycleFlags();
        assertEquals(modifiedFlags, actual);
    }

    @Nested
    @DisplayName("return builder from state")
    class ReturnBuilderFromState {

        @Test
        @DisplayName("which is non-null")
        void nonNull() {
            final ValidatingBuilder builder = newEntity().builderFromState();
            assertNotNull(builder);
        }

        @Test
        @DisplayName("which reflects current state")
        void reflectingCurrentState() {
            final TransactionalEntity entity = newEntity();
            final Message originalState = entity.builderFromState()
                                                .build();

            final StringValue newState = newUuidValue();
            assertNotEquals(originalState, newState);

            TestTransaction.injectState(entity, newState, Version.getDefaultInstance());
            final Message modifiedState = entity.builderFromState()
                                                .build();

            assertEquals(newState, modifiedState);
        }
    }

    @SuppressWarnings("unchecked")  // OK for the test code.
    private TransactionalEntity entityWithInactiveTx() {
        final TransactionalEntity entity = newEntity();

        final Transaction tx = mock(Transaction.class);
        when(tx.isActive()).thenReturn(false);
        when(tx.getEntity()).thenReturn(entity);
        entity.injectTransaction(tx);
        return entity;
    }

    @SuppressWarnings("unchecked")  // OK for the test.
    private TransactionalEntity entityWithActiveTx(boolean txChanged) {
        final TransactionalEntity entity = newEntity();
        final Transaction tx = spy(mock(Transaction.class));
        when(tx.isActive()).thenReturn(true);
        when(tx.isStateChanged()).thenReturn(txChanged);
        when(tx.getEntity()).thenReturn(entity);

        entity.injectTransaction(tx);
        return entity;
    }
}
