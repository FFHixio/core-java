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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.base.EventMessage;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;
import io.spine.core.Event;
import io.spine.experiment.CreditsHeld;
import io.spine.grpc.MemoizingObserver;
import io.spine.grpc.StreamObservers;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.event.EventBus;
import io.spine.server.integration.given.MemoizingProjectDetails1Repository;
import io.spine.server.integration.given.MemoizingProjectDetails2Repository;
import io.spine.server.integration.given.MemoizingProjection;
import io.spine.server.integration.given.ProjectCountAggregate;
import io.spine.server.integration.given.ProjectDetails;
import io.spine.server.integration.given.ProjectEventsSubscriber;
import io.spine.server.integration.given.ProjectStartedExtSubscriber;
import io.spine.server.integration.given.ProjectWizard;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import io.spine.testing.server.blackbox.BlackBoxBoundedContext;
import io.spine.testing.server.model.ModelTests;
import io.spine.validate.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.integration.ExternalMessageValidationError.UNSUPPORTED_EXTERNAL_MESSAGE;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithExtEntitySubscribers;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithExternalSubscribers;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithProjectCreatedNeeds;
import static io.spine.server.integration.given.IntegrationBusTestEnv.contextWithProjectStartedNeeds;
import static io.spine.server.integration.given.IntegrationBusTestEnv.newContext;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectCreated;
import static io.spine.server.integration.given.IntegrationBusTestEnv.projectStarted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"InnerClassMayBeStatic", "ClassCanBeStatic"
        /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
@DisplayName("IntegrationBus should")
class IntegrationBusTest {

    @BeforeEach
    void setUp() {
        ModelTests.dropAllModels();
        ServerEnvironment.instance().reset();
        ProjectDetails.clear();
        ProjectWizard.clear();
        ProjectCountAggregate.clear();
        MemoizingProjection.clear();
        ProjectEventsSubscriber.clear();
        ProjectStartedExtSubscriber.clear();
    }

    @AfterEach
    void tearDown() {
        ServerEnvironment.instance().reset();
        ModelTests.dropAllModels();
    }

    @Nested
    @DisplayName("dispatch events from one BC")
    class DispatchEvents {

        @Test
        @DisplayName("to entities with external subscribers of another BC")
        void toEntitiesOfBc() throws Exception {
            BoundedContext sourceContext = newContext();
            contextWithExtEntitySubscribers();

            assertNull(ProjectDetails.getExternalEvent());
            assertNull(ProjectWizard.getExternalEvent());
            assertNull(ProjectCountAggregate.getExternalEvent());

            Event event = projectCreated();
            sourceContext.eventBus()
                         .post(event);

            Message expectedMessage = AnyPacker.unpack(event.getMessage());
            assertEquals(expectedMessage, ProjectDetails.getExternalEvent());
            assertEquals(expectedMessage, ProjectWizard.getExternalEvent());
            assertEquals(expectedMessage, ProjectCountAggregate.getExternalEvent());

            sourceContext.close();
        }

        @Test
        @DisplayName("to external subscribers of another BC")
        void toBcSubscribers() throws Exception {
            BoundedContext sourceContext = newContext();
            contextWithExternalSubscribers();

            assertNull(ProjectEventsSubscriber.getExternalEvent());

            Event event = projectCreated();
            sourceContext.eventBus()
                         .post(event);
            assertEquals(AnyPacker.unpack(event.getMessage()),
                         ProjectEventsSubscriber.getExternalEvent());

            sourceContext.close();
        }

        @Test
        @DisplayName("to entities with external subscribers of multiple BCs")
        void toEntitiesOfMultipleBcs() throws Exception {
            BoundedContext sourceContext = newContext();

            BoundedContext destination1 = newContext();
            destination1.register(new MemoizingProjectDetails1Repository());

            BoundedContext destination2 = newContext();
            destination2.register(new MemoizingProjectDetails2Repository());

            assertTrue(MemoizingProjection.events()
                                          .isEmpty());
            Event event = projectCreated();
            sourceContext.eventBus()
                         .post(event);
            assertEquals(2, MemoizingProjection.events().size());
            sourceContext.close();
            destination1.close();
            destination2.close();
        }

        @SuppressWarnings("unused") // Variables declared for readability.
        @Test
        @DisplayName("to two BCs with different needs")
        void toTwoBcSubscribers() throws Exception {
            InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();

            BoundedContext sourceContext = newContext();
            BoundedContext destA = contextWithProjectCreatedNeeds();
            BoundedContext destB = contextWithProjectStartedNeeds();

            assertNull(ProjectStartedExtSubscriber.getExternalEvent());
            assertNull(ProjectEventsSubscriber.getExternalEvent());

            EventBus sourceEventBus = sourceContext.eventBus();
            Event eventA = projectCreated();
            sourceEventBus.post(eventA);
            Event eventB = projectStarted();
            sourceEventBus.post(eventB);

            assertEquals(AnyPacker.unpack(eventA.getMessage()),
                         ProjectEventsSubscriber.getExternalEvent());

            assertEquals(AnyPacker.unpack(eventB.getMessage()),
                         ProjectStartedExtSubscriber.getExternalEvent());

            sourceContext.close();
            destA.close();
            destB.close();
        }
    }

    @Nested
    @DisplayName("avoid dispatching events from one BC")
    class AvoidDispatching {

        @Test
        @DisplayName("to domestic entity subscribers of another BC")
        void toDomesticEntitySubscribers() throws Exception {
            BoundedContext sourceContext = newContext();
            BoundedContext destContext = contextWithExtEntitySubscribers();

            assertNull(ProjectDetails.getDomesticEvent());

            Event event = projectStarted();
            sourceContext.eventBus()
                         .post(event);

            assertNotEquals(AnyPacker.unpack(event.getMessage()),
                            ProjectDetails.getDomesticEvent());
            assertNull(ProjectDetails.getDomesticEvent());

            destContext.eventBus()
                       .post(event);
            assertEquals(AnyPacker.unpack(event.getMessage()), ProjectDetails.getDomesticEvent());

            sourceContext.close();
            destContext.close();
        }

        @Test
        @DisplayName("to domestic standalone subscribers of another BC")
        void toDomesticStandaloneSubscribers() throws Exception {
            BoundedContext sourceContext = newContext();
            BoundedContext destContext = contextWithExternalSubscribers();

            assertNull(ProjectEventsSubscriber.getDomesticEvent());

            Event event = projectStarted();
            sourceContext.eventBus()
                         .post(event);

            assertNotEquals(AnyPacker.unpack(event.getMessage()),
                            ProjectEventsSubscriber.getDomesticEvent());
            assertNull(ProjectEventsSubscriber.getDomesticEvent());

            destContext.eventBus()
                       .post(event);
            assertEquals(AnyPacker.unpack(event.getMessage()),
                         ProjectEventsSubscriber.getDomesticEvent());

            sourceContext.close();
            destContext.close();
        }
    }

    @Test
    @DisplayName("update local subscriptions upon repeated RequestForExternalMessages")
    void updateLocalSubscriptions() throws Exception {
        BoundedContext sourceContext = newContext();
        BoundedContext destinationCtx = newContext();

        // Prepare two external subscribers for the different events in the the `destinationCtx`.
        ProjectEventsSubscriber projectCreatedSubscriber
                = new ProjectEventsSubscriber();
        ProjectStartedExtSubscriber projectStartedSubscriber
                = new ProjectStartedExtSubscriber();

        // Before anything happens, there were no events received by those.
        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectStartedExtSubscriber.getExternalEvent());

        // Both events are prepared along with the `EventBus` of the source bounded context.
        EventBus sourceEventBus = sourceContext.eventBus();
        Event eventA = projectCreated();
        Event eventB = projectStarted();

        // Both events are emitted, `ProjectCreated` subscriber only is present.
        destinationCtx.integrationBus()
                      .register(projectCreatedSubscriber);
        sourceEventBus.post(eventA);
        sourceEventBus.post(eventB);
        // Only `ProjectCreated` should have been dispatched.
        assertEquals(AnyPacker.unpack(eventA.getMessage()),
                     ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectStartedExtSubscriber.getExternalEvent());

        // Clear before the next round starts.
        ProjectStartedExtSubscriber.clear();
        ProjectEventsSubscriber.clear();

        // Both events are emitted, No external subscribers at all.
        destinationCtx.integrationBus()
                      .unregister(projectCreatedSubscriber);
        sourceEventBus.post(eventA);
        sourceEventBus.post(eventB);
        // No events should have been dispatched.
        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertNull(ProjectStartedExtSubscriber.getExternalEvent());

        // Both events are emitted, `ProjectStarted` subscriber only is present
        destinationCtx.integrationBus()
                      .register(projectStartedSubscriber);
        sourceEventBus.post(eventA);
        sourceEventBus.post(eventB);
        // This time `ProjectStarted` event should only have been dispatched.
        assertNull(ProjectEventsSubscriber.getExternalEvent());
        assertEquals(AnyPacker.unpack(eventB.getMessage()),
                     ProjectStartedExtSubscriber.getExternalEvent());

        sourceContext.close();
        destinationCtx.close();
    }

    @Test
    @DisplayName("send messages between two contexts regardless of registration order")
    void mutual() {
        BlackBoxBoundedContext<?> photos = BlackBoxBoundedContext
                .singleTenant("Photos-" + IntegrationBusTest.class.getSimpleName());
        BlackBoxBoundedContext<?> billing = BlackBoxBoundedContext
                .singleTenant("Billing-" + IntegrationBusTest.class.getSimpleName());
        photos.receivesCommand(UploadPhotos.generate());
        assertReceived(photos, PhotosUploaded.class);
        assertReceived(billing, PhotosUploaded.class);
        assertReceived(billing, CreditsHeld.class);
        assertReceived(photos, CreditsHeld.class);
        assertReceived(photos, PhotosProcessed.class);

        photos.close();
        billing.close();
    }

    private static void assertReceived(BlackBoxBoundedContext<?> context,
                                       Class<? extends EventMessage> eventClass) {
        context.assertEvents()
               .withType(eventClass)
               .hasSize(1);
    }

    @Nested
    @DisplayName("not dispatch to domestic subscribers if they requested external")
    class NotDispatchToDomestic {

        @Test
        @DisplayName("events")
        void eventsIfNeedExternal() throws Exception {
            BoundedContext context = contextWithExtEntitySubscribers();
            ProjectEventsSubscriber eventSubscriber = new ProjectEventsSubscriber();
            EventBus eventBus = context.eventBus();
            eventBus.register(eventSubscriber);

            assertNull(ProjectEventsSubscriber.getExternalEvent());
            assertNull(ProjectDetails.getExternalEvent());
            assertNull(ProjectWizard.getExternalEvent());
            assertNull(ProjectCountAggregate.getExternalEvent());

            Event projectCreated = projectCreated();
            eventBus.post(projectCreated);

            assertNull(ProjectEventsSubscriber.getExternalEvent());
            assertNull(ProjectDetails.getExternalEvent());
            assertNull(ProjectWizard.getExternalEvent());
            assertNull(ProjectCountAggregate.getExternalEvent());

            context.close();
        }
    }

    @Test
    @DisplayName("emit unsupported external message exception if message type is unknown")
    void throwOnUnknownMessage() throws Exception {
        BoundedContext context = newContext();

        Event event = projectCreated();
        BoundedContextName name = BoundedContextNames.newName("External context ID");
        ExternalMessage externalMessage = ExternalMessages.of(event, name);
        MemoizingObserver<Ack> observer = StreamObservers.memoizingObserver();
        context.integrationBus().post(externalMessage, observer);
        Error error = observer.firstResponse()
                              .getStatus()
                              .getError();
        assertFalse(Validate.isDefault(error));
        assertEquals(ExternalMessageValidationError.getDescriptor()
                                                   .getFullName(),
                     error.getType());
        assertEquals(UNSUPPORTED_EXTERNAL_MESSAGE.getNumber(), error.getCode());

        context.close();
    }
}
