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

package io.spine.server.stand.given;

import com.google.protobuf.Message;
import io.spine.client.Subscription;
import io.spine.client.Target;
import io.spine.client.Targets;
import io.spine.test.aggregate.Project;
import io.spine.test.commandservice.customer.Customer;
import io.spine.type.TypeUrl;

import java.util.Collections;

/**
 * @author Dmytro Dashenkov
 * @author Dmytro Kuzmin
 */
public class SubscriptionRecordTestEnv {

    /** Prevents instantiation of this utility class. */
    private SubscriptionRecordTestEnv() {
    }

    @SuppressWarnings("UtilityClass")
    public static class Given {

        public static final TypeUrl TYPE = TypeUrl.of(Project.class);
        public static final TypeUrl OTHER_TYPE = TypeUrl.of(Customer.class);

        public static Target target() {
            final Target target = Targets.allOf(Project.class);
            return target;
        }

        public static Target target(Message targetId) {
            final Target target = Targets.someOf(Project.class, Collections.singleton(targetId));
            return target;
        }

        public static Subscription subscription() {
            final Subscription subscription = Subscription.getDefaultInstance();
            return subscription;
        }
    }
}
