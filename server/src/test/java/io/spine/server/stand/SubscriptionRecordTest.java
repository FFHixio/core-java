/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.stand;

import io.spine.client.CompositeFilter;
import io.spine.client.Filter;
import io.spine.client.Filters;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Subscriptions;
import io.spine.client.Target;
import io.spine.core.EventId;
import io.spine.server.type.EventEnvelope;
import io.spine.test.aggregate.Project;
import io.spine.test.aggregate.ProjectId;
import io.spine.test.event.EvTeamId;
import io.spine.test.event.ProjectCreated;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth8.assertThat;
import static io.spine.client.Targets.composeTarget;
import static io.spine.server.stand.SubscriptionRecordFactory.newRecordFor;
import static io.spine.server.stand.given.SubscriptionRecordTestEnv.OTHER_TYPE;
import static io.spine.server.stand.given.SubscriptionRecordTestEnv.projectCreatedEnvelope;
import static io.spine.server.stand.given.SubscriptionRecordTestEnv.stateChangedEnvelope;
import static io.spine.server.stand.given.SubscriptionRecordTestEnv.subscription;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("SubscriptionRecord should")
class SubscriptionRecordTest {

    private static final String TARGET_ID = "target-ID";
    public static final Project FRESH_PROJECT = Project.getDefaultInstance();

    @Test
    @DisplayName("fail to match improper type")
    void notMatchImproperType() {
        SubscriptionRecord record = newRecordFor(subscription());
        ProjectId id = ProjectId.getDefaultInstance();

        EventEnvelope envelope = stateChangedEnvelope(id, FRESH_PROJECT, FRESH_PROJECT);
        assertThat(record.findUpdates(envelope)).isPresent();

        EventEnvelope envelope2 = stateChangedEnvelope(id, FRESH_PROJECT, FRESH_PROJECT, OTHER_TYPE);
        assertThat(record.findUpdates(envelope2)).isEmpty();
    }

    @Nested
    @DisplayName("fail to match improper ID")
    class MatchById {

        @Test
        @DisplayName("in case of entity subscription")
        void entitySubscription() {
            ProjectId targetId = ProjectId
                    .newBuilder()
                    .setId(TARGET_ID)
                    .build();
            Project state = Project.getDefaultInstance();
            SubscriptionRecord record = newRecordFor(subscription(targetId));

            EventEnvelope envelope = stateChangedEnvelope(targetId, state, state);
            assertThat(record.findUpdates(envelope)).isPresent();

            ProjectId otherId = ProjectId
                    .newBuilder()
                    .setId("some-other-ID")
                    .build();
            EventEnvelope envelope2 = stateChangedEnvelope(otherId, state, state);
            assertThat(record.findUpdates(envelope2)).isEmpty();
        }

        @Test
        @DisplayName("in case of event subscription")
        void eventSubscription() {
            EventId targetId = EventId
                    .newBuilder()
                    .setValue(TARGET_ID)
                    .build();
            Target target = composeTarget(ProjectCreated.class, singleton(targetId), null);
            Subscription subscription = subscription(target);
            SubscriptionRecord record = newRecordFor(subscription);

            EventEnvelope envelope = projectCreatedEnvelope(targetId);
            assertThat(record.findUpdates(envelope)).isPresent();

            EventId otherId = EventId
                    .newBuilder()
                    .setValue("other-event-ID")
                    .build();
            EventEnvelope nonMatching = projectCreatedEnvelope(otherId);
            assertThat(record.findUpdates(nonMatching)).isEmpty();
        }
    }

    @Nested
    @DisplayName("fail to match improper state")
    class MatchByState {

        @Test
        @DisplayName("in case of entity subscription")
        void entitySubscription() {
            ProjectId targetId = ProjectId
                    .newBuilder()
                    .setId(TARGET_ID)
                    .build();
            String targetName = "super-project";

            Filter filter = Filters.eq("name", targetName);
            CompositeFilter compositeFilter = Filters.all(filter);
            Set<CompositeFilter> filters = singleton(compositeFilter);
            Target target = composeTarget(Project.class, singleton(targetId), filters);

            Subscription subscription = subscription(target);
            SubscriptionRecord record = newRecordFor(subscription);

            Project matching = Project
                    .newBuilder()
                    .setName(targetName)
                    .build();
            EventEnvelope envelope = stateChangedEnvelope(targetId, FRESH_PROJECT, matching);
            assertThat(record.findUpdates(envelope)).isPresent();

            Project nonMatching = Project
                    .newBuilder()
                    .setName("some-other-name")
                    .build();
            EventEnvelope envelope2 = stateChangedEnvelope(targetId, FRESH_PROJECT, nonMatching);
            assertThat(record.findUpdates(envelope2)).isEmpty();
        }

        @Test
        @DisplayName("in case of event subscription")
        void eventSubscription() {
            EventId targetId = EventId
                    .newBuilder()
                    .setValue(TARGET_ID)
                    .build();
            String targetTeamId = "target-team-ID";

            Filter filter = Filters.eq("team_id.id", targetTeamId);
            CompositeFilter compositeFilter = Filters.all(filter);
            Set<CompositeFilter> filters = singleton(compositeFilter);
            Target target = composeTarget(ProjectCreated.class, singleton(targetId), filters);

            Subscription subscription = subscription(target);
            SubscriptionRecord record = newRecordFor(subscription);

            EvTeamId matchingTeamId = EvTeamId
                    .newBuilder()
                    .setId(targetTeamId)
                    .build();
            ProjectCreated matching = ProjectCreated
                    .newBuilder()
                    .setTeamId(matchingTeamId)
                    .build();
            EventEnvelope envelope = projectCreatedEnvelope(targetId, matching);
            assertThat(record.findUpdates(envelope)).isPresent();

            EvTeamId otherTeamId = EvTeamId
                    .newBuilder()
                    .setId("some-other-team-ID")
                    .build();
            ProjectCreated other = ProjectCreated
                    .newBuilder()
                    .setTeamId(otherTeamId)
                    .build();
            EventEnvelope nonMatching = projectCreatedEnvelope(targetId, other);
            assertThat(record.findUpdates(nonMatching)).isEmpty();
        }
    }

    @Test
    @DisplayName("be equal only to SubscriptionRecord that has same subscription")
    void beEqualToSame() {
        Subscription oneSubscription = subscription();
        SubscriptionId breakingId = Subscriptions.newId("breaking-id");
        Subscription otherSubscription = oneSubscription
                .toBuilder()
                .setId(breakingId)
                .build();
        @SuppressWarnings("QuestionableName")
        SubscriptionRecord one = newRecordFor(oneSubscription);
        SubscriptionRecord similar = newRecordFor(otherSubscription);
        SubscriptionRecord same = newRecordFor(oneSubscription);
        assertNotEquals(one, similar);
        assertEquals(one, same);
    }
}
