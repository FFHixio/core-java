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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.logging.Logging;
import io.spine.server.transport.GrpcContainer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT;

/**
 * Exposes one or more Bounded Contexts using {@link io.spine.server.CommandService CommandService}
 * and {@link io.spine.server.QueryService QueryService}.
 */
public final class Server implements Logging {

    /** The port assigned to the server. */
    private final int port;

    /** Bounded Contexts exposed by the server. */
    private final ImmutableSet<BoundedContext> contexts;

    /** The container for Command- and Query- services. */
    private final GrpcContainer grpcContainer;

    /**
     * Creates a new builder for the server.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    private Server(Builder builder) {
        this.port = builder.port;
        this.contexts = ImmutableSet.copyOf(builder.contexts);
        this.grpcContainer = createContainer(contexts);
    }

    /**
     * Starts the server.
     *
     * @throws IOException if the service could not be started
     */
    public void start() throws IOException {
        grpcContainer.start();
        grpcContainer.addShutdownHook();

        log().info("Server started, listening to the port {}.", port);
    }

    /**
     * Waits for the server to become terminated.
     */
    @SuppressWarnings("unused")
    public void awaitTermination() {
        grpcContainer.awaitTermination();
    }

    /**
     * Initiates an orderly shutdown in which existing calls continue but new calls are rejected.
     */
    public void shutdown() {
        log().info("Shutting down the server...");
        grpcContainer.shutdown();
        contexts.forEach(context -> {
            try {
                context.close();
            } catch (Exception e) {
                String contextName = context.name()
                                            .getValue();
                _error(e, "Unable to close Bounded Context {}.", contextName);
            }
        });
    }

    /**
     * Initiates a forceful shutdown in which preexisting and new calls are rejected.
     *
     * <p>The method returns when the server becomes terminated.
     * The most common usage scenario for this method is clean-up in unit tests
     * (e.g. {@literal @}{@code AfterEach} in JUnit5) that involve client-server communications.
     */
    @VisibleForTesting
    public void shutdownAndWait() {
        grpcContainer.shutdownNowAndWait();
    }

    /**
     * Obtains the port assigned to the server.
     */
    public int port() {
        return port;
    }

    /**
     * Creates a container for the passed Bounded Contexts.
     */
    private GrpcContainer createContainer(Set<BoundedContext> contexts) {
        CommandService.Builder commandService = CommandService.newBuilder();
        QueryService.Builder queryService = QueryService.newBuilder();

        contexts.forEach(context -> {
            commandService.add(context);
            queryService.add(context);
        });

        GrpcContainer result = GrpcContainer
                .newBuilder()
                .setPort(port)
                .addService(commandService.build())
                .addService(queryService.build())
                .build();
        return result;
    }

    public static class Builder {

        private int port = DEFAULT_CLIENT_SERVICE_PORT;
        private final Set<BoundedContextBuilder> contextBuilders = new HashSet<>();

        private final Set<BoundedContext> contexts = new HashSet<>();

        /** Prevents instantiation from outside. */
        private Builder() {
        }

        /**
         * Adds a builder for a {@code BoundedContext} to be added the server.
         */
        @CanIgnoreReturnValue
        public Builder add(BoundedContextBuilder contextBuilder) {
            checkNotNull(contextBuilder);
            contextBuilders.add(contextBuilder);
            return this;
        }

        /**
         * Defines a port for the server.
         */
        @CanIgnoreReturnValue
        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        /**
         * Creates a new instance of the server.
         */
        public Server build() {
            buildContexts();
            Server result = new Server(this);
            return result;
        }

        private void buildContexts() {
            for (BoundedContextBuilder builder : contextBuilders) {
                contexts.add(builder.build());
            }
        }
    }
}
