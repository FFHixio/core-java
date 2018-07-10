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

package io.spine.server.integration.given;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.integration.IntAddTask;
import io.spine.server.integration.IntCreateProject;
import io.spine.server.integration.IntProjectAlreadyStarted;
import io.spine.server.integration.IntProjectCreated;
import io.spine.server.integration.IntProjectStarted;
import io.spine.server.integration.IntStartProject;
import io.spine.server.integration.IntTaskAdded;
import io.spine.server.integration.IntTaskCreatedInCompletedProject;
import io.spine.server.integration.Project;
import io.spine.server.integration.ProjectId;
import io.spine.server.integration.ProjectVBuilder;
import io.spine.server.integration.Task;

import static io.spine.server.integration.Project.Status.COMPLETED;
import static io.spine.server.integration.Project.Status.CREATED;
import static io.spine.server.integration.Project.Status.STARTED;

public class IntProjectAggregate extends Aggregate<ProjectId, Project, ProjectVBuilder> {

    protected IntProjectAggregate(ProjectId id) {
        super(id);
    }

    @Assign
    IntProjectCreated handle(IntCreateProject command) {
        return IntProjectCreated
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    IntProjectStarted handle(IntStartProject command) throws IntProjectAlreadyStarted {
        if (getState().getStatus() != CREATED) {
            throw new IntProjectAlreadyStarted(command.getProjectId());
        }
        return IntProjectStarted
                .newBuilder()
                .setProjectId(command.getProjectId())
                .build();
    }

    @Assign
    IntTaskAdded handle(IntAddTask command) throws IntTaskCreatedInCompletedProject {
        ProjectId projectId = command.getProjectId();
        Task task = command.getTask();
        if (getState().getStatus() == COMPLETED) {
            throw new IntTaskCreatedInCompletedProject(projectId, task);
        }
        return IntTaskAdded
                .newBuilder()
                .setProjectId(projectId)
                .setTask(task)
                .build();
    }

    @SuppressWarnings("ReturnValueIgnored")
    @Apply
    void on(IntProjectCreated event) {
        getBuilder().setId(event.getProjectId());
    }

    @SuppressWarnings("ReturnValueIgnored")
    @Apply
    void on(IntProjectStarted event) {
        getBuilder().setStatus(STARTED);
    }

    @SuppressWarnings("ReturnValueIgnored")
    @Apply
    void on(IntTaskAdded event) {
        getBuilder().addTask(event.getTask());
    }
}
