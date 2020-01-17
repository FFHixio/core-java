/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.TenantId;
import io.spine.server.event.EventEnricher;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.testing.client.TestActorRequestFactory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Test fixture for multi-tenant Bounded Contexts.
 */
@VisibleForTesting
public final class MultitenantBlackBoxContext
        extends BlackBoxBoundedContext<MultitenantBlackBoxContext> {

    private @MonotonicNonNull TenantId tenantId;

    /**
     * Creates a new multi-tenant instance.
     */
    MultitenantBlackBoxContext(String name, EventEnricher enricher) {
        super(true, enricher, name);
    }

    /**
     * Switches the bounded context to operate on behalf of the specified tenant.
     *
     * @param tenant
     *         new tenant ID
     * @return current instance
     */
    public MultitenantBlackBoxContext withTenant(TenantId tenant) {
        this.tenantId = checkNotNull(tenant);
        return this;
    }

    @Override
    protected TestActorRequestFactory requestFactory() {
        return new TestActorRequestFactory(MultitenantBlackBoxContext.class, tenantId());
    }

    @Override
    protected ImmutableList<Command> select(CommandCollector collector) {
        return collector.ofTenant(tenantId());
    }

    @Override
    protected ImmutableList<Event> select(EventCollector collector) {
        return collector.ofTenant(tenantId());
    }

    @Override
    protected <D> @Nullable D readOperation(Supplier<D> supplier) {
        TenantAwareRunner tenantAwareRunner = TenantAwareRunner.with(tenantId());
        D result = tenantAwareRunner.evaluate(() -> super.readOperation(supplier));
        return result;
    }

    private TenantId tenantId() {
        checkState(tenantId != null,
                   "Set a tenant ID before calling receive and assert methods");
        return tenantId;
    }
}
