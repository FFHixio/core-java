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

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.base.EventMessage;
import io.spine.client.ResponseFormat;
import io.spine.core.Event;
import io.spine.core.MessageId;
import io.spine.core.TenantId;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryTest;
import io.spine.server.entity.given.Given;
import io.spine.server.projection.given.EntitySubscriberProjection;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.GivenEventMessage;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.NoOpTaskNamesRepository;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.SensoryDeprivedProjectionRepository;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.TestProjectionRepository;
import io.spine.server.projection.given.TestProjection;
import io.spine.server.storage.RecordStorage;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.server.type.given.GivenEvent;
import io.spine.system.server.CannotDispatchDuplicateEvent;
import io.spine.system.server.DiagnosticMonitor;
import io.spine.system.server.RoutingFailed;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.ProjectTaskNames;
import io.spine.test.projection.Task;
import io.spine.test.projection.event.PrjProjectArchived;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjProjectDeleted;
import io.spine.test.projection.event.PrjProjectStarted;
import io.spine.test.projection.event.PrjTaskAdded;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.TestEventFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.pack;
import static io.spine.base.Time.currentTime;
import static io.spine.server.projection.ProjectionRepository.nullToDefault;
import static io.spine.server.projection.given.ProjectionRepositoryTestEnv.GivenEventMessage.projectCreated;
import static io.spine.server.projection.given.ProjectionRepositoryTestEnv.dispatchedEventId;
import static io.spine.server.projection.given.dispatch.ProjectionEventDispatcher.dispatch;
import static io.spine.testing.TestValues.randomString;
import static io.spine.testing.server.Assertions.assertEventClasses;
import static io.spine.testing.server.TestEventFactory.newInstance;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProjectionRepository should")
class ProjectionRepositoryTest
        extends RecordBasedRepositoryTest<TestProjection, ProjectId, Project> {

    private static final Any PRODUCER_ID = pack(GivenEventMessage.ENTITY_ID);

    private BoundedContext boundedContext;

    private static TestEventFactory newEventFactory(TenantId tenantId, Any producerId) {
        TestActorRequestFactory requestFactory =
                new TestActorRequestFactory(ProjectionRepositoryTest.class, tenantId);
        return newInstance(producerId, requestFactory);
    }

    private static Event createEvent(TenantId tenantId, EventMessage eventMessage, Timestamp when) {
        Version version = Versions.increment(Versions.zero());
        return newEventFactory(tenantId, PRODUCER_ID).createEvent(eventMessage, version, when);
    }

    /**
     * Simulates updating TenantIndex, which occurs during command processing
     * in multi-tenant context.
     */
    private static void keepTenantIdFromEvent(BoundedContext boundedContext, Event event) {
        TenantId tenantId = event.tenant();
        if (boundedContext.isMultitenant()) {
            boundedContext.tenantIndex()
                          .keep(tenantId);
        }
    }

    @Override
    protected TestProjectionRepository repository() {
        return (TestProjectionRepository) super.repository();
    }

    @Override
    protected RecordBasedRepository<ProjectId, TestProjection, Project> createRepository() {
        TestProjectionRepository repository = new TestProjectionRepository();
        repository.registerWith(boundedContext);
        return repository;
    }

    @Override
    protected TestProjection createEntity(ProjectId id) {
        Project project = Project
                .newBuilder()
                .setId(id)
                .build();
        TestProjection projection =
                Given.projectionOfClass(TestProjection.class)
                     .withId(id)
                     .withState(project)
                     .build();
        return projection;
    }

    @Override
    protected List<TestProjection> createEntities(int count) {
        List<TestProjection> projections = Lists.newArrayList();

        for (int i = 0; i < count; i++) {
            ProjectId id = createId(i);
            TestProjection projection =
                    Given.projectionOfClass(TestProjection.class)
                         .withId(id)
                         .withState(Project.newBuilder()
                                           .setId(id)
                                           .setName("Test name " + randomString())
                                           .build())
                         .build();
            projections.add(projection);
        }

        return projections;
    }

    @Override
    protected List<TestProjection> createNamed(int count, Supplier<String> nameSupplier) {
        List<TestProjection> projections = Lists.newArrayList();

        for (int i = 0; i < count; i++) {
            ProjectId id = createId(i);
            TestProjection projection = Given.projectionOfClass(TestProjection.class)
                                             .withId(id)
                                             .withState(newProject(id, nameSupplier.get()))
                                             .build();
            projections.add(projection);
        }

        return projections;
    }

    @Override
    protected List<TestProjection> orderedByName(List<TestProjection> entities) {
        return entities.stream()
                       .sorted(comparing(ProjectionRepositoryTest::entityName))
                       .collect(toList());
    }

    private static String entityName(TestProjection entity) {
        return entity.state()
                     .getName();
    }

    private static Project newProject(ProjectId id, String name) {
        return Project.newBuilder()
                      .setId(id)
                      .setName(name)
                      .build();
    }

    @Override
    protected ProjectId createId(int i) {
        return ProjectId.newBuilder()
                        .setId(format("test-projection-%s", i))
                        .build();
    }

    @Override
    @BeforeEach
    protected void setUp() {
        boundedContext = BoundedContext
                .multitenant(getClass().getSimpleName())
                .build();
        super.setUp();
        boundedContext.register(this.repository());
        TestProjection.clearMessageDeliveryHistory();
    }

    /**
     * Closes the BoundedContest and shuts down the gRPC service.
     *
     * <p>The {@link #tearDown()} method of the super class will be invoked by JUnit automatically
     * after calling this method.
     */
    @AfterEach
    void shutDown() throws Exception {
        boundedContext.close();
    }

    /*
     * Tests
     ************/

    @Nested
    @DisplayName("dispatch")
    class Dispatch {

        @Test
        @DisplayName("event")
        void event() {
            PrjProjectCreated msg = projectCreated();

            // Ensure no instances are present in the repository now.
            assertFalse(repository().loadAll(ResponseFormat.getDefaultInstance())
                                    .hasNext());
            // And no instances of `TestProjection` processed the event message we are posting.
            assertTrue(TestProjection.whoProcessed(msg)
                                     .isEmpty());

            // Post an event message and grab the ID of the projection, which processed it.
            checkDispatchesEvent(msg);
            Set<ProjectId> projectIds = TestProjection.whoProcessed(msg);
            assertEquals(1, projectIds.size());
            ProjectId receiverId = projectIds.iterator()
                                             .next();

            // Check that the projection item has actually been stored and now can be loaded.
            Iterator<TestProjection> allItems =
                    repository().loadAll(ResponseFormat.getDefaultInstance());
            assertTrue(allItems.hasNext());
            TestProjection storedProjection = allItems.next();
            assertFalse(allItems.hasNext());

            // Check that the stored instance has the same ID as the instance handling the event.
            assertEquals(storedProjection.id(), receiverId);
        }

        @Test
        @DisplayName("several events")
        void severalEvents() {
            checkDispatchesEvent(projectCreated());
            checkDispatchesEvent(GivenEventMessage.taskAdded());
            checkDispatchesEvent(GivenEventMessage.projectStarted());
        }

        @Test
        @DisplayName("event to archived projection")
        void eventToArchived() {
            checkDispatchesEvent(projectCreated());
            PrjProjectArchived projectArchived = GivenEventMessage.projectArchived();
            checkDispatchesEvent(projectArchived);
            ProjectId projectId = projectArchived.getProjectId();
            TestProjection projection = repository().findOrCreate(projectId);
            assertTrue(projection.isArchived());

            // Dispatch an event to the archived projection.
            checkDispatchesEvent(GivenEventMessage.taskAdded());
            projection = repository().findOrCreate(projectId);
            List<Task> addedTasks = projection.state()
                                              .getTaskList();
            assertFalse(addedTasks.isEmpty());

            // Check that the projection was not re-created before dispatching.
            assertTrue(projection.isArchived());
        }

        @Test
        @DisplayName("event to deleted projection")
        void eventToDeleted() {
            checkDispatchesEvent(projectCreated());
            PrjProjectDeleted projectDeleted = GivenEventMessage.projectDeleted();
            checkDispatchesEvent(projectDeleted);
            ProjectId projectId = projectDeleted.getProjectId();
            TestProjection projection = repository().findOrCreate(projectId);
            assertTrue(projection.isDeleted());

            // Dispatch an event to the deleted projection.
            checkDispatchesEvent(GivenEventMessage.taskAdded());
            projection = repository().findOrCreate(projectId);
            List<Task> addedTasks = projection.state()
                                              .getTaskList();
            assertTrue(projection.isDeleted());

            // Check that the projection was not re-created before dispatching.
            assertFalse(addedTasks.isEmpty());
        }

        @SuppressWarnings("OverlyCoupledMethod")
            // A complex test case with many test domain messages.
        @Test
        @DisplayName("entity state update")
        void entityState() throws Exception {
            PrjProjectCreated projectCreated = GivenEventMessage.projectCreated();
            PrjTaskAdded taskAdded = GivenEventMessage.taskAdded();
            ProjectId id = projectCreated.getProjectId();
            TestEventFactory eventFactory = newInstance(id, ProjectionRepositoryTest.class);
            TestProjection project = new TestProjection(id);
            dispatch(project, eventFactory.createEvent(projectCreated));
            Any oldState = pack(project.state());
            dispatch(project, eventFactory.createEvent(taskAdded));
            Any newState = pack(project.state());
            MessageId entityId = MessageId
                    .newBuilder()
                    .setTypeUrl(newState.getTypeUrl())
                    .setId(pack(id))
                    .vBuild();
            EntityStateChanged changedEvent = EntityStateChanged
                    .newBuilder()
                    .setEntity(entityId)
                    .setWhen(currentTime())
                    .setOldState(oldState)
                    .setNewState(newState)
                    .addSignalId(dispatchedEventId())
                    .build();
            EntitySubscriberProjection.Repository repository =
                    new EntitySubscriberProjection.Repository();
            BoundedContext context = BoundedContextBuilder.assumingTests().build();
            context.register(repository);
            EventEnvelope envelope = EventEnvelope.of(eventFactory.createEvent(changedEvent));
            repository.dispatch(envelope);
            ProjectTaskNames expectedValue = ProjectTaskNames
                    .newBuilder()
                    .setProjectId(id)
                    .setProjectName(projectCreated.getName())
                    .addTaskName(taskAdded.getTask().getTitle())
                    .build();
            Optional<EntitySubscriberProjection> projection = repository.find(id);
            assertTrue(projection.isPresent());
            assertEquals(expectedValue, projection.get().state());

            context.close();
        }

        private void checkDispatchesEvent(EventMessage eventMessage) {
            TestEventFactory eventFactory = newEventFactory(tenantId(), PRODUCER_ID);
            Event event = eventFactory.createEvent(eventMessage);

            keepTenantIdFromEvent(boundedContext, event);

            dispatchEvent(event);
            assertTrue(TestProjection.processed(eventMessage));
        }
    }

    @Nested
    @DisplayName("not allow duplicate")
    @MuteLogging
    class AvoidDuplicates {

        private DiagnosticMonitor monitor;

        @BeforeEach
        void setUp() {
            monitor = new DiagnosticMonitor();
            boundedContext.registerEventDispatcher(monitor);
        }

        @Test
        @DisplayName("events")
        void events() {
            PrjProjectCreated msg = projectCreated();
            TestEventFactory eventFactory = newEventFactory(tenantId(), pack(msg.getProjectId()));
            Event event = eventFactory.createEvent(msg);

            dispatchSuccessfully(event);
            dispatchDuplicate(event);
        }

        @Test
        @DisplayName("different events with same ID")
        void differentEventsWithSameId() {
            PrjProjectCreated created = projectCreated();
            PrjProjectArchived archived = GivenEventMessage.projectArchived();
            ProjectId id = created.getProjectId();
            TestEventFactory eventFactory = newEventFactory(tenantId(), pack(id));

            Event firstEvent = eventFactory.createEvent(created);
            Event secondEvent = eventFactory.createEvent(archived)
                                            .toBuilder()
                                            .setId(firstEvent.getId())
                                            .build();
            dispatchSuccessfully(firstEvent);
            dispatchDuplicate(secondEvent);
        }

        private void dispatchSuccessfully(Event event) {
            dispatchEvent(event);
            assertTrue(TestProjection.processed(event.enclosedMessage()));
            List<CannotDispatchDuplicateEvent> events = monitor.duplicateEventEvents();
            assertThat(events).isEmpty();
        }

        private void dispatchDuplicate(Event event) {
            dispatchEvent(event);
            List<CannotDispatchDuplicateEvent> events = monitor.duplicateEventEvents();
            assertThat(events).hasSize(1);
            CannotDispatchDuplicateEvent systemEvent = events.get(0);
            assertThat(systemEvent.getDuplicateEvent())
                    .isEqualTo(event.messageId());
        }
    }

    @SuppressWarnings("CheckReturnValue") // Can ignore dispatch() result in this test.
    private void dispatchEvent(Event event) {
        repository().dispatch(EventEnvelope.of(event));
    }

    @Test
    @DisplayName("emit `RoutingFailed` when dispatching unknown event")
    void emitRoutingFailedOnUnknownEvent() {
        Event event = GivenEvent.arbitrary();
        DiagnosticMonitor monitor = new DiagnosticMonitor();
        boundedContext.registerEventDispatcher(monitor);

        dispatchEvent(event);

        List<RoutingFailed> failures = monitor.routingFailures();
        assertThat(failures.size()).isEqualTo(1);
        RoutingFailed failure = failures.get(0);
        assertThat(failure.getError().getMessage()).contains(repository().idClass().getName());
    }

    @Nested
    @DisplayName("return")
    class Return {

        @Test
        @DisplayName("processed event classes")
        void eventClasses() {
            Set<EventClass> eventClasses = repository().messageClasses();
            assertEventClasses(eventClasses,
                               PrjProjectCreated.class,
                               PrjTaskAdded.class,
                               PrjProjectStarted.class);
        }

        @Test
        @DisplayName("entity storage")
        void entityStorage() {
            RecordStorage<ProjectId> recordStorage = repository().recordStorage();
            assertNotNull(recordStorage);
        }
    }

    @Test
    @DisplayName("convert null timestamp to default")
    void convertNullTimestamp() {
        Timestamp timestamp = currentTime();
        assertThat(nullToDefault(timestamp)).isEqualTo(timestamp);
        assertThat(nullToDefault(null)).isEqualTo(Timestamp.getDefaultInstance());
    }

    @SuppressWarnings("CheckReturnValue") // can ignore dispatch() result in this test
    @Test
    @DisplayName("not create record if entity is not updated")
    void notCreateRecordForUnchanged() {
        NoOpTaskNamesRepository repo = new NoOpTaskNamesRepository();
        boundedContext.register(repo);

        assertFalse(repo.loadAll(ResponseFormat.getDefaultInstance())
                        .hasNext());

        Event event = createEvent(tenantId(), projectCreated(), currentTime());
        repo.dispatch(EventEnvelope.of(event));

        Iterator<?> items = repo.loadAll(ResponseFormat.getDefaultInstance());
        assertFalse(items.hasNext());
    }

    @Nested
    @DisplayName("provide package-private access to")
    class ExposeToPackage {

        /**
         * Ensures that {@link ProjectionRepository#eventStore()} which is used by the catch-up
         * functionality is exposed to the package.
         */
        @Test
        @DisplayName("event store")
        void eventStore() {
            ProjectionRepository<?, ?, ?> repository = repository();
            assertNotNull(repository.eventStore());
        }
    }

    @Test
    @DisplayName("check that its `Projection` class is subscribed to at least one message")
    void notRegisterIfSubscribedToNothing() {
        SensoryDeprivedProjectionRepository repo = new SensoryDeprivedProjectionRepository();
        BoundedContext context = BoundedContextBuilder
                .assumingTests()
                .build();

        assertThrows(IllegalStateException.class, () ->
                repo.registerWith(context));
    }
}
