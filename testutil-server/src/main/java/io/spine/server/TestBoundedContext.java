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

import com.google.common.base.Supplier;
import io.spine.core.BoundedContextName;
import io.spine.core.CommandEnvelope;
import io.spine.server.bus.BusFilter;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.StorageFactorySwitch;
import io.spine.server.storage.memory.InMemoryStorageFactory;

/**
 * A bounded context used for unit testing of an {@link io.spine.server.entity.Entity Entity}.
 *
 * @author Vladyslav Lubenskyi
 */
public class TestBoundedContext {

    private static final boolean MULTITENANT = false;

    private static final BoundedContextName NAME =
            BoundedContextName.newBuilder()
                              .setValue("TestBoundedContext")
                              .build();

    private TestBoundedContext() {
        // Prevent instantiation.
    }

    /**
     * Creates a new instance of the test bounded context.
     *
     * @return {@code BoundedContext} instance
     */
    @SuppressWarnings("Guava") // Spine Java 7 API.
    public static BoundedContext create() {
        final StorageFactorySwitch storageFactorySwitch =
                StorageFactorySwitch.newInstance(NAME, MULTITENANT);
        final Supplier<StorageFactory> factorySupplier = new StorageFactorySupplier();
        final StorageFactorySwitch supplier = storageFactorySwitch.init(factorySupplier,
                                                                        factorySupplier);
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setName(NAME.getValue())
                                                            .setStorageFactorySupplier(supplier)
                                                            .build();
        return boundedContext;
    }

    /**
     * Creates a new instance of the test bounded context with the given command filter.
     *
     * @param commandFilter a command filter
     * @return {@link BoundedContext} instance
     */
    @SuppressWarnings("Guava") // Spine Java 7 API.
    public static BoundedContext create(BusFilter<CommandEnvelope> commandFilter) {
        final StorageFactorySwitch storageFactorySwitch =
                StorageFactorySwitch.newInstance(NAME, MULTITENANT);
        final Supplier<StorageFactory> factorySupplier = new StorageFactorySupplier();
        final StorageFactorySwitch supplier = storageFactorySwitch.init(factorySupplier,
                                                                        factorySupplier);
        final CommandBus.Builder commandBus = CommandBus.newBuilder()
                                                        .appendFilter(commandFilter);
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .setName(NAME.getValue())
                                                            .setStorageFactorySupplier(supplier)
                                                            .setCommandBus(commandBus)
                                                            .build();
        return boundedContext;
    }

    private static class StorageFactorySupplier implements Supplier<StorageFactory> {

        @Override
        public StorageFactory get() {
            return InMemoryStorageFactory.newInstance(NAME, MULTITENANT);
        }
    }
}
