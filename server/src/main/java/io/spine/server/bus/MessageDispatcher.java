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

package io.spine.server.bus;

import io.spine.server.type.MessageEnvelope;
import io.spine.type.MessageClass;

import java.util.Set;

/**
 * A dispatcher of a message.
 *
 * @param <C>
 *         the type of class of the dispatched messages
 * @param <E>
 *         the type of the message envelopes
 * @param <R>
 *         the type of the result of the {@linkplain #dispatch(MessageEnvelope) dispatching
 *         function}. For {@linkplain UnicastDispatcher unicast dispatching} is the type of
 *         the IDs of entity that receives a dispatched message.
 *         For {@linkplain MulticastDispatcher multicast dispatching} is the type of the set
 *         of entity IDs.
 */
public interface MessageDispatcher<C extends MessageClass, E extends MessageEnvelope, R> {

    /**
     * Obtains a set of message classes that can be processed by this dispatcher.
     *
     * @return non-empty set of message classes
     */
    Set<C> messageClasses();

    /**
     * Dispatches the message contained in the passed envelope.
     *
     * @param envelope the envelope with the message
     */
    void dispatch(E envelope);

    /**
     * Checks if this dispatcher can dispatch the given message.
     *
     * <p>This method does not check that the type of the message is one of
     * the {@linkplain #messageClasses() dispatched message classes}. Instead, it validates
     * the message upon some custom rules of this dispatcher.
     *
     * <p>Extend this method to forbid messages from being dispatched to this instance.
     *
     * @param envelope
     *         the message to check
     * @return {@code true} is the given message can be dispatched by this dispatcher,
     *         {@code false} otherwise
     */
    default boolean canDispatch(E envelope) {
        return true;
    }
}
