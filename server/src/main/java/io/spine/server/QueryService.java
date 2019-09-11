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
package io.spine.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.spine.client.Queries;
import io.spine.client.Query;
import io.spine.client.QueryResponse;
import io.spine.client.grpc.QueryServiceGrpc;
import io.spine.logging.Logging;
import io.spine.server.model.UnknownEntityTypeException;
import io.spine.server.stand.InvalidRequestException;
import io.spine.server.stand.Stand;
import io.spine.type.TypeUrl;

import java.util.Map;
import java.util.Set;

import static io.spine.server.transport.Statuses.invalidArgumentWithCause;

/**
 * The {@code QueryService} provides a synchronous way to fetch read-side state from the server.
 *
 * <p>For asynchronous read-side updates please see {@link SubscriptionService}.
 */
public final class QueryService
        extends QueryServiceGrpc.QueryServiceImplBase
        implements Logging {

    private final ImmutableMap<TypeUrl, BoundedContext> typeToContextMap;

    private QueryService(Map<TypeUrl, BoundedContext> map) {
        super();
        this.typeToContextMap = ImmutableMap.copyOf(map);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public void read(Query query, StreamObserver<QueryResponse> responseObserver) {
        _debug().log("Incoming query: %s.", query);

        TypeUrl type = Queries.typeOf(query);
        BoundedContext boundedContext = typeToContextMap.get(type);
        if (boundedContext == null) {
            handleUnsupported(type, responseObserver);
        } else {
            handleQuery(boundedContext, query, responseObserver);
        }
    }

    private void handleQuery(BoundedContext boundedContext,
                             Query query,
                             StreamObserver<QueryResponse> responseObserver) {
        Stand stand = boundedContext.stand();
        try {
            stand.execute(query, responseObserver);
        } catch (InvalidRequestException e) {
            _error().log("Invalid request. `%s`", e.asError());
            StatusRuntimeException exception = invalidArgumentWithCause(e);
            responseObserver.onError(exception);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
            _error().withCause(e)
                    .log("Error processing query.");
            responseObserver.onError(e);
        }
    }

    private void handleUnsupported(TypeUrl type, StreamObserver<QueryResponse> observer) {
        UnknownEntityTypeException exception = new UnknownEntityTypeException(type);
        _error().withCause(exception)
                .log("Unknown type encountered.");
        observer.onError(exception);
    }

    public static class Builder {
        private final Set<BoundedContext> boundedContexts = Sets.newHashSet();

        @CanIgnoreReturnValue
        public Builder add(BoundedContext boundedContext) {
            // Save it to a temporary set so that it is easy to remove it if needed.
            boundedContexts.add(boundedContext);
            return this;
        }

        @CanIgnoreReturnValue
        public Builder remove(BoundedContext boundedContext) {
            boundedContexts.remove(boundedContext);
            return this;
        }

        public boolean contains(BoundedContext boundedContext) {
            return boundedContexts.contains(boundedContext);
        }

        /**
         * Builds the {@link QueryService}.
         *
         * @throws IllegalStateException if no bounded contexts were added.
         */
        public QueryService build() throws IllegalStateException {
            if (boundedContexts.isEmpty()) {
                String message = "Query service must have at least one `BoundedContext`.";
                throw new IllegalStateException(message);
            }
            ImmutableMap<TypeUrl, BoundedContext> map = createMap();
            QueryService result = new QueryService(map);
            return result;
        }

        private ImmutableMap<TypeUrl, BoundedContext> createMap() {
            ImmutableMap.Builder<TypeUrl, BoundedContext> builder = ImmutableMap.builder();
            for (BoundedContext boundedContext : boundedContexts) {
                putIntoMap(boundedContext, builder);
            }
            return builder.build();
        }

        private static void putIntoMap(BoundedContext boundedContext,
                                       ImmutableMap.Builder<TypeUrl, BoundedContext> mapBuilder) {

            Stand stand = boundedContext.stand();
            ImmutableSet<TypeUrl> exposedTypes = stand.exposedTypes();

            for (TypeUrl availableType : exposedTypes) {
                mapBuilder.put(availableType, boundedContext);
            }
        }
    }
}
