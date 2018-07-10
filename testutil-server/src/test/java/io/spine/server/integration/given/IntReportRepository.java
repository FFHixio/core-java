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

import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.integration.IntTaskAdded;
import io.spine.server.integration.ProjectId;
import io.spine.server.integration.ReportId;
import io.spine.server.route.EventRoute;

import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toSet;

public class IntReportRepository extends AggregateRepository<ReportId, IntReportAggregate> {

    private final List<IntReportAggregate> aggregates = newArrayList();

    public IntReportRepository() {
        getEventRouting().route(IntTaskAdded.class, (EventRoute<ReportId, IntTaskAdded>)
                (event, context) -> getReportsContainingProject(event.getProjectId()));
    }

    @Override
    public IntReportAggregate create(ReportId id) {
        IntReportAggregate aggregate = super.create(id);
        aggregates.add(aggregate);
        return aggregate;
    }

    private Set<ReportId> getReportsContainingProject(ProjectId projectId) {
        return aggregates
                .stream()
                .filter(report -> reportContainsProject(report, projectId))
                .map(AbstractEntity::getId)
                .collect(toSet());
    }

    private static boolean reportContainsProject(IntReportAggregate report, ProjectId projectId) {
        return report.getState()
                     .getProjectIdsList()
                     .contains(projectId);
    }
}
