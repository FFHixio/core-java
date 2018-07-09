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

package io.spine.server;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.core.Ack;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.server.bus.BusFilter;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.type.KnownTypes;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The implementation base for a message handler test.
 *
 * <p>A derived class initializes the test environment by providing the entity to test and
 * the message to dispatch to that entity.
 *
 * <p>Test assertions are performed via an {@link AbstractExpected Expected} instance. A typical
 * test case dispatches the message to the entity via {@link #expectThat(Entity)} and validates
 * the message handling result.
 *
 * @author Dmytro Dashenkov
 * @see CommandHandlerTest
 * @see ReactionTest
 */
public abstract class MessageHandlerTest<M extends Message,
                                         I,
                                         S extends Message,
                                         E extends Entity<I, S>,
                                         X extends AbstractExpected<S, X>> {

    protected static final String BE_REJECTED_TEST_NAME = "be rejected";
    protected static final String CHANGE_STATE_TEST_NAME = "change a state of the entity";

    private I id;
    private @Nullable M message;
    private @Nullable Repository<I, E> entityRepository;

    /**
     * List of the commands sent to the bus during the test.
     */
    private final List<Message> interceptedCommands = newArrayList();

    /**
     * A bounded context used for testing. Test methods don't share the bounded context, it gets
     * recreated after each and every test method.
     */
    private BoundedContext boundedContext;

    /**
     * Creates and stores the reference to the command message being tested.
     */
    @BeforeEach
    @OverridingMethodsMustInvokeSuper
    protected void setUp() {
        id = newId();
        storeMessage(createMessage());
    }

    /**
     * Creates a new ID of the tested entity.
     *
     * @return new ID
     */
    protected abstract I newId();

    /**
     * Creates a new message to test.
     *
     * <p>This message is then dispatched to the entity.
     *
     * @return a new message to test
     */
    protected abstract M createMessage();

    /**
     * Dispatches the {@linkplain #message() message} to the given entity.
     *
     * @param entity the message receiver
     * @return a list of produced events if applicable, or an empty list otherwise
     */
    protected abstract List<? extends Message> dispatchTo(E entity);

    /**
     * Returns a {@link Repository} instance for the entity being tested in order
     * to register it in {@link TestBoundedContext}.
     *
     * @return instance of {@link Repository}
     */
    protected abstract Repository<I, E> createEntityRepository();

    /**
     * Retrieves the ID of the tested entity.
     */
    protected final I id() {
        return id;
    }

    /**
     * Retrieves the message dispatched to the entity.
     *
     * <p>By default, this message is created by {@link #createMessage()}. Call
     * {@link #storeMessage(Message)} to override.
     *
     * @return the message to handle
     */
    protected final M message() {
        return message;
    }

    /**
     * Overrides the handled message with the given one.
     *
     * @param message the new message to handle
     */
    protected void storeMessage(M message) {
        this.message = message;
    }

    /**
     * Returns instance of {@link BoundedContext} which is being used in this test suite.
     *
     * @return {@link BoundedContext} instance
     */
    protected BoundedContext boundedContext() {
        assertNotNull(boundedContext);
        return boundedContext;
    }

    @BeforeEach
    protected final void configureBoundedContext() {
        boundedContext = TestBoundedContext.create(new MemoizingBusFilter());
        entityRepository = createEntityRepository();
        assertNotNull(entityRepository);

        Set<CommandClass> commandClasses = getAllCommandClasses();
        CommandBus commandBus = boundedContext().getCommandBus();
        commandBus.register(new VoidCommandDispatcher<>(commandClasses));
    }

    private static Set<CommandClass> getAllCommandClasses() {
        return KnownTypes
                .instance()
                .getAllUrls()
                .stream()
                .filter(typeUrl -> commandOfType(typeUrl).isPresent())
                .map(typeUrl -> commandOfType(typeUrl).get())
                .collect(toSet());
    }

    private static Optional<CommandClass> commandOfType(TypeUrl type) {
        Class<?> cls = type.getJavaClass();
        if (Message.class.isAssignableFrom(cls)) {
            @SuppressWarnings("unchecked")
            Class<? extends Message> messageType = (Class<? extends Message>) cls;
            CommandClass commandClass = CommandClass.of(messageType);
            return Optional.of(commandClass);
        } else {
            return Optional.absent();
        }
    }

    /**
     * Resets the state of the test case, so test methods can't share it.
     */
    @AfterEach
    protected void resetTestCase() {
        message = null;
        entityRepository = null;
        interceptedCommands.clear();
        if (boundedContext != null) {
            try {
                boundedContext.close();
            } catch (Exception e) {
                throw illegalStateWithCauseOf(e);
            }
        }
    }

    ImmutableList<Message> interceptedCommands() {
        return copyOf(interceptedCommands);
    }

    /**
     * Dispatches the {@linkplain #message() message} to the given {@code entity} and returns
     * the message handling result validator.
     *
     * @param entity the entity which handles the tested message
     * @return new message handling result validator
     */
    protected abstract X expectThat(E entity);

    /**
     * A command dispatcher to dispatch commands into nothing.
     *
     * @param <I> the type of entity ID.
     */
    private static class VoidCommandDispatcher<I> implements CommandDispatcher<I> {

        private final Set<CommandClass> expectedCommands;

        private VoidCommandDispatcher(Set<CommandClass> expectedCommands) {
            this.expectedCommands = newHashSet(expectedCommands);
        }

        @Override
        public Set<CommandClass> getMessageClasses() {
            return newHashSet(expectedCommands);
        }

        @Override
        public I dispatch(CommandEnvelope envelope) {
            return null;
        }

        @Override
        public void onError(CommandEnvelope envelope, RuntimeException exception) {
            log().info("Error while dispatching a command during the unit test");
        }
    }

    /**
     * The bus filter that remembers all commands posted to the command bus.
     */
    private class MemoizingBusFilter implements BusFilter<CommandEnvelope> {

        @Override
        public Optional<Ack> accept(CommandEnvelope envelope) {
            interceptedCommands.add(unpack(envelope.getCommand()
                                                   .getMessage()));
            return Optional.absent();
        }

        @Override
        public void close() {
            // NoOp.
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(MessageHandlerTest.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
