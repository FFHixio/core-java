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

package io.spine.server.integration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.Status;
import io.spine.type.TypeUrl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Contains the data on all acknowledgements in a {@link BlackBoxBoundedContext Bounded Context}.
 * Can be queried for information about acks, errors, and rejections.
 *
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
class CommandAcks {

    private static final Rejection EMPTY_REJECTION = Rejection.getDefaultInstance();
    private static final Error EMPTY_ERROR = Error.getDefaultInstance();

    private final List<Ack> acks = newArrayList();
    private final List<Error> errors = newArrayList();
    private final List<Rejection> rejections = newArrayList();
    private final Map<RejectionClass, Integer> rejectionTypes;

    CommandAcks(Iterable<Ack> responses) {
        for (Ack response : responses) {
            acks.add(response);

            Status status = response.getStatus();

            Error error = status.getError();
            if (!error.equals(EMPTY_ERROR)) {
                errors.add(error);
            }

            Rejection rejection = status.getRejection();
            if (!rejection.equals(EMPTY_REJECTION)) {
                rejections.add(rejection);
            }
        }
        rejectionTypes = countRejectionTypes(rejections);
    }

    /**
     * Counts the number of times the domain event types are included in the provided list.
     *
     * @param rejections a list of {@link Event}
     * @return a mapping of Rejection classes to their count
     */
    private static Map<RejectionClass, Integer> countRejectionTypes(List<Rejection> rejections) {
        Map<RejectionClass, Integer> countForType = new HashMap<>();
        for (Rejection rejection : rejections) {
            RejectionClass type = RejectionClass.of(rejection);
            int currentCount = countForType.getOrDefault(type, 0);
            countForType.put(type, currentCount + 1);
        }
        return ImmutableMap.copyOf(countForType);
    }

    /**
     * @return the total number of acknowledgements observed in a Bounded Context.
     */
    public int count() {
        return acks.size();
    }

    /*
     * Errors
     ******************************************************************************/

    /**
     * @return {@code true} if errors did occur in the Bounded Context during command handling,
     * {@code false} otherwise.
     */
    boolean containErrors() {
        return !errors.isEmpty();
    }

    /**
     * @return a total number of errors which were observed in Bounded Context acknowledgements.
     */
    int countErrors() {
        return errors.size();
    }

    /**
     * @param error an error that matches the one in acknowledgement
     * @return {@code true} if the provided error did occur in the Bounded Context during
     * command handling, {@code false} otherwise.
     */
    boolean containErrors(Error error) {
        checkNotNull(error);
        return errors.contains(error);
    }

    /**
     * @return {@code true} if an error which matches the provided qualifier did occur in
     * the Bounded Context during command handling, {@code false} otherwise.
     */
    boolean containErrors(ErrorQualifier qualifier) {
        checkNotNull(qualifier);
        return errors.stream()
                     .anyMatch(qualifier);
    }

    /**
     * @return a total number of times the provided error was observed in the
     * Bounded Context responses
     */
    long countErrors(Error error) {
        checkNotNull(error);
        return errors.stream()
                     .filter(error::equals)
                     .count();
    }

    /**
     * @return a total number of times errors matching the provided qualifier were observed in the
     * Bounded Context responses
     */
    long countErrors(ErrorQualifier qualifier) {
        checkNotNull(qualifier);
        return errors.stream()
                     .filter(qualifier)
                     .count();
    }

    /*
     * Rejections
     ******************************************************************************/

    /**
     * @return {@code true} if there were any rejections in the Bounded Context,
     * {@code false} otherwise
     */
    boolean containRejections() {
        return !rejections.isEmpty();
    }

    /**
     * @return a total amount of rejections observed in Bounded Context
     */
    int countRejections() {
        return rejections.size();
    }

    /**
     * @param type rejection type in a form of {@link RejectionClass RejectionClass}
     * @return {@code true} if the rejection of a provided type was observed in the Bounded Context,
     * {@code false} otherwise
     */
    boolean containRejections(RejectionClass type) {
        return rejectionTypes.containsKey(type);
    }

    /**
     * @param type rejection type in a form of {@link RejectionClass RejectionClass}
     * @return an amount of rejections of the provided type observed in Bounded Context
     */
    int countRejections(RejectionClass type) {
        return rejectionTypes.get(type);
    }

    /**
     * @param predicate a domain message representing the rejection
     * @param type      a class of a domain rejection
     * @param <T>       a domain rejection type
     * @return {@code true} if the rejection matching the predicate was observed
     * in the Bounded Context, {@code false} otherwise
     */
    <T extends Message> boolean containRejection(Class<T> type, Predicate<T> predicate) {
        return rejections.stream()
                         .anyMatch(new RejectionFilter<>(type, predicate));
    }

    /**
     * @param predicate a domain message representing the rejection
     * @param type      a class of a domain rejection
     * @param <T>       a domain rejection type
     * @return an amount of rejections matching the predicate observed in Bounded Context
     */
    <T extends Message> long countRejections(Class<T> type, Predicate<T> predicate) {
        return rejections.stream()
                         .filter(new RejectionFilter<>(type, predicate))
                         .count();
    }

    /**
     * A predicate filtering the {@link Rejection rejections} which match the provided predicate.
     */
    private static class RejectionFilter<T extends Message> implements Predicate<Rejection> {

        private final TypeUrl typeUrl;
        private final Predicate<T> predicate;

        private RejectionFilter(Class<T> rejectionType, Predicate<T> predicate) {
            this.typeUrl = TypeUrl.of(rejectionType);
            this.predicate = predicate;
        }

        @Override
        public boolean test(Rejection rejection) {
            T message = unpack(rejection.getMessage());
            return typeUrl.equals(TypeUrl.of(message)) && predicate.test(message);
        }
    }
}
