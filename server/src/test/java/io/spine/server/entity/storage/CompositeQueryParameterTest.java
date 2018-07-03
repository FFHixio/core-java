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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import io.spine.client.ColumnFilter;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.ImmutableMultimap.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.ColumnFilters.ge;
import static io.spine.client.ColumnFilters.lt;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.CCF_CO_UNDEFINED;
import static io.spine.server.entity.storage.Columns.findColumn;
import static io.spine.server.entity.storage.CompositeQueryParameter.from;
import static io.spine.server.storage.EntityField.version;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.test.Verify.assertContainsAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("CompositeQueryParameter should")
class CompositeQueryParameterTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testStaticMethods(CompositeQueryParameter.class, PACKAGE);
    }

    @Test
    @DisplayName("be serializable")
    void beSerializable() {
        ImmutableMultimap<EntityColumn, ColumnFilter> filters = ImmutableMultimap.of();
        CompositeQueryParameter parameter = from(filters, ALL);
        SerializableTester.reserializeAndAssert(parameter);
    }

    @Test
    @DisplayName("fail to construct for invalid operator")
    void rejectInvalidOperator() {
        assertThrows(IllegalArgumentException.class,
                     () -> from(ImmutableMultimap.of(), CCF_CO_UNDEFINED));
    }

    @Test
    @DisplayName("merge with other instances")
    void mergeWithOtherInstances() {
        Class<? extends Entity> cls = AbstractVersionableEntity.class;

        String archivedColumnName = archived.name();
        String deletedColumnName = deleted.name();
        String versionColumnName = version.name();

        EntityColumn archivedColumn = findColumn(cls, archivedColumnName);
        EntityColumn deletedColumn = findColumn(cls, deletedColumnName);
        EntityColumn versionColumn = findColumn(cls, versionColumnName);

        ColumnFilter archived = eq(archivedColumnName, true);
        ColumnFilter deleted = eq(archivedColumnName, false);
        ColumnFilter versionLower = ge(archivedColumnName, 2);
        ColumnFilter versionUpper = lt(archivedColumnName, 10);

        CompositeQueryParameter lifecycle =
                from(of(archivedColumn, archived, deletedColumn, deleted), ALL);
        CompositeQueryParameter versionLowerBound =
                from(of(versionColumn, versionLower), ALL);
        CompositeQueryParameter versionUpperBound =
                from(of(versionColumn, versionUpper), ALL);
        // Merge the instances
        CompositeQueryParameter all =
                lifecycle.conjunct(newArrayList(versionLowerBound, versionUpperBound));

        // Check
        assertEquals(all.getOperator(), ALL);

        Multimap<EntityColumn, ColumnFilter> asMultimap = all.getFilters();
        assertContainsAll(asMultimap.get(versionColumn), versionLower, versionUpper);
        assertContainsAll(asMultimap.get(archivedColumn), archived);
        assertContainsAll(asMultimap.get(deletedColumn), deleted);
    }

    @Test
    @DisplayName("merge with single filter")
    void mergeWithSingleFilter() {
        Class<? extends Entity> cls = AbstractVersionableEntity.class;

        String archivedColumnName = archived.name();
        String deletedColumnName = deleted.name();
        String versionColumnName = version.name();

        EntityColumn archivedColumn = findColumn(cls, archivedColumnName);
        EntityColumn deletedColumn = findColumn(cls, deletedColumnName);
        EntityColumn versionColumn = findColumn(cls, versionColumnName);

        ColumnFilter archived = eq(archivedColumnName, false);
        ColumnFilter deleted = eq(archivedColumnName, false);
        ColumnFilter version = ge(archivedColumnName, 4);

        CompositeQueryParameter lifecycle =
                from(of(archivedColumn, archived, deletedColumn, deleted), ALL);
        // Merge the instances
        CompositeQueryParameter all = lifecycle.and(versionColumn, version);

        // Check
        assertEquals(all.getOperator(), ALL);

        Multimap<EntityColumn, ColumnFilter> asMultimap = all.getFilters();
        assertContainsAll(asMultimap.get(versionColumn), version);
        assertContainsAll(asMultimap.get(archivedColumn), archived);
        assertContainsAll(asMultimap.get(deletedColumn), deleted);
    }
}
