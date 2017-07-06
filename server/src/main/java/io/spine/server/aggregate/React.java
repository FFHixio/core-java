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

package io.spine.server.aggregate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method of an aggregate as one that <em>may</em> modify the state of the aggregate in
 * response to an <em>external</em> event.
 *
 * <p>A reacting method <strong>must:</strong>
 * <ul>
 *     <li>be annotated with {@link React @React};
 *
 *     <li>accept an event message (derived from {@link com.google.protobuf.Message
 *     Message}) which is <not>generated</not> by this aggregate, as the first parameter;
 *
 *     <li>return an event derived from {@link com.google.protobuf.Message Message}
 *     <strong>or</strong> several event messages returned as a {@link java.util.List List}.
 *     The returned event messages represent the state of the aggregate. Therefore, each returned
 *     event message must have corresponding {@linkplain Apply event applier}.
 * </ul>
 *
 * <p>If the annotation is applied to a method which does not satisfy any of these requirements,
 * this method will not be registering for receiving events.
 *
 * <p>A reacting method <strong>must not:</strong>
 * <ul>
 *     <li>React to events produced by this aggregate.
 *     <li>Return the event message on which it reacts.
 * </ul>
 *
 * @author Alexander Yevsyukov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface React {

    /**
     * When {@code true}, the annotated method of the aggregate reacts on the event generated from
     * outside of the Bounded Context to which this aggregate belongs.
     */
    boolean external() default false;
}
