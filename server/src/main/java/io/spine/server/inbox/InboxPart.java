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

package io.spine.server.inbox;

import com.google.protobuf.Any;
import io.spine.base.Time;
import io.spine.protobuf.AnyPacker;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.MessageEndpoint;
import io.spine.server.sharding.ShardIndex;
import io.spine.server.sharding.Sharding;
import io.spine.server.type.ActorMessageEnvelope;
import io.spine.type.TypeUrl;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An abstract base of {@link Inbox inbox} part.
 *
 * <p>Commands and events which are delivered to their targets through inbox are treated
 * in a similar way, but still there is some difference in storage mechanism and measures
 * taken in case of some runtime issues (e.g. duplication). Therefore the inbox is split into
 * parts, specific to each of the message types.
 *
 * @param <I>
 *         the type of identifier or inbox target entities
 * @param <M>
 *         the type of message envelopes, which are served by this inbox part
 */
abstract class InboxPart<I, M extends ActorMessageEnvelope<?, ?, ?>> {

    private final Endpoints<I, M> endpoints;
    private final InboxStorage storage;
    private final TypeUrl entityStateType;

    InboxPart(Inbox.Builder<I> builder, Endpoints<I, M> endpoints) {
        this.endpoints = endpoints;
        this.storage = builder.getStorage();
        this.entityStateType = builder.getEntityStateType();
    }

    /**
     * Fetches the message object wrapped into the {@code envelope} and sets it as a payload of
     * the record further passed to storage.
     */
    protected abstract void setRecordPayload(M envelope, InboxMessageVBuilder builder);

    protected abstract InboxMessageId inboxMsgIdFrom(M envelope);

    protected abstract M asEnvelope(InboxMessage message);

    protected abstract Delivery deliveryBasedOn(Collection<InboxMessage> deduplicationSource);

    void storeOrDeliver(M envelope, I entityId, InboxLabel label) {
        InboxId inboxId = InboxIds.wrap(entityId, entityStateType);
        MessageEndpoint<I, M> endpoint = getEndpoint(envelope, label, inboxId);
        Sharding sharding = ServerEnvironment.getInstance()
                                             .sharding();
        if (!sharding.enabled()) {
            endpoint.dispatchTo(entityId);
        } else {
            ShardIndex shardIndex = sharding.whichShardFor(entityId);
            InboxMessageVBuilder builder = InboxMessageVBuilder
                    .newBuilder()
                    .setId(inboxMsgIdFrom(envelope))
                    .setInboxId(inboxId)
                    .setShardIndex(shardIndex)
                    .setLabel(label)
                    .setWhenReceived(Time.currentTime());
            setRecordPayload(envelope, builder);
            InboxMessage message = builder.build();

            storage.write(message);
        }
    }

    private MessageEndpoint<I, M> getEndpoint(M envelope, InboxLabel label, InboxId inboxId) {
        return endpoints.get(label, envelope)
                        .orElseThrow(() -> new LabelNotFoundException(inboxId, label));
    }

    private MessageEndpoint<I, M> getEndpoint(InboxMessage message) {
        M envelope = asEnvelope(message);
        InboxLabel label = message.getLabel();
        InboxId inboxId = message.getInboxId();
        return getEndpoint(envelope, label, inboxId);
    }

    /**
     * An abstract base for routines which deliver {@code InboxMessage}s to their endpoints.
     *
     * <p>In case a duplication is found, the respective endpoint is
     * {@linkplain MessageEndpoint#onError(ActorMessageEnvelope, RuntimeException) notified}.
     */
    abstract class Delivery {

        private final Set<String> rawIds;

        Delivery(Collection<InboxMessage> deduplicationSource) {
            this.rawIds = deduplicationSource.stream()
                                             .map(InboxMessage::getId)
                                             .map(InboxMessageId::getValue)
                                             .collect(Collectors.toSet());
        }

        void deliver(InboxMessage message) {
            Optional<? extends RuntimeException> duplicationException = checkDuplicate(message);
            MessageEndpoint<I, M> endpoint = getEndpoint(message);
            if(duplicationException.isPresent()) {
                endpoint.onError(asEnvelope(message), duplicationException.get());
            } else {
                Any entityId = message.getInboxId()
                                .getEntityId()
                                .getId();
                I unpackedId = (I) AnyPacker.unpack(entityId);
                endpoint.dispatchTo(unpackedId);
            }
        }

        /**
         * Emits a exception, specific to the type of the message, in case the passed message
         * is determined to be a duplicate.
         *
         * @param duplicate
         *         the duplicate message
         * @return a type-specific exception
         */
        protected abstract RuntimeException onDuplicateFound(InboxMessage duplicate);

        /**
         * Checks whether the message has already been stored in the inbox.
         *
         * <p>In case of duplication returns an {@code Optional} containing the duplication
         * exception wrapped into a inbox-specific runtime exception for further handling.
         */
        private Optional<? extends RuntimeException> checkDuplicate(InboxMessage message) {
            String currentId = message.getId()
                                      .getValue();
            boolean hasDuplicate = rawIds.contains(currentId);
            if (hasDuplicate) {
                RuntimeException exception = onDuplicateFound(message);
                return Optional.of(exception);
            }
            return Optional.empty();
        }
    }
}
