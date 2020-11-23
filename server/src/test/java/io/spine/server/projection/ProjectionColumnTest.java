/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.projection;

import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.server.entity.storage.EntityRecordColumn;
import io.spine.server.entity.storage.EntityRecordSpec;
import io.spine.server.projection.given.SavedString;
import io.spine.server.projection.given.SavingProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth8.assertThat;

@Nested
@DisplayName("Projection should have columns")
class ProjectionColumnTest {

    private static final EntityRecordSpec<String, SavedString, SavingProjection> recordSpec =
            EntityRecordSpec.of(SavingProjection.class);

    @Test
    @DisplayName("`version`")
    void version() {
        assertHasColumn(EntityRecordColumn.version.columnName());
    }

    @Test
    @DisplayName("`archived` and `deleted`")
    void lifecycleColumns() {
        assertHasColumn(EntityRecordColumn.archived.columnName());
        assertHasColumn(EntityRecordColumn.deleted.columnName());
    }

    private static void assertHasColumn(ColumnName columnName) {
        Optional<Column<?, ?>> result = recordSpec.findColumn(columnName);
        assertThat(result).isPresent();
    }
}
