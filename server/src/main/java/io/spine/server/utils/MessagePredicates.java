/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.utils;

import com.google.common.base.Predicate;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;

/**
 * A utility class containing predicates for {@link Message protobuf messages}.
 */
public class MessagePredicates {
    /** Prevents instantiation of this utility class. */
    private MessagePredicates() {
    }

    /**
     * @return an instance of an {@linkplain NonEmpty non-empty predicate}
     */
    public static Predicate<Message> nonEmpty() {
        return NonEmpty.INSTANCE;
    }

    /**
     * A predicate checking that message is not {@linkplain Empty empty}.
     */
    private enum NonEmpty implements Predicate<Message> {
    
        INSTANCE;
    
        private static final Empty EMPTY = Empty.getDefaultInstance();

        /**
         * Checks that message is not {@linkplain Empty empty}.
         *
         * @param  message the message being checked
         * @return {@code true} if the message is not empty, {@code false} otherwise 
         */
        @Override
        public boolean apply(Message message) {
            return !message.equals(EMPTY);
        }
    }
}
