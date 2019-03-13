package io.spine.core;

import com.google.protobuf.Empty;
import io.spine.protobuf.AnyPacker;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.core.Acks.toCommandId;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Acks utility class should")
class AcksTest extends UtilityClassTest<Acks> {

    AcksTest() {
        super(Acks.class);
    }


    @Nested
    @DisplayName("obtain `CommandId`")
    class GetCommandId {

        @Test
        @DisplayName("returning ID value")
        void value() {
            CommandId commandId = CommandId.generate();
            Ack ack = newAck(commandId);
            assertThat(toCommandId(ack))
                    .isEqualTo(commandId);
        }

        @Test
        @DisplayName("throw `IllegalArgumentException`")
        void args() {
            assertThrows(IllegalArgumentException.class, () ->
                    toCommandId(newAck(Events.generateId()))
            );
        }
    }

    /*
     * Test environment
     ************************/

    static Ack newAck(MessageId messageId) {
        return AckVBuilder
                .newBuilder()
                .setMessageId(AnyPacker.pack(messageId))
                .setStatus(newOkStatus())
                .build();
    }

    private static Status newOkStatus() {
        return StatusVBuilder
                .newBuilder()
                .setOk(Empty.getDefaultInstance())
                .build();
    }
}
