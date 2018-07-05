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

import com.google.protobuf.Message;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Assertions for command handler invocation results.
 *
 * @author Dmytro Dashenkov
 */
public class CommandExpected<S extends Message> extends MessageProducingExpected<S> {

    @Nullable
    private final Message rejection;

    CommandExpected(List<? extends Message> events,
                    @Nullable Message rejection,
                    S initialState,
                    S state,
                    List<Message> interceptedCommands) {
        super(events, initialState, state, interceptedCommands);
        this.rejection = rejection;
    }

    @Override
    public MessageProducingExpected<S> ignoresMessage() {
        assertNull(rejection, "Message caused a rejection.");
        return super.ignoresMessage();
    }

    @Override
    public <M extends Message> MessageProducingExpected<S> producesEvent(Class<M> eventClass,
                                                                         Consumer<M> validator) {
        assertNotRejected(eventClass.getName());
        return super.producesEvent(eventClass, validator);
    }

    @Override
    public MessageProducingExpected<S> producesEvents(Class<?>... eventClasses) {
        assertNotRejected(Stream.of(eventClasses)
                                .map(Class::getSimpleName)
                                .collect(joining(",")));
        return super.producesEvents(eventClasses);
    }

    @Override
    public <M extends Message> MessageProducingExpected<S> producesCommand(Class<M> commandClass,
                                                                           Consumer<M> validator) {
        assertNotRejected(commandClass.getName());
        return super.producesCommand(commandClass, validator);
    }

    @Override
    public MessageProducingExpected<S> producesCommands(Class<?>... commandClasses) {
        assertNotRejected(Stream.of(commandClasses)
                                .map(Class::getSimpleName)
                                .collect(joining(",")));
        return super.producesCommands(commandClasses);
    }

    private void assertNotRejected(String eventType) {
        final boolean rejected = rejection != null;
        if (rejected) {
            fail(format("Message was rejected. Expected messages(s): [%s]. Rejection: %s%s%s.",
                        eventType, rejection.getClass().getSimpleName(),
                        System.lineSeparator(), rejection));
        }
    }

    /**
     * Ensures that the command produces a rejection of {@code rejectionClass} type.
     *
     * @param rejectionClass type of the rejection expected to be produced
     */
    @SuppressWarnings("UnusedReturnValue")
    public MessageProducingExpected<S> throwsRejection(
            Class<? extends Message> rejectionClass) {
        assertNotNull(rejection, format("No rejection encountered. Expected %s",
                                        rejectionClass.getSimpleName()));
        assertTrue(rejectionClass.isInstance(rejection),
                   format("%s is not an instance of %s.",
                          rejection.getClass()
                                   .getSimpleName(),
                          rejectionClass));
        return self();
    }
}
