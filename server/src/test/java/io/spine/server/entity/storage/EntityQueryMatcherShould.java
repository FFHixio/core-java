/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Test;
import io.spine.client.ColumnFilter;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.EntityRecord;
import io.spine.test.entity.Project;
import io.spine.test.entity.ProjectId;
import io.spine.test.entity.TaskId;
import io.spine.testdata.Sample;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.ImmutableMultimap.of;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.server.entity.storage.EntityRecordWithColumns.of;

/**
 * @author Dmytro Dashenkov
 */
public class EntityQueryMatcherShould {

    @Test
    public void match_everything_except_null_to_empty_query() {
        final Collection<Object> idFilter = Collections.emptyList();
        final EntityQuery<?> query = EntityQuery.of(idFilter, defaultQueryParameters());

        final EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);

        assertFalse(matcher.apply(null));
        assertTrue(matcher.apply(of(EntityRecord.getDefaultInstance())));
    }

    @Test
    public void match_ids() {
        final Message genericId = Sample.messageOfType(ProjectId.class);
        final Collection<Object> idFilter = Collections.<Object>singleton(genericId);
        final Any entityId = AnyPacker.pack(genericId);
        final EntityQuery<?> query = EntityQuery.of(idFilter, defaultQueryParameters());

        final EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);
        final EntityRecord matching = EntityRecord.newBuilder()
                                                  .setEntityId(entityId)
                                                  .build();
        final Any otherEntityId = AnyPacker.pack(Sample.messageOfType(ProjectId.class));
        final EntityRecord nonMatching = EntityRecord.newBuilder()
                                                     .setEntityId(otherEntityId)
                                                     .build();
        final EntityRecordWithColumns matchingRecord = of(matching);
        final EntityRecordWithColumns nonMatchingRecord = of(nonMatching);
        assertTrue(matcher.apply(matchingRecord));
        assertFalse(matcher.apply(nonMatchingRecord));
    }

    @SuppressWarnings("unchecked") // Mocks <-> reflection issues
    @Test
    public void match_columns() {
        final String targetName = "feature";
        final Column target = mock(Column.class);
        when(target.isNullable()).thenReturn(true);
        when(target.getName()).thenReturn(targetName);
        when(target.getType()).thenReturn(Boolean.class);
        final Serializable acceptedValue = true;

        final Collection<Object> ids = Collections.emptyList();

        final Multimap<Column, ColumnFilter> filters = of(target, eq(targetName, acceptedValue));
        final CompositeQueryParameter parameter = CompositeQueryParameter.from(filters, ALL);
        final QueryParameters params = QueryParameters.newBuilder()
                                                      .add(parameter)
                                                      .build();
        final EntityQuery<?> query = EntityQuery.of(ids, params);

        final Any matchingId = AnyPacker.pack(Sample.messageOfType(TaskId.class));
        final Any nonMatchingId = AnyPacker.pack(Sample.messageOfType(TaskId.class));

        final EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);
        final EntityRecord matching = EntityRecord.newBuilder()
                                                  .setEntityId(matchingId)
                                                  .build();
        final EntityRecord nonMatching = EntityRecord.newBuilder()
                                                     .setEntityId(nonMatchingId)
                                                     .build();
        final Column.MemoizedValue storedValue = mock(Column.MemoizedValue.class);
        when(storedValue.getSourceColumn()).thenReturn(target);
        when(storedValue.getValue()).thenReturn(acceptedValue);
        final Map<String, Column.MemoizedValue> matchingColumns =
                ImmutableMap.<String, Column.MemoizedValue>of(targetName, storedValue);
        final EntityRecordWithColumns nonMatchingRecord = of(nonMatching);
        final EntityRecordWithColumns matchingRecord = of(matching, matchingColumns);

        assertTrue(matcher.apply(matchingRecord));
        assertFalse(matcher.apply(nonMatchingRecord));
    }

    @Test
    public void match_Any_instances() {
        final String columnName = "column";

        final Project someMessage = Sample.messageOfType(Project.class);
        final Any actualValue = AnyPacker.pack(someMessage);

        final Column column = mock(Column.class);
        when(column.getType()).thenReturn(Any.class);
        when(column.getName()).thenReturn(columnName);

        final Column.MemoizedValue value = mock(Column.MemoizedValue.class);
        when(value.getSourceColumn()).thenReturn(column);
        when(value.getValue()).thenReturn(actualValue);

        final EntityRecord record = Sample.messageOfType(EntityRecord.class);
        final Map<String, Column.MemoizedValue> columns = singletonMap(columnName, value);
        final EntityRecordWithColumns recordWithColumns = of(record, columns);

        final Multimap<Column, ColumnFilter> filters = of(column, eq(columnName, actualValue));
        final CompositeQueryParameter parameter = CompositeQueryParameter.from(filters, ALL);
        final QueryParameters parameters = QueryParameters.newBuilder()
                                                          .add(parameter)
                                                          .build();
        final EntityQuery<?> query = EntityQuery.of(emptySet(), parameters);

        final EntityQueryMatcher<?> matcher = new EntityQueryMatcher<>(query);
        assertTrue(matcher.apply(recordWithColumns));
    }

    private static QueryParameters defaultQueryParameters() {
        return QueryParameters.newBuilder()
                              .build();
    }
}
