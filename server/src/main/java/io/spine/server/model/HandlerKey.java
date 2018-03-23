/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.model;

import com.google.common.base.MoreObjects;
import com.google.protobuf.Empty;
import io.spine.core.CommandClass;
import io.spine.type.MessageClass;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A key of {@link HandlerMethod}.
 *
 * <p>Contains information about parameters of a handler method.
 *
 * @param <M> the type of the handled message class
 * @author Dmytro Grankin
 */
public final class HandlerKey<M extends MessageClass> {

    private final M handledMessage;
    private final MessageClass originMessage;

    private HandlerKey(M handledMessage, MessageClass originMessage) {
        this.handledMessage = checkNotNull(handledMessage);
        this.originMessage = checkNotNull(originMessage);
    }

    public static <M extends MessageClass>
    HandlerKey<M> of(M handledMessage, MessageClass originMessage) {
        return new HandlerKey<>(handledMessage, originMessage);
    }

    public static <M extends MessageClass> HandlerKey<M> of(M handledMessage) {
        return new HandlerKey<>(handledMessage, CommandClass.of(Empty.class));
    }

    /**
     * Obtains a {@link MessageClass}, which is handled by {@link HandlerMethod} with this key.
     */
    public M getHandledMessageCls() {
        return handledMessage;
    }

    /**
     * Obtains {@link MessageClass} of the message caused
     * {@linkplain #getHandledMessageCls() handled message}.
     *
     * @return the message class of the origin
     *         or {@code CommandClass} of {@code Empty} if origin is ignored
     */
    public MessageClass getOriginCls() {
        return originMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HandlerKey)) {
            return false;
        }
        HandlerKey<?> that = (HandlerKey<?>) o;
        return Objects.equals(handledMessage, that.handledMessage) &&
                Objects.equals(originMessage, that.originMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(handledMessage, originMessage);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("handledMessage", handledMessage)
                          .add("originMessage", originMessage)
                          .toString();
    }
}
