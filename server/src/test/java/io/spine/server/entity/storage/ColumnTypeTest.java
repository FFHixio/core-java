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

import io.spine.server.entity.storage.given.ColumnTypeTestEnv.TestColumnType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An example of using {@link ColumnType} {@code interface}.
 *
 * @author Dmytro Dashenkov
 */
@DisplayName("ColumnType should")
class ColumnTypeTest {

    private static final int VALUE = 42;
    private static final String KEY = "the Answer";

    private static final String EXPECTED_RESULT = "the Answer: 42";

    @Test
    @DisplayName("convert java type to storage type")
    void convertJavaType() {
        final String stringValue = String.valueOf(VALUE);
        final ColumnType<Integer, String, ?, ?> type = new TestColumnType<>();
        final String storedValue = type.convertColumnValue(VALUE);
        assertEquals(stringValue, storedValue);
    }

    @Test
    @DisplayName("store value to container")
    void storeValueToContainer() {
        final String stringValue = String.valueOf(VALUE);
        final StringBuilder container = new StringBuilder(16);
        final ColumnType<?, String, StringBuilder, String> type = new TestColumnType<>();
        type.setColumnValue(container, stringValue, KEY);

        assertThat(container.toString(), containsString(EXPECTED_RESULT));
    }

    @Test
    @DisplayName("provide interface for entire storage preparation flow")
    void haveInterfaceForEntireFlow() {
        final ColumnType<Integer, String, StringBuilder, String> type = new TestColumnType<>();
        final StringBuilder container = new StringBuilder(16);

        type.setColumnValue(container, type.convertColumnValue(VALUE), KEY);

        assertThat(container.toString(), containsString(EXPECTED_RESULT));
    }
}
