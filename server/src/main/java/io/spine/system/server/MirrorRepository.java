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

package io.spine.system.server;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.EntityStateWithVersion;
import io.spine.client.Query;
import io.spine.client.ResponseFormat;
import io.spine.client.Target;
import io.spine.client.TargetFilters;
import io.spine.code.proto.EntityStateOption;
import io.spine.core.MessageId;
import io.spine.option.EntityOption;
import io.spine.option.EntityOption.Kind;
import io.spine.server.entity.EntityVisibility;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRouting;
import io.spine.system.server.event.EntityArchived;
import io.spine.system.server.event.EntityDeleted;
import io.spine.system.server.event.EntityRestored;
import io.spine.system.server.event.EntityStateChanged;
import io.spine.system.server.event.EntityUnarchived;
import io.spine.type.TypeUrl;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Streams.stream;
import static com.google.protobuf.util.FieldMaskUtil.fromFieldNumbers;
import static io.spine.option.EntityOption.Kind.AGGREGATE;
import static io.spine.option.EntityOption.Kind.KIND_UNKNOWN;
import static io.spine.system.server.Mirror.ID_FIELD_NUMBER;
import static io.spine.system.server.Mirror.STATE_FIELD_NUMBER;
import static io.spine.system.server.Mirror.VERSION_FIELD_NUMBER;
import static io.spine.system.server.MirrorProjection.buildFilters;

/**
 * The repository for {@link Mirror} projections.
 *
 * <p>An entity has a mirror if all of the following conditions are met:
 * <ul>
 *     <li>the entity repository is registered in a domain bounded context;
 *     <li>the entity state is marked as an {@link EntityOption.Kind#AGGREGATE AGGREGATE};
 *     <li>the aggregate is visible for querying or subscribing.
 * </ul>
 *
 * <p>In other cases, an entity won't have a {@link Mirror}.
 */
@Internal
public final class MirrorRepository
        extends ProjectionRepository<MirrorId, MirrorProjection, Mirror> {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static final FieldMask AGGREGATE_STATE_WITH_VERSION = fromFieldNumbers(
            Mirror.class, ID_FIELD_NUMBER, STATE_FIELD_NUMBER, VERSION_FIELD_NUMBER
    );

    @Override
    protected void setupEventRouting(EventRouting<MirrorId> routing) {
        super.setupEventRouting(routing);
        routing.route(EntityStateChanged.class,
                      (message, context) -> targetsFrom(message.getEntity()))
               .route(EntityArchived.class,
                      (message, context) -> targetsFrom(message.getEntity()))
               .route(EntityDeleted.class,
                      (message, context) -> targetsFrom(message.getEntity()))
               .route(EntityUnarchived.class,
                      (message, context) -> targetsFrom(message.getEntity()))
               .route(EntityRestored.class,
                      (message, context) -> targetsFrom(message.getEntity()));
    }

    /**
     * Tells if the entity type should be mirrored.
     */
    public static boolean shouldMirror(TypeUrl type) {
        Kind kind = entityKind(type);
        EntityVisibility visibility = entityVisibility(type);
        boolean aggregate = kind == AGGREGATE && visibility.isNotNone();
        return aggregate;
    }

    private static Set<MirrorId> targetsFrom(MessageId entityId) {
        TypeUrl type = TypeUrl.parse(entityId.getTypeUrl());
        boolean shouldMirror = shouldMirror(type);
        return shouldMirror
               ? ImmutableSet.of(idFrom(entityId))
               : ImmutableSet.of();
    }

    private static EntityOption.Kind entityKind(TypeUrl type) {
        Descriptor descriptor = type.toTypeName()
                                    .messageDescriptor();
        Optional<EntityOption> option = EntityStateOption.valueOf(descriptor);
        Kind kind = option.map(EntityOption::getKind)
                          .orElse(KIND_UNKNOWN);
        if (kind == KIND_UNKNOWN) {
            logger.atWarning()
                  .log("Received a state update of entity `%s`. The entity kind is unknown. " +
                               "Please use `(entity)` option to define entity states.", type);
        }
        return kind;
    }

    private static EntityVisibility entityVisibility(TypeUrl entityStateType) {
        Class<Message> stateClass = entityStateType.toTypeName()
                                                   .toMessageClass();
        EntityVisibility visibility = EntityVisibility.of(stateClass);
        return visibility;
    }

    private static MirrorId idFrom(MessageId messageId) {
        Any any = messageId.getId();
        MirrorId result = MirrorId
                .newBuilder()
                .setValue(any)
                .setTypeUrl(messageId.getTypeUrl())
                .build();
        return result;
    }

    /**
     * Executes the given query upon the aggregate states of the target type.
     *
     * <p>In a multitenant environment, this method should only be invoked if the current tenant is
     * set to the one in the {@link Query}.
     *
     * @param query
     *         an aggregate query
     * @return an {@code Iterator} over the result aggregate states
     * @see SystemReadSide#readDomainAggregate(Query)
     */
    Iterator<EntityStateWithVersion> execute(Query query) {
        ResponseFormat requestedFormat = query.getFormat();
        FieldMask aggregateFields = requestedFormat.getFieldMask();
        ResponseFormat responseFormat = requestedFormat
                .toBuilder()
                .setFieldMask(AGGREGATE_STATE_WITH_VERSION)
                .vBuild();
        Target target = query.getTarget();
        TargetFilters filters = buildFilters(target);
        Iterator<MirrorProjection> mirrors = find(filters, responseFormat);
        Iterator<EntityStateWithVersion> result = aggregateStates(mirrors, aggregateFields);
        return result;
    }

    private static Iterator<EntityStateWithVersion>
    aggregateStates(Iterator<MirrorProjection> projections, FieldMask requiredFields) {
        Iterator<EntityStateWithVersion> result = stream(projections)
                .map(mirror -> toAggregateState(mirror, requiredFields))
                .iterator();
        return result;
    }

    private static EntityStateWithVersion
    toAggregateState(MirrorProjection mirror, FieldMask requiredFields) {
        EntityStateWithVersion result = EntityStateWithVersion
                .newBuilder()
                .setState(mirror.aggregateState(requiredFields))
                .setVersion(mirror.aggregateVersion())
                .build();
        return result;
    }
}
