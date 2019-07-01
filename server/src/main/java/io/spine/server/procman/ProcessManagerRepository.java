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

package io.spine.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.command.CommandErrorHandler;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcherDelegate;
import io.spine.server.commandbus.DelegatingCommandDispatcher;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.Inbox;
import io.spine.server.delivery.InboxLabel;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.entity.EntityLifecycleMonitor;
import io.spine.server.entity.EventDispatchingRepository;
import io.spine.server.entity.TransactionListener;
import io.spine.server.event.EventBus;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.ExternalMessageDispatcher;
import io.spine.server.integration.ExternalMessageEnvelope;
import io.spine.server.procman.model.ProcessManagerClass;
import io.spine.server.route.CommandRouting;
import io.spine.server.route.EventRoute;
import io.spine.server.route.EventRouting;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Suppliers.memoize;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.option.EntityOption.Kind.PROCESS_MANAGER;
import static io.spine.server.procman.model.ProcessManagerClass.asProcessManagerClass;
import static io.spine.server.tenant.TenantAwareRunner.with;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The abstract base for Process Managers repositories.
 *
 * @param <I>
 *         the type of IDs of process managers
 * @param <P>
 *         the type of process managers
 * @param <S>
 *         the type of process manager state messages
 * @see ProcessManager
 */
public abstract class ProcessManagerRepository<I,
                                               P extends ProcessManager<I, S, ?>,
                                               S extends Message>
                extends EventDispatchingRepository<I, P, S>
                implements CommandDispatcherDelegate<I> {

    /** The command routing schema used by this repository. */
    private final Supplier<CommandRouting<I>> commandRouting;

    /**
     * The {@link CommandErrorHandler} tackling the dispatching errors.
     *
     * <p>This field is not {@code final} only because it is initialized in {@link #onRegistered()}
     * method.
     */
    private @MonotonicNonNull CommandErrorHandler commandErrorHandler;

    /**
     * The {@link Inbox} for the messages, which are sent to the instances managed by this
     * repository.
     */
    private @MonotonicNonNull Inbox<I> inbox;

    /**
     * The configurable lifecycle rules of the repository.
     *
     * <p>The rules allow to automatically mark entities as archived/deleted upon certain event and
     * rejection types emitted.
     *
     * @see LifecycleRules#archiveOn(Class[])
     * @see LifecycleRules#deleteOn(Class[])
     */
    private final LifecycleRules lifecycleRules = new LifecycleRules();

    protected ProcessManagerRepository() {
        super();
        this.commandRouting = memoize(() -> CommandRouting.newInstance(idClass()));
    }

    /**
     * Obtains class information of process managers managed by this repository.
     */
    private ProcessManagerClass<P> processManagerClass() {
        return (ProcessManagerClass<P>) entityModelClass();
    }

    @Internal
    @Override
    protected final ProcessManagerClass<P> toModelClass(Class<P> cls) {
        return asProcessManagerClass(cls);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Customizes event routing to use first message field.
     *
     * <p>Registers with the {@code CommandBus} for dispatching commands
     * (via {@linkplain DelegatingCommandDispatcher delegating dispatcher}).
     *
     * <p>Registers with the {@code IntegrationBus} for dispatching external events and rejections.
     *
     * <p>Ensures there is at least one handler method declared by the class of the managed
     * process manager:
     *
     * <ul>
     *     <li>command handler methods;
     *     <li>domestic or external event reactor methods;
     *     <li>domestic or external rejection reactor methods;
     *     <li>commanding method.
     * </ul>
     *
     * <p>Throws an {@code IllegalStateException} otherwise.
     * @param context
     *         the Bounded Context of this repository
     * @throws IllegalStateException
     *          if the Process Manager class of this repository does not declare message
     *          handling methods
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    protected void init(BoundedContext context) {
        super.init(context);

        setupCommandRouting(commandRouting());
        checkNotDeaf();

        context.registerCommandDispatcher(this);

        this.commandErrorHandler = context.createCommandErrorHandler();

        initInbox();
    }

    /**
     * Initializes the {@code Inbox}.
     */
    private void initInbox() {
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        inbox = delivery
                .<I>newInbox(entityStateType())
                .addEventEndpoint(InboxLabel.REACT_UPON_EVENT,
                                  e -> PmEventEndpoint.of(this, e))
                .addCommandEndpoint(InboxLabel.HANDLE_COMMAND,
                                    c -> PmCommandEndpoint.of(this, c))
                .build();
    }

    /**
     * Replaces default routing with the one which takes the target ID from the first field
     * of an event message.
     *
     * @param routing
     *          the routing to customize
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    protected void setupEventRouting(EventRouting<I> routing) {
        super.setupEventRouting(routing);
        routing.replaceDefault(EventRoute.byFirstMessageField(idClass()));
    }

    /**
     * A callback for derived classes to customize routing schema for commands.
     *
     * <p>Default routing returns the value of the first field of a command message.
     *
     * @param routing
     *         the routing schema to customize
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // See Javadoc
    protected void setupCommandRouting(CommandRouting<I> routing) {
        // Do nothing.
    }

    /**
     * Ensures the process manager class handles at least one type of messages.
     */
    private void checkNotDeaf() {
        boolean dispatchesEvents = dispatchesEvents() || dispatchesExternalEvents();

        if (!dispatchesCommands() && !dispatchesEvents) {
            throw newIllegalStateException(
                    "Process managers of the repository %s have no command handlers, " +
                            "and do not react to any events.", this);
        }
    }

    /**
     * Obtains a set of event classes to which process managers of this repository react.
     *
     * @return a set of event classes or empty set if process managers do not react to
     *         domestic events
     */
    @Override
    public Set<EventClass> messageClasses() {
        return processManagerClass().domesticEvents();
    }

    /**
     * Obtains classes of external events to which the process managers managed by this repository
     * react.
     *
     * @return a set of event classes or an empty set if process managers do not react to
     *         external events
     */
    @Override
    public Set<EventClass> externalEventClasses() {
        return processManagerClass().externalEvents();
    }

    /**
     * Obtains a set of classes of commands handled by process managers of this repository.
     *
     * @return a set of command classes or empty set if process managers do not handle commands
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField") // it is immutable
    public Set<CommandClass> commandClasses() {
        return processManagerClass().commands();
    }

    /**
     * Obtains command routing schema used by this repository.
     */
    private CommandRouting<I> commandRouting() {
        return commandRouting.get();
    }

    /**
     * Obtains configurable lifecycle rules of this repository.
     *
     * <p>The rules allow to automatically archive/delete entities upon certain event and rejection
     * types produced.
     *
     * <p>The rules can be set as follows:
     * <pre>{@code
     *   repository.lifecycle()
     *             .archiveOn(Event1.class, Rejection1.class)
     *             .deleteOn(Rejection2.class)
     * }</pre>
     */
    public final LifecycleRules lifecycle() {
        return lifecycleRules;
    }

    @Override
    public ImmutableSet<EventClass> outgoingEvents() {
        Set<EventClass> eventClasses = processManagerClass().outgoingEvents();
        return ImmutableSet.copyOf(eventClasses);
    }

    /**
     * Dispatches the command to a corresponding process manager.
     *
     * <p>If there is no stored process manager with such an ID,
     * a new process manager is created and stored after it handles the passed command.
     *
     * @param command a request to dispatch
     */
    @Override
    public I dispatchCommand(CommandEnvelope command) {
        checkNotNull(command);
        I id = route(command);
        inbox.send(command)
             .toHandler(id);
        return id;
    }

    private I route(CommandEnvelope cmd) {
        CommandRouting<I> routing = commandRouting();
        I target = routing.apply(cmd.message(), cmd.context());

        // We need to have a tenant set in order the callbacks could post the system events.
        with(cmd.tenantId())
                .run(() -> lifecycleOf(target).onTargetAssignedToCommand(cmd.id()));
        return target;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends the given event to the {@code Inbox} of this repository.
     */
    @Override
    protected final void dispatchTo(I id, Event event) {
        inbox.send(EventEnvelope.of(event)).toReactor(id);
    }

    @Override
    public void onError(CommandEnvelope cmd, RuntimeException exception) {
        commandErrorHandler.handle(cmd, exception, event -> postEvents(ImmutableList.of(event)));
    }

    @SuppressWarnings("unchecked")   // to avoid massive generic-related issues.
    @VisibleForTesting
    protected PmTransaction<?, ?, ?> beginTransactionFor(P manager) {
        PmTransaction<I, S, ?> tx =
                PmTransaction.start((ProcessManager<I, S, ?>) manager, lifecycle());
        TransactionListener listener = EntityLifecycleMonitor.newInstance(this, manager.id());
        tx.setListener(listener);
        return tx;
    }

    /**
     * Posts passed events to {@link EventBus}.
     */
    void postEvents(Collection<Event> events) {
        Iterable<Event> filteredEvents = eventFilter().filter(events);
        EventBus bus = context().eventBus();
        bus.post(filteredEvents);
    }

    /**
     * Posts the passed event to {@link EventBus}.
     */
    void postEvent(Event event) {
        postEvents(ImmutableList.of(event));
    }

    /**
     * Posts passed commands to {@link CommandBus}.
     */
    void postCommands(Collection<Command> commands) {
        CommandBus bus = context().commandBus();
        bus.post(commands, noOpObserver());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overridden to expose the method into current package.
     */
    @Override
    protected EntityLifecycle lifecycleOf(I id) {
        return super.lifecycleOf(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose the method to the package.
     */
    @Override
    protected P findOrCreate(I id) {
        return super.findOrCreate(id);
    }

    @Override
    public P create(I id) {
        P procman = super.create(id);
        lifecycleOf(id).onEntityCreated(PROCESS_MANAGER);
        return procman;
    }

    @Override
    public Optional<ExternalMessageDispatcher<I>> createExternalDispatcher() {
        if (!dispatchesExternalEvents()) {
            return Optional.empty();
        }
        return Optional.of(new PmExternalEventDispatcher());
    }

    @OverridingMethodsMustInvokeSuper
    @Override
    public void close() {
        super.close();
        if(inbox != null) {
            inbox.unregister();
        }
    }

    /**
     * An implementation of an external message dispatcher feeding external events
     * to {@code ProcessManager} instances.
     */
    private class PmExternalEventDispatcher extends AbstractExternalEventDispatcher {

        @Override
        public Set<ExternalMessageClass> messageClasses() {
            ProcessManagerClass<?> pmClass = asProcessManagerClass(entityClass());
            Set<EventClass> eventClasses = pmClass.externalEvents();
            return ExternalMessageClass.fromEventClasses(eventClasses);
        }

        @Override
        public void onError(ExternalMessageEnvelope envelope, RuntimeException exception) {
            checkNotNull(envelope);
            checkNotNull(exception);
            logError("Error dispatching external event (class: `%s`, id: `%s`) " +
                             "to a process manager with state `%s`.",
                     envelope, exception);
        }
    }
}
