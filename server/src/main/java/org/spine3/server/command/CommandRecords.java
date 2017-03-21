/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.CommandStatus;
import org.spine3.type.TypeName;

import javax.annotation.Nullable;
import java.util.Iterator;

import static org.spine3.base.Commands.generateId;
import static org.spine3.base.Commands.getId;
import static org.spine3.base.Identifiers.EMPTY_ID;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;

/**
 * Utility class for working with {@link CommandRecord}s.
 *
 * @author Alexander Yevsyukov
 */
class CommandRecords {

    private static final Function<CommandRecord, Command> TO_COMMAND =
            new Function<CommandRecord, Command>() {
                @Override
                public Command apply(@Nullable CommandRecord record) {
                    if (record == null) {
                        return Command.getDefaultInstance();
                    }
                    final Command cmd = record.getCommand();
                    return cmd;
                }
            };

    private CommandRecords() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a command storage record builder passed on the passed parameters.
     *
     * <p>{@code targetId} and {@code targetIdType} are set to empty strings if
     * the command is not for an entity.
     *
     * @param command
     *            a command to convert to a record. This includes instances of faulty commands.
     *            An example of such a fault is missing command ID.
     * @param status
     *            a command status to set in the record
     * @param generatedCommandId
     *            a command ID to used because the passed command does not have own ID.
     *            If the command has own ID this parameter is {@code null}.
     * @return a storage record
     */
    static CommandRecord.Builder newRecordBuilder(Command command,
                                                  CommandStatus status,
                                                  @Nullable CommandId generatedCommandId) {
        final CommandContext context = command.getContext();

        final CommandId commandId = generatedCommandId != null
                                    ? generatedCommandId
                                    : context.getCommandId();

        final String commandType = TypeName.ofCommand(command)
                                           .getSimpleName();

        final CommandRecord.Builder builder =
                CommandRecord.newBuilder()
                             .setCommandId(commandId)
                             .setCommandType(commandType)
                             .setCommand(command)
                             .setTimestamp(getCurrentTime())
                             .setStatus(ProcessingStatus.newBuilder()
                                                        .setCode(status));
        return builder;
    }

    /**
     * Obtains or generates a {@code CommandId} from the passed command.
     *
     * <p>We don't have a command ID in the passed command.
     * But need an ID to store the error in the record associated
     * with this command. So, the ID will be generated.
     *
     * <p>We pass this ID to the record, so that it has an identity.
     * But this ID does not belong to the command.
     *
     * <p>Therefore, commands without ID can be found by records
     * where `command.context.command_id` field is empty.
     */
    static CommandId getOrGenerateCommandId(Command command) {
        CommandId id = getId(command);
        if (idToString(id).equals(EMPTY_ID)) {
            id = generateId();
        }
        return id;
    }

    /** Converts {@code CommandStorageRecord}s to {@code Command}s. */
    static Iterator<Command> toCommandIterator(Iterator<CommandRecord> records) {
        return Iterators.transform(records, TO_COMMAND);
    }
}
