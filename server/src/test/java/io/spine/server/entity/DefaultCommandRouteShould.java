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

import com.google.common.base.Optional;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.spine.server.route.DefaultCommandRoute;
import io.spine.test.entity.command.EntCreateProject;
import io.spine.testdata.Sample;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Litus
 */
public class DefaultCommandRouteShould {

    @Test
    @DisplayName("return empty Optional if fail to get ID from command message without ID field")
    void returnEmptyOptionalIfFailToGetIDFromCommandMessageWithoutIDField() {
        final Optional id = DefaultCommandRoute.asOptional(Empty.getDefaultInstance());

        assertFalse(id.isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We call isPresent() in assertion.
    @Test
    @DisplayName("get ID from command message")
    void getIDFromCommandMessage() {
        final EntCreateProject msg = Sample.messageOfType(EntCreateProject.class);

        final Optional id = DefaultCommandRoute.asOptional(msg);

        assertTrue(id.isPresent());
        assertEquals(msg.getProjectId(), id.get());
    }
}
