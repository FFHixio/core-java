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

package io.spine.server.entity.storage;

import com.google.common.testing.NullPointerTester;
import com.google.common.testing.NullPointerTester.Visibility;
import com.google.protobuf.Any;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithManyGetters;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithNoStorageFields;
import io.spine.server.entity.storage.given.ColumnsTestEnv.EntityWithRepeatedColumnNames;
import io.spine.server.entity.storage.given.ColumnsTestEnv.RealLifeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static io.spine.server.entity.storage.Columns.extractColumnValues;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.entity.storage.given.ColumnsTestEnv.CUSTOM_COLUMN_NAME;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.test.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.test.Verify.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("Columns utility should")
class ColumnsTest {

    private static final String STRING_ID = "some-string-id-never-used";

    /**
     * Helper method that checks all {@link EntityWithManyGetters} field values.
     *
     * <p>Created to avoid code duplication, as {@link EntityWithManyGetters} is
     * the main {@link Entity} class used to test
     * {@linkplain Columns#extractColumnValues(Entity, Collection) column extraction}
     * functionality.
     */
    private
    static void checkEntityWithManyGettersFields(EntityWithManyGetters entity,
                                                 Map<String, MemoizedValue> fields) {
        assertNotNull(fields);

        assertSize(3, fields);

        String floatNullKey = "floatNull";
        MemoizedValue floatMemoizedNull = fields.get(floatNullKey);
        assertNotNull(floatMemoizedNull);
        assertNull(floatMemoizedNull.getValue());

        assertEquals(entity.getIntegerFieldValue(),
                     fields.get(CUSTOM_COLUMN_NAME)
                           .getValue());

        String messageKey = "someMessage";
        assertEquals(entity.getSomeMessage(),
                     fields.get(messageKey)
                           .getValue());
    }

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Columns.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testStaticMethods(Columns.class, Visibility.PACKAGE);
    }

    @Test
    @DisplayName("get all valid columns for entity class")
    void getAllColumns() {
        Collection<EntityColumn> entityColumns = Columns.getAllColumns(EntityWithManyGetters.class);

        assertNotNull(entityColumns);
        assertSize(3, entityColumns);
    }

    @Test
    @DisplayName("fail to obtain columns for invalid entity class")
    void rejectInvalidColumns() {
        assertThrows(IllegalStateException.class,
                     () -> Columns.getAllColumns(EntityWithRepeatedColumnNames.class));
    }

    @Test
    @DisplayName("retrieve specific column metadata from given class")
    void getColumnByName() {
        Class<? extends Entity<?, ?>> entityClass = RealLifeEntity.class;
        String existingColumnName = archived.name();
        EntityColumn archivedColumn = findColumn(entityClass, existingColumnName);
        assertNotNull(archivedColumn);
        assertEquals(existingColumnName, archivedColumn.getName());
    }

    @Test
    @DisplayName("fail to retrieve non-existing column")
    void notGetNonExisting() {
        Class<? extends Entity<?, ?>> entityClass = EntityWithNoStorageFields.class;
        String nonExistingColumnName = "foo";

        assertThrows(IllegalArgumentException.class,
                     () -> findColumn(entityClass, nonExistingColumnName));
    }

    @Test
    @DisplayName("extract column values with names for storing")
    void getColumnValues() {
        EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        Collection<EntityColumn> entityColumns = Columns.getAllColumns(entity.getClass());
        Map<String, MemoizedValue> fields = extractColumnValues(entity, entityColumns);

        checkEntityWithManyGettersFields(entity, fields);
    }

    @Test
    @DisplayName("extract column values using predefined columns")
    void getValuesOfPredefinedColumns() {
        EntityWithManyGetters entity = new EntityWithManyGetters(STRING_ID);
        Collection<EntityColumn> entityColumns = Columns.getAllColumns(entity.getClass());
        Map<String, MemoizedValue> fields = extractColumnValues(entity, entityColumns);

        checkEntityWithManyGettersFields(entity, fields);
    }
}
