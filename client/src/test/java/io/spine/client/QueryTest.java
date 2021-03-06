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

package io.spine.client;

import io.spine.test.client.TestEntity;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`Query` should")
class QueryTest {

    private static final String TARGET_ENTITY_TYPE_URL =
            "type.spine.io/spine.test.queries.TestEntity";

    @Test
    @DisplayName("obtain entity type url for known query target type")
    void returnTypeUrlForKnownType() {
        Target target = Targets.allOf(TestEntity.class);
        Query query = Query
                .newBuilder()
                .setTarget(target)
                .build();
        TypeUrl type = query.targetType();
        assertNotNull(type);
        assertEquals(TARGET_ENTITY_TYPE_URL, type.toString());
    }

    @Test
    @DisplayName("throw `IllegalStateException` for unknown query target type")
    void throwErrorForUnknownType() {
        Target target = Target
                .newBuilder()
                .setType("nonexistent/message.type")
                .build();
        Query query = Query
                .newBuilder()
                .setTarget(target)
                .build();
        assertThrows(IllegalStateException.class, query::targetType);
    }
}
