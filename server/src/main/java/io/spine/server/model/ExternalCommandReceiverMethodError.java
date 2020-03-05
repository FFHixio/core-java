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

package io.spine.server.model;

import io.spine.server.command.model.CommandAcceptingMethod;
import io.spine.server.command.model.CommandHandlingClass;
import io.spine.server.type.CommandClass;

import java.util.Collection;

import static io.spine.server.model.ModelError.MessageFormatter.toStringEnumeration;

/**
 * An error thrown when one or more of the command accepting methods are marked {@code external}
 * in the {@link CommandHandlingClass}.
 *
 * <p>Although technically it's possible for entities like
 * {@linkplain io.spine.server.command.Command commanders} to declare their command substitution
 * methods as {@linkplain io.spine.server.command.Command#external() external}, there is no notion
 * of "external" commands in the system, and, to avoid confusion, such declarations should be
 * avoided.
 *
 * <p>Example of a faulty method:
 * <pre>
 *     {@code
 *     \@Command(external = true)
 *     public StartProject on(CreateProject command) {
 *     }
 *     }
 * </pre>
 */
public final class ExternalCommandReceiverMethodError extends ModelError {

    private static final long serialVersionUID = 0L;

    private static final String MESSAGE =
            "The class `%s` declares `external` command receiver methods for command types: %s. " +
                    "Only event accepting methods should be marked as `external`.";

    public ExternalCommandReceiverMethodError(
            CommandHandlingClass classWithViolation,
            Collection<? extends CommandAcceptingMethod<?, ?>> invalidMethods) {
        super(MESSAGE, classWithViolation, handledCommandTypes(invalidMethods));
    }

    private static String
    handledCommandTypes(Collection<? extends CommandAcceptingMethod<?, ?>> handlers) {
        String result = handlers
                .stream()
                .map(CommandAcceptingMethod::messageClass)
                .map(CommandClass::toString)
                .map(MessageFormatter::backtick)
                .collect(toStringEnumeration());
        return result;
    }
}