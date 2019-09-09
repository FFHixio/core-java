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

import com.google.common.collect.ImmutableSet;
import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.core.BoundedContextName;
import io.spine.core.Event;
import io.spine.protobuf.AnyPacker;
import io.spine.server.BoundedContext;
import io.spine.server.ContextAware;
import io.spine.server.ServerEnvironment;
import io.spine.server.event.EventDispatcher;
import io.spine.server.transport.ChannelId;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.PublisherHub;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.SubscriberHub;
import io.spine.server.transport.TransportFactory;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.transport.MessageChannel.channelIdFor;

/**
 * Dispatches {@linkplain ExternalMessage external messages} from and to the Bounded Context
 * in which this bus operates.
 *
 * <p>This bus is available as a part of single {@code BoundedContext}.
 * In a multi-component environment messages may travel across components from one Bounded Context
 * to another.
 *
 * <p>{@code IntegrationBus} is always based upon some {@linkplain TransportFactory transport}
 * that delivers the messages from and to it. For several Bounded Contexts to communicate,
 * their integration buses have to share the transport. Typically that would be a single
 * messaging broker.
 *
 * <p>The messages from external components received by the {@code IntegrationBus} instance
 * via the transport are propagated into the bounded context via the domestic {@code EventBus}.
 *
 * {@code IntegrationBus} is also responsible for publishing the messages
 * born within the current `BoundedContext` to external collaborators. To do that properly,
 * the bus listens to a special document message called {@linkplain RequestForExternalMessages}
 * that describes the needs of other parties.
 *
 * <p><b>Sample Usage.</b> The Bounded Context named Projects has an external event handler method
 * in the projection as follows:
 *
 * <p>Bounded context "Projects" has the external event handler method in the projection as follows:
 * <pre>
 * public class ProjectListView extends Projection ...  {
 *
 *     {@literal @}Subscribe(external = true)
 *      public void on(UserDeleted event) {
 *          // Remove the projects that belong to this user.
 *          // ...
 *      }
 *
 *      // ...
 *  }
 * </pre>
 *
 * <p>Upon a registration of the corresponding repository, the integration bus of
 * "Projects" context sends out a {@code RequestForExternalMessages} saying that an {@code Event}
 * of {@code UserDeleted} type is needed from other parties.
 *
 * <p>Let's say the second Bounded Context is "Users". Its integration bus will receive
 * the {@code RequestForExternalMessages} request sent out by "Projects". To handle it properly,
 * it will create a bridge between "Users"'s event bus (which may eventually be transmitting
 * a {@code UserDeleted} event) and an external transport.
 *
 * <p>Once {@code UserDeleted} is published locally in "Users" context, it will be received
 * by this bridge (as well as other local dispatchers) and published to the external transport.
 *
 * <p>The integration bus of "Projects" context will receive the {@code UserDeleted}
 * external message. The event will be dispatched to the external event handler of
 * {@code ProjectListView} projection.
 *
 * @implNote An {@code IntegrationBus} never performs dispatching on its own. Instead, it
 *         prepares and passes the message to the local {@code EventBus}. Hence,
 *         {@code IntegrationBus} is unicast, as it always dispatches a message to a single target,
 *         the {@code EventBus}. However, {@code IntegrationBus} is aware of the local dispatchers
 *         and the types of external events they dispatch, so that it can tell if an event should be
 *         posted to this Context or not.
 */
@SuppressWarnings("OverlyCoupledClass")
public class IntegrationBus implements ContextAware, AutoCloseable {

    private static final ChannelId CONFIG_EXCHANGE_CHANNEL_ID = channelIdFor(
            TypeUrl.of(RequestForExternalMessages.class)
    );
    private static final TypeUrl EVENT = TypeUrl.of(Event.class);

    private final SubscriberHub subscriberHub;
    private final PublisherHub publisherHub;
    private @MonotonicNonNull BusAdapter busAdapter;
    private @MonotonicNonNull BoundedContextName context;
    private @MonotonicNonNull ConfigurationChangeObserver configurationObserver;
    private @MonotonicNonNull ConfigurationBroadcast broadcast;

    public IntegrationBus() {
        TransportFactory transportFactory = ServerEnvironment
                .instance()
                .transportFactory();
        this.subscriberHub = new SubscriberHub(transportFactory);
        this.publisherHub = new PublisherHub(transportFactory);
    }

    @Override
    public void registerWith(BoundedContext context) {
        checkNotRegistered();
        BoundedContextName name = context.name();
        this.busAdapter = new BusAdapter(this, context.eventBus());
        this.context = name;
        this.configurationObserver = new ConfigurationChangeObserver(this, name, busAdapter);
        this.subscriberHub.get(CONFIG_EXCHANGE_CHANNEL_ID)
                          .addObserver(configurationObserver);
        Publisher configurationPublisher = publisherHub.get(CONFIG_EXCHANGE_CHANNEL_ID);
        this.broadcast = new ConfigurationBroadcast(name, configurationPublisher);
    }

    @Override
    public boolean isRegistered() {
        return context != null;
    }

    void publish(EventEnvelope event) {
        Event outerObject = event.outerObject();
        ExternalMessage msg = ExternalMessages.of(outerObject, context);
        EventClass eventClass = event.messageClass();
        ChannelId channelId = toChannelId(eventClass);
        boolean eventFromUpstream = subscriberHub.hasChannel(channelId);
        if (!eventFromUpstream) {
            Publisher channel = publisherHub.get(channelId);
            channel.publish(AnyPacker.pack(event.id()), msg);
        }
    }

    void dispatchLocally(Event event) {
        dispatchLocally(event, noOpObserver());
    }

    void dispatchLocally(Event event, StreamObserver<Ack> ackObserver) {
        busAdapter.dispatch(event, ackObserver);
    }

    /**
     * Registers a local dispatcher which is subscribed to {@code external} messages.
     *
     * @param dispatcher
     *         the dispatcher to register
     */
    public void register(EventDispatcher dispatcher) {
        Iterable<EventClass> receivedTypes = dispatcher.externalEventClasses();
        for (EventClass cls : receivedTypes) {
            ChannelId channelId = toChannelId(cls);
            Subscriber subscriber = subscriberHub.get(channelId);
            ExternalMessageObserver observer = observerFor(cls);
            subscriber.addObserver(observer);
            notifyTypesChanged();
        }
    }

    /**
     * Unregisters a local dispatcher which should no longer be subscribed
     * to {@code external} messages.
     *
     * @param dispatcher
     *         the dispatcher to unregister
     */
    public void unregister(EventDispatcher dispatcher) {
        Iterable<EventClass> transformed = dispatcher.externalEventClasses();
        for (EventClass cls : transformed) {
            ChannelId channelId = toChannelId(cls);
            Subscriber subscriber = subscriberHub.get(channelId);
            ExternalMessageObserver observer = observerFor(cls);
            subscriber.removeObserver(observer);
        }
        subscriberHub.closeStaleChannels();
    }

    private static ChannelId toChannelId(EventClass cls) {
        TypeUrl targetType = TypeUrl.of(cls.value());
        return channelIdFor(targetType);
    }

    private ExternalMessageObserver observerFor(EventClass externalClass) {
        ExternalMessageObserver observer =
                new ExternalMessageObserver(context, externalClass.value(), this);
        return observer;
    }

    /**
     * Notifies other Bounded Contexts that this integration bus instance now requests a different
     * set of message types.
     *
     * <p>Sends out an instance of {@linkplain RequestForExternalMessages
     * request for external messages} for that purpose.
     */
    private void notifyTypesChanged() {
        ImmutableSet<ExternalMessageType> needs = subscriberHub
                .ids()
                .stream()
                .map(channelId -> ExternalMessageType
                        .newBuilder()
                        .setMessageTypeUrl(channelId.getTargetType())
                        .setWrapperTypeUrl(EVENT.value())
                        .buildPartial())
                .collect(toImmutableSet());
        broadcast.onTypesChanged(needs);
    }

    /**
     * Notifies other Bounded Contexts of the application about the types requested by this Context.
     *
     * <p>The {@code IntegrationBus} sends a {@link RequestForExternalMessages}. The request
     * triggers other Contexts to send their requests. As the result, all the Contexts know about
     * the needs of all the Contexts.
     */
    void notifyOthers() {
        broadcast.send();
    }

    /**
     * Removes all subscriptions and closes all the underlying transport channels.
     */
    @Override
    public void close() throws Exception {
        configurationObserver.close();
        notifyTypesChanged();

        subscriberHub.close();
        publisherHub.close();
    }

    @Override
    public String toString() {
        return "Integration Bus of BoundedContext Name = " + context.getValue();
    }
}
