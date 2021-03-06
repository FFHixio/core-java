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

package io.spine.server.integration.given;

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.External;
import io.spine.server.event.React;
import io.spine.server.procman.ProcessManager;
import io.spine.test.integration.Project;
import io.spine.test.integration.ProjectId;
import io.spine.test.integration.event.ItgProjectCreated;
import io.spine.test.integration.rejection.IntegrationRejections;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")  // OK to preserve the state.
public class ProjectWizard
        extends ProcessManager<ProjectId, Project, Project.Builder> {

    protected ProjectWizard(ProjectId id) {
        super(id);
    }

    private static Message externalEvent = null;

    @React
    List<EventMessage> on(@External ItgProjectCreated event) {
        externalEvent = event;
        return Collections.emptyList();
    }

    @React
    List<EventMessage> on(@External IntegrationRejections.ItgCannotStartArchivedProject rejection) {
        externalEvent = rejection;
        return Collections.emptyList();
    }

    public static Message externalEvent() {
        return externalEvent;
    }

    public static void clear() {
        externalEvent = null;
    }
}
