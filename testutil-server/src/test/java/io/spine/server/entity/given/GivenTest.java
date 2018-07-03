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

package io.spine.server.entity.given;

import com.google.common.testing.NullPointerTester;
import io.spine.server.entity.given.GivenTestEnv.AProcessManager;
import io.spine.server.entity.given.GivenTestEnv.AProjection;
import io.spine.server.entity.given.GivenTestEnv.AnAggregate;
import io.spine.server.entity.given.GivenTestEnv.AnAggregatePart;
import io.spine.server.entity.given.GivenTestEnv.AnEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.test.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexander Yevsyukov
 * @author Illia Shepilov
 */
@DisplayName("Given should")
class GivenTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Given.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(Given.class);
    }

    @Test
    @DisplayName("create entity builder")
    void createEntityBuilder() {
        assertEquals(AnEntity.class, Given.entityOfClass(AnEntity.class)
                                          .getResultClass());
    }

    @Test
    @DisplayName("create aggregate builder")
    void createAggregateBuilder() {
        assertEquals(AnAggregate.class, Given.aggregateOfClass(AnAggregate.class)
                                             .getResultClass());
    }

    @Test
    @DisplayName("create aggregate part builder")
    void createAggregatePartBuilder() {
        assertEquals(AnAggregatePart.class, Given.aggregatePartOfClass(AnAggregatePart.class)
                                                 .getResultClass());
    }

    @Test
    @DisplayName("create projection builder")
    void createProjectionBuilder() {
        assertEquals(AProjection.class, Given.projectionOfClass(AProjection.class)
                                             .getResultClass());
    }

    @Test
    @DisplayName("create builder for process managers")
    void createBuilderForProcessManagers() {
        assertEquals(AProcessManager.class, Given.processManagerOfClass(AProcessManager.class)
                                                 .getResultClass());
    }
}
