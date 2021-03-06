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

package io.spine.system.server;

import com.google.common.base.Objects;
import com.google.errorprone.annotations.Immutable;

/**
 * An immutable set of features of a {@link SystemContext}.
 */
@Immutable
final class SystemConfig implements SystemFeatures {

    private final boolean commandLog;
    private final boolean aggregateMirrors;
    private final boolean storeEvents;
    private final boolean parallelPosting;

    SystemConfig(boolean commandLog,
                 boolean aggregateMirrors,
                 boolean storeEvents,
                 boolean parallelPosting) {
        this.commandLog = commandLog;
        this.aggregateMirrors = aggregateMirrors;
        this.storeEvents = storeEvents;
        this.parallelPosting = parallelPosting;
    }

    @Override
    public boolean includeCommandLog() {
        return commandLog;
    }

    @Override
    public boolean includeAggregateMirroring() {
        return aggregateMirrors;
    }

    @Override
    public boolean includePersistentEvents() {
        return storeEvents;
    }

    @Override
    public boolean postEventsInParallel() {
        return parallelPosting;
    }

    @SuppressWarnings("OverlyComplexBooleanExpression")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SystemConfig)) {
            return false;
        }
        SystemConfig config = (SystemConfig) o;
        return commandLog == config.commandLog &&
                aggregateMirrors == config.aggregateMirrors &&
                storeEvents == config.storeEvents &&
                parallelPosting == config.parallelPosting;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(commandLog, aggregateMirrors, storeEvents, parallelPosting);
    }
}
