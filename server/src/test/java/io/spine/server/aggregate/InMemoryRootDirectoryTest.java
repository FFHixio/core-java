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

package io.spine.server.aggregate;

import com.google.common.truth.OptionalSubject;
import io.spine.server.aggregate.given.part.TaskRepository;
import io.spine.server.aggregate.given.part.TaskRoot;
import io.spine.test.aggregate.task.AggTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth8.assertThat;

@DisplayName("In memory AggregateRootDirectory should")
class InMemoryRootDirectoryTest {

    private AggregateRootDirectory directory;
    private AggregatePartRepository<?, ?, ?, ?> repository;

    @BeforeEach
    void setUp() {
        directory = new InMemoryRootDirectory();
        repository = new TaskRepository();
    }

    @Test
    @DisplayName("find a registered repository")
    void findRepository() {
        directory.register(repository);
        Optional<? extends AggregatePartRepository<?, ?, ?, ?>> found =
                directory.findPart(TaskRoot.class, AggTask.class);
        OptionalSubject assertRepository = assertThat(found);
        assertRepository.isPresent();
        assertRepository.hasValue(repository);
    }

    @Test
    @DisplayName("not find a non-registered repository")
    void notFindRepository() {
        Optional<? extends AggregatePartRepository<?, ?, ?, ?>> found =
                directory.findPart(TaskRoot.class, AggTask.class);
        OptionalSubject assertRepository = assertThat(found);
        assertRepository.isEmpty();
    }
}
