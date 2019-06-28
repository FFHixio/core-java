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
import io.spine.annotation.Internal;
import io.spine.base.Environment;
import io.spine.base.Identifier;
import io.spine.server.commandbus.CommandScheduler;
import io.spine.server.commandbus.ExecutorCommandScheduler;
import io.spine.server.delivery.Delivery;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.trace.TracerFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

/**
 * The server conditions and configuration under which the application operates.
 */
public final class ServerEnvironment implements AutoCloseable {

    private static final ServerEnvironment INSTANCE = new ServerEnvironment();

    /** The key of the Google AppEngine runtime version system property. */
    private static final String ENV_KEY_APP_ENGINE_RUNTIME_VERSION =
            "com.google.appengine.runtime.version";

    /** If set, contains the version of AppEngine obtained from the system property. */
    @SuppressWarnings("AccessOfSystemProperties") /*  Based on system property. */
    private static final @Nullable String appEngineRuntimeVersion =
            emptyToNull(System.getProperty(ENV_KEY_APP_ENGINE_RUNTIME_VERSION));

    /**
     * The deployment detector is instantiated with a system {@link DeploymentDetector} and
     * can be reassigned the value using {@link #configureDeployment(Supplier)}.
     *
     * <p>Value from this supplier are used to {@linkplain #deploymentType() get the deployment
     * type}.
     */
    private static Supplier<DeploymentType> deploymentDetector = DeploymentDetector.newInstance();

    /**
     * The identifier of the server instance running in scope of this application.
     *
     * <p>It is currently impossible to set the node identifier directly. This is a subject
     * to change in the future framework versions.
     */
    private final NodeId nodeId;

    /**
     * The strategy of delivering the messages received by entity repositories
     * to the entity instances.
     *
     * <p>By default, initialized with the {@linkplain Delivery#local() local} delivery.
     */
    private Delivery delivery;

    /**
     * The storage factory for the production mode of the application.
     */
    private @Nullable StorageFactory productionStorageFactory;

    /**
     * The factory of {@code Tracer}s used in this environment.
     */
    private @Nullable TracerFactory tracerFactory;

    /**
     * Provides schedulers used by all {@code CommandBus} instances of this environment.
     */
    private Supplier<CommandScheduler> commandScheduler;

    private ServerEnvironment() {
        delivery = Delivery.local();
        nodeId = NodeId.newBuilder()
                       .setValue(Identifier.newUuid())
                       .vBuild();
        commandScheduler = ExecutorCommandScheduler::new;
    }

    /**
     * Returns a singleton instance.
     */
    public static ServerEnvironment instance() {
        return INSTANCE;
    }

    /**
     * The type of the environment application is deployed to.
     */
    public DeploymentType deploymentType() {
        return deploymentDetector.get();
    }

    /**
     * Returns {@code true} if the code is running on the Google App Engine,
     * {@code false} otherwise.
     *
     * @deprecated this method will be removed in 1.0, please verify {@linkplain #deploymentType()
     *         deployment type} to match any of
     *         {@link DeploymentType#APPENGINE_EMULATOR APPENGINE_EMULATOR} or
     *         {@link DeploymentType#APPENGINE_CLOUD APPENGINE_CLOUD} instead.
     */
    @Deprecated
    public boolean isAppEngine() {
        boolean isVersionPresent = appEngineRuntimeVersion != null;
        return isVersionPresent;
    }

    /**
     * Returns an optional with current Google App Engine version
     * or {@code empty} if the program is not running on the App Engine.
     *
     * @deprecated this method will be removed in 1.0.
     */
    @Deprecated
    public Optional<String> appEngineVersion() {
        return Optional.ofNullable(appEngineRuntimeVersion);
    }

    /**
     * Updates the delivery for this environment.
     *
     * <p>This method is most typically used upon an application start. It's very uncommon and
     * even dangerous to update the delivery mechanism later when the message delivery
     * process may have been already used by various {@code BoundedContext}s.
     */
    public void configureDelivery(Delivery delivery) {
        checkNotNull(delivery);
        this.delivery = delivery;
    }

    /**
     * Returns the delivery mechanism specific to this environment.
     *
     * <p>Unless {@linkplain #configureDelivery(Delivery) updated manually}, returns
     * a {@linkplain Delivery#local() local implementation} of {@code Delivery}.
     */
    public Delivery delivery() {
        return delivery;
    }

    /**
     * Obtains command scheduling mechanism used by {@code CommandBus} in this environment.
     */
    public CommandScheduler newCommandScheduler() {
        return commandScheduler.get();
    }

    /**
     * Assigns command scheduling mechanism used at this environment by all
     * {@code CommandBus} instances.
     *
     * <p>If not configured, {@link ExecutorCommandScheduler} is used.
     */
    public void scheduleCommandsUsing(Supplier<CommandScheduler> commandScheduler) {
        checkNotNull(commandScheduler);
        this.commandScheduler = commandScheduler;
    }

    /**
     * Obtains the identifier of the server node, on which this code is running at the moment.
     *
     * <p>At the moment, the node identifier is always UUID-generated. In future versions of the
     * framework it is expected to become configurable.
     */
    public NodeId nodeId() {
        return nodeId;
    }

    /**
     * Sets the default {@linkplain DeploymentType deployment type}
     * {@linkplain Supplier supplier} which utilizes system properties.
     */
    @VisibleForTesting
    public static void resetDeploymentType() {
        Supplier<DeploymentType> supplier = DeploymentDetector.newInstance();
        configureDeployment(supplier);
    }

    /**
     * Makes the {@link #deploymentType()} return the values from the provided supplier.
     *
     * <p>When supplying your own deployment type in tests, remember to
     * {@linkplain #resetDeploymentType() reset it} during tear down.
     */
    @VisibleForTesting
    public static void configureDeployment(Supplier<DeploymentType> supplier) {
        checkNotNull(supplier);
        deploymentDetector = supplier;
    }

    /**
     * Assigns {@code StorageFactory} for the production mode of the application.
     *
     * <p>Tests use {@code InMemoryStorageFactory}.
     */
    public void configureProductionStorage(StorageFactory storageFactory) {
        checkNotNull(storageFactory);
        checkArgument(
                !(storageFactory instanceof InMemoryStorageFactory),
                "%s cannot be used for production storage.",
                InMemoryStorageFactory.class.getName()
        );
        this.productionStorageFactory = storageFactory;
    }

    /**
     * This is a test-only method required in tests (or cleanup after tests) that deal
     * with assigning production storage factory.
     */
    @VisibleForTesting
    void clearStorageFactory() {
        this.productionStorageFactory = null;
    }

    /**
     * Assigns {@code TracerFactory} to this server environment.
     */
    public void configureTracing(TracerFactory tracerFactory) {
        this.tracerFactory = checkNotNull(tracerFactory);
    }

    /**
     * Obtains {@link TracerFactory} associated with this server environment.
     */
    public Optional<TracerFactory> tracing() {
        return Optional.ofNullable(tracerFactory);
    }

    /**
     * This is a test-only method required in tests that deal with assigning tracer factories.
     */
    @Internal
    @VisibleForTesting
    public void clearTracerFactory() {
        this.tracerFactory = null;
    }

    /**
     * Obtains production {@code StorageFactory} previously associated with the environment.
     *
     * @return {@code StorageFactory} instance for the production storage
     * @throws NullPointerException
     *         if the production {@code StorageFactory} was not
     *         {@linkplain #configureProductionStorage(StorageFactory) configured} prior to the call
     */
    public StorageFactory storageFactory() {
        if (Environment.getInstance().isTests()) {
            return InMemoryStorageFactory.newInstance();
        }
        checkNotNull(productionStorageFactory,
                     "Production `%s` is not configured." +
                             " Please call `configureProductionStorage()`.",
                     StorageFactory.class.getSimpleName()
        );
        return productionStorageFactory;
    }

    /**
     * Releases resources associated with this instance.
     */
    @Override
    public void close() throws Exception {
        if (tracerFactory != null) {
            tracerFactory.close();
        }
        if (productionStorageFactory != null) {
            productionStorageFactory.close();
        }
    }
}
