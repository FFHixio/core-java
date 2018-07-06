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

package io.spine.system.server;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.ActorRequestFactory;
import io.spine.client.CommandFactory;
import io.spine.core.Command;
import io.spine.core.TenantId;
import io.spine.core.UserId;
import io.spine.server.BoundedContext;
import io.spine.server.tenant.TenantFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.grpc.StreamObservers.noOpObserver;

/**
 * @author Dmytro Dashenkov
 */
@Internal
public final class DefaultSystemGateway implements SystemGateway {

    private static final UserId SYSTEM = UserId
            .newBuilder()
            .setValue("SYSTEM")
            .build();

    private final BoundedContext system;

    public DefaultSystemGateway(BoundedContext system) {
        this.system = system;
    }

    @Override
    public void post(Message systemCommand) {
        CommandFactory commandFactory = buildRequestFactory().command();
        Command command = commandFactory.create(systemCommand);
        system.getCommandBus()
              .post(command, noOpObserver());
    }

    private ActorRequestFactory buildRequestFactory() {
        ActorRequestFactory result = system.isMultitenant()
                                     ? buildMultitenantFactory()
                                     : buildSingleTenantFaltory();
        return result;
    }

    private static ActorRequestFactory buildMultitenantFactory() {
        TenantFunction<ActorRequestFactory> contextFactory =
                new TenantFunction<ActorRequestFactory>(true) {
                    @Override
                    @CanIgnoreReturnValue
                    public ActorRequestFactory apply(@Nullable TenantId input) {
                        return ActorRequestFactory.newBuilder()
                                                  .setTenantId(input)
                                                  .setActor(SYSTEM)
                                                  .build();
                    }
                };
        ActorRequestFactory result = contextFactory.execute();
        checkNotNull(result);
        return result;
    }

    private static ActorRequestFactory buildSingleTenantFaltory() {
        return ActorRequestFactory.newBuilder()
                                  .setActor(SYSTEM)
                                  .build();
    }
}
