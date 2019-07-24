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

package io.spine.testing.server.blackbox.verify.query;

import io.spine.testing.server.blackbox.BbTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static io.spine.testing.server.blackbox.verify.query.given.GivenQueryResponse.TASK_TITLE;
import static io.spine.testing.server.blackbox.verify.query.given.GivenQueryResponse.responseWithSingleState;

@DisplayName("QueryResultSubjectShould")
class QueryResultSubjectTest {

    private QueryResultSubject subject;

    @BeforeEach
    void initSubject() {
        subject = QueryResultSubject.assertQueryResult(responseWithSingleState());
    }

    @Test
    @DisplayName("check contains all matching the provided Predicate")
    void checkContainsAllMatching() {
        Predicate<BbTask> taskPredicate = task -> TASK_TITLE.equals(task.getTitle());
        subject.containsAllMatching(taskPredicate);
    }
}
