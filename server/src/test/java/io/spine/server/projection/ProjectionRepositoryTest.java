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

package io.spine.server.projection;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.Events;
import io.spine.core.TenantId;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.core.given.GivenEvent;
import io.spine.server.BoundedContext;
import io.spine.server.TestEventClasses;
import io.spine.server.command.TestEventFactory;
import io.spine.server.entity.RecordBasedRepository;
import io.spine.server.entity.RecordBasedRepositoryTest;
import io.spine.server.entity.given.Given;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.GivenEventMessage;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.NoOpTaskNamesRepository;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.SensoryDeprivedProjectionRepository;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.TestProjection;
import io.spine.server.projection.given.ProjectionRepositoryTestEnv.TestProjectionRepository;
import io.spine.server.storage.RecordStorage;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.Task;
import io.spine.test.projection.event.PrjProjectArchived;
import io.spine.test.projection.event.PrjProjectCreated;
import io.spine.test.projection.event.PrjProjectDeleted;
import io.spine.test.projection.event.PrjProjectStarted;
import io.spine.test.projection.event.PrjTaskAdded;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.getCurrentTime;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("ProjectionRepository should")
class ProjectionRepositoryTest
        extends RecordBasedRepositoryTest<TestProjection, ProjectId, Project> {

    private static final Any PRODUCER_ID = Identifier.pack(GivenEventMessage.ENTITY_ID);

    private BoundedContext boundedContext;

    private static TestEventFactory newEventFactory(TenantId tenantId, Any producerId) {
        TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(ProjectionRepositoryTest.class, tenantId);
        return TestEventFactory.newInstance(producerId, requestFactory);
    }

    private static Event createEvent(TenantId tenantId,
                                     Message eventMessage,
                                     Any producerId,
                                     Timestamp when) {
        Version version = Versions.increment(Versions.zero());
        return newEventFactory(tenantId, producerId).createEvent(eventMessage,
                                                                 version,
                                                                 when);
    }

    /**
     * Simulates updating TenantIndex, which occurs during command processing
     * in multi-tenant context.
     */
    private static void keepTenantIdFromEvent(BoundedContext boundedContext, Event event) {
        TenantId tenantId = event.getContext()
                                 .getCommandContext()
                                 .getActorContext()
                                 .getTenantId();
        if (boundedContext.isMultitenant()) {
            boundedContext.getTenantIndex()
                          .keep(tenantId);
        }
    }

    private ProjectionRepository<ProjectId, TestProjection, Project> repository() {
        return (ProjectionRepository<ProjectId, TestProjection, Project>) repository;
    }

    @Override
    protected RecordBasedRepository<ProjectId, TestProjection, Project> createRepository() {
        return new TestProjectionRepository();
    }

    @Override
    protected TestProjection createEntity() {
        TestProjection projection =
                Given.projectionOfClass(TestProjection.class)
                     .withId(ProjectId.newBuilder()
                                      .setId(newUuid())
                                      .build())
                     .build();
        return projection;
    }

    @Override
    protected List<TestProjection> createEntities(int count) {
        List<TestProjection> projections = Lists.newArrayList();

        for (int i = 0; i < count; i++) {
            TestProjection projection = Given.projectionOfClass(TestProjection.class)
                                             .withId(createId(i))
                                             .build();
            projections.add(projection);
        }

        return projections;
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
                .newBuilder()
                .setName(getClass().getSimpleName())
                .setMultitenant(true)
                .build();
        super.setUp();

        boundedContext.register(repository);

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
            PrjProjectStarted msg = GivenEventMessage.projectStarted();

            // Ensure no instances are present in the repository now.
            assertFalse(repository().loadAll()
                                    .hasNext());
            // And no instances of `TestProjection` processed the event message we are going to post.
            assertTrue(TestProjection.whoProcessed(msg)
                                     .isEmpty());

            // Post an event message and grab the ID of the projection, which processed it.
            checkDispatchesEvent(msg);
            Set<ProjectId> projectIds = TestProjection.whoProcessed(msg);
            assertEquals(1, projectIds.size());
            ProjectId receiverId = projectIds.iterator()
                                             .next();

            // Check that the projection item has actually been stored and now can be loaded.
            Iterator<TestProjection> allItems = repository().loadAll();
            assertTrue(allItems.hasNext());
            TestProjection storedProjection = allItems.next();
            assertFalse(allItems.hasNext());

            // Check that the stored instance has the same ID as the instance that handled the event.
            assertEquals(storedProjection.getId(), receiverId);
        }

        @Test
        @DisplayName("several events")
        void severalEvents() {
            checkDispatchesEvent(GivenEventMessage.projectCreated());
            checkDispatchesEvent(GivenEventMessage.taskAdded());
            checkDispatchesEvent(GivenEventMessage.projectStarted());
        }

        @Test
        @DisplayName("event to archived projection")
        void eventToArchived() {
            PrjProjectArchived projectArchived = GivenEventMessage.projectArchived();
            checkDispatchesEvent(projectArchived);
            ProjectId projectId = projectArchived.getProjectId();
            TestProjection projection = repository().findOrCreate(projectId);
            assertTrue(projection.isArchived());

            // Dispatch an event to the archived projection.
            checkDispatchesEvent(GivenEventMessage.taskAdded());
            projection = repository().findOrCreate(projectId);
            List<Task> addedTasks = projection.getState()
                                              .getTaskList();
            assertFalse(addedTasks.isEmpty());

            // Check that the projection was not re-created before dispatching.
            assertTrue(projection.isArchived());
        }

        @Test
        @DisplayName("event to deleted projection")
        void eventToDeleted() {
            PrjProjectDeleted projectDeleted = GivenEventMessage.projectDeleted();
            checkDispatchesEvent(projectDeleted);
            ProjectId projectId = projectDeleted.getProjectId();
            TestProjection projection = repository().findOrCreate(projectId);
            assertTrue(projection.isDeleted());

            // Dispatch an event to the deleted projection.
            checkDispatchesEvent(GivenEventMessage.taskAdded());
            projection = repository().findOrCreate(projectId);
            List<Task> addedTasks = projection.getState()
                                              .getTaskList();
            assertTrue(projection.isDeleted());

            // Check that the projection was not re-created before dispatching.
            assertFalse(addedTasks.isEmpty());
        }

        private void checkDispatchesEvent(Message eventMessage) {
            TestEventFactory eventFactory = newEventFactory(tenantId(), PRODUCER_ID);
            Event event = eventFactory.createEvent(eventMessage);

            keepTenantIdFromEvent(boundedContext, event);

            dispatchEvent(event);
            assertTrue(TestProjection.processed(eventMessage));
        }
    }

    @SuppressWarnings("CheckReturnValue") // Can ignore dispatch() result in this test.
    private void dispatchEvent(Event event) {
        repository().dispatch(EventEnvelope.of(event));
    }

    @Test
    @DisplayName("log error when dispatching unknown event")
    void logErrorOnUnknownEvent() {
        StringValue unknownEventMessage = StringValue.getDefaultInstance();

        Event event = GivenEvent.withMessage(unknownEventMessage);

        dispatchEvent(event);

        TestProjectionRepository testRepo = (TestProjectionRepository) repository();

        assertTrue(testRepo.getLastErrorEnvelope() instanceof EventEnvelope);
        assertEquals(Events.getMessage(event), testRepo.getLastErrorEnvelope()
                                                       .getMessage());
        assertEquals(event, testRepo.getLastErrorEnvelope()
                                    .getOuterObject());

        // It must be "illegal argument type" since projections of this repository
        // do not handle such events.
        assertTrue(testRepo.getLastException() instanceof IllegalArgumentException);
    }

    @Nested
    @DisplayName("return")
    class Return {

        @Test
        @DisplayName("processed event classes")
        void eventClasses() {
            Set<EventClass> eventClasses = repository().getMessageClasses();
            TestEventClasses.assertContains(eventClasses,
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
        Timestamp timestamp = getCurrentTime();
        assertEquals(timestamp, ProjectionRepository.nullToDefault(timestamp));
        assertEquals(Timestamp.getDefaultInstance(), ProjectionRepository.nullToDefault(null));
    }

    @SuppressWarnings("CheckReturnValue") // can ignore dispatch() result in this test
    @Test
    @DisplayName("not create record if entity is not updated")
    void notCreateRecordForUnchanged() {
        NoOpTaskNamesRepository repo = new NoOpTaskNamesRepository();
        boundedContext.register(repo);

        assertFalse(repo.loadAll()
                        .hasNext());

        Event event = createEvent(tenantId(),
                                  GivenEventMessage.projectCreated(),
                                  PRODUCER_ID,
                                  getCurrentTime());
        repo.dispatch(EventEnvelope.of(event));

        Iterator<?> items = repo.loadAll();
        assertFalse(items.hasNext());
    }

    /**
     * Ensures that {@link ProjectionRepository#readLastHandledEventTime()} and
     * {@link ProjectionRepository#writeLastHandledEventTime(Timestamp)} which are used by
     * Beam-based catch-up are exposed.
     */
    @Test
    @DisplayName("expose read and write methods for last handled event timestamp")
    void getSetLastHandled() {
        assertNotNull(repository().readLastHandledEventTime());
        repository().writeLastHandledEventTime(Time.getCurrentTime());
    }

    /**
     * Ensures that {@link ProjectionRepository#createStreamQuery()}, which is used by the catch-up
     * procedures is exposed.
     */
    @Test
    @DisplayName("create stream query")
    void createStreamQuery() {
        assertNotNull(repository().createStreamQuery());
    }

    @Nested
    @DisplayName("provide package-private access to")
    class ExposeToPackage {

        /**
         * Ensures that {@link ProjectionRepository#getEventStore()} which is used by the catch-up
         * functionality is exposed to the package.
         */
        @Test
        @DisplayName("event store")
        void eventStore() {
            assertNotNull(repository().getEventStore());
        }

        /**
         * Ensures that {@link ProjectionRepository#boundedContext()} which is used by the catch-up
         * functionality is exposed to the package.
         */
        @Test
        @DisplayName("bounded context")
        void boundedContext() {
            assertNotNull(repository().boundedContext());
        }
    }

    @Test
    @DisplayName("throw exception on attempt to register in BC with no messages handled")
    void notRegisterIfNothingHandled() {
        SensoryDeprivedProjectionRepository repo = new SensoryDeprivedProjectionRepository();
        BoundedContext boundedContext = BoundedContext
                .newBuilder()
                .setMultitenant(false)
                .build();
        repo.setBoundedContext(boundedContext);

        assertThrows(IllegalStateException.class, repo::onRegistered);
    }
}
