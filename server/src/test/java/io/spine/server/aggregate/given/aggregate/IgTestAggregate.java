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

package io.spine.server.aggregate.given.aggregate;

import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.aggregate.ProjectVBuilder;
import io.spine.test.aggregate.Status;
import io.spine.test.aggregate.command.AggCreateProject;
import io.spine.test.aggregate.command.AggStartProject;
import io.spine.test.aggregate.event.AggProjectCreated;
import io.spine.test.aggregate.event.AggProjectStarted;

import static io.spine.server.aggregate.given.Given.EventMessage.projectCreated;
import static io.spine.server.aggregate.given.Given.EventMessage.projectStarted;

/**
 * An aggregate class for {@linkplain
 * io.spine.server.aggregate.IdempotencyGuardTest IdempotencyGuard tests}.
 *
 * @author Mykhailo Drachuk
 */
public class IgTestAggregate
        extends Aggregate<ProjectId, Project, ProjectVBuilder> {

    public IgTestAggregate(ProjectId id) {
        super(id);
    }

    @Assign
    AggProjectCreated handle(AggCreateProject cmd) {
        final AggProjectCreated event = projectCreated(cmd.getProjectId(),
                                                       cmd.getName());
        return event;
    }

    @Assign
    AggProjectStarted handle(AggStartProject cmd) {
        final AggProjectStarted message = projectStarted(cmd.getProjectId());
        return message;
    }

    @Apply
    void event(AggProjectCreated event) {
        getBuilder()
                .setId(event.getProjectId())
                .setStatus(Status.CREATED);
    }

    @Apply
    void event(AggProjectStarted event) {
        getBuilder()
                .setId(event.getProjectId())
                .setStatus(Status.STARTED);
    }
}
