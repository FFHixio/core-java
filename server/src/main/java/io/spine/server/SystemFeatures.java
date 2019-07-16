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

import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * System bounded context feature configuration.
 */
public final class SystemFeatures {

    private boolean commandLog;
    private boolean aggregateMirrors;
    private boolean storeEvents;

    /**
     * Prevents direct instantiation.
     */
    private SystemFeatures() {
    }

    static SystemFeatures defaults() {
        return new SystemFeatures()
                .disableCommandLog()
                .enableAggregateQuerying()
                .forgetEvents();
    }

    @CanIgnoreReturnValue
    public SystemFeatures enableCommandLog() {
        this.commandLog = true;
        return this;
    }

    @CanIgnoreReturnValue
    public SystemFeatures disableCommandLog() {
        this.commandLog = false;
        return this;
    }

    @CanIgnoreReturnValue
    public SystemFeatures enableAggregateQuerying() {
        this.aggregateMirrors = true;
        return this;
    }

    @CanIgnoreReturnValue
    public SystemFeatures disableAggregateQuerying() {
        this.aggregateMirrors = false;
        return this;
    }

    public SystemFeatures persistEvents() {
        this.storeEvents = true;
        return this;
    }

    public SystemFeatures forgetEvents() {
        this.storeEvents = false;
        return this;
    }

    public boolean includeCommandLog() {
        return commandLog;
    }

    public boolean includeAggregateMirroring() {
        return aggregateMirrors;
    }

    public boolean includePersistentEvents() {
        return storeEvents;
    }
}
