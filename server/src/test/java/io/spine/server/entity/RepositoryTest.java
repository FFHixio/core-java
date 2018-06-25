/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.entity;

import com.google.common.base.Predicates;
import io.spine.core.TenantId;
import io.spine.server.BoundedContext;
import io.spine.server.entity.given.RepositoryTestEnv.ProjectEntity;
import io.spine.server.entity.given.RepositoryTestEnv.RepoForEntityWithUnsupportedId;
import io.spine.server.entity.given.RepositoryTestEnv.TestRepo;
import io.spine.server.model.ModelError;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.server.tenant.TenantAwareFunction0;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.test.entity.ProjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.core.given.GivenTenantId.newUuid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@DisplayName("Repository should")
class RepositoryTest {

    private BoundedContext boundedContext;
    private Repository<ProjectId, ProjectEntity> repository;
    private StorageFactory storageFactory;
    private TenantId tenantId;

    private static ProjectId createId(String value) {
        return ProjectId.newBuilder()
                        .setId(value)
                        .build();
    }

    @BeforeEach
    void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .setMultitenant(true)
                                       .build();
        repository = new TestRepo();
        storageFactory = boundedContext.getStorageFactory();
        tenantId = newUuid();
    }

    @AfterEach
    void tearDown() throws Exception {
        boundedContext.close();
    }

    @Test
    @DisplayName("check for entity ID class")
    void checkEntityIdType() {
        assertThrows(ModelError.class, () -> new RepoForEntityWithUnsupportedId().getIdClass());
    }

    @Test
    @DisplayName("report unregistered on init")
    void beUnregisteredOnInit() {
        assertFalse(new TestRepo().isRegistered());
    }

    @Test
    @DisplayName("not allow getting BoundedContext before registration")
    void notGetBcIfUnregistered() {
        assertThrows(IllegalStateException.class, () -> new TestRepo().getBoundedContext());
    }

    @Test
    @DisplayName("reject repeated storage initialization")
    void notInitStorageRepeatedly() {
        repository.initStorage(storageFactory);

        assertThrows(IllegalStateException.class, () -> repository.initStorage(storageFactory));
    }

    @Test
    @DisplayName("have no storage upon creation")
    void haveNoStorageOnCreation() {
        assertFalse(repository.isStorageAssigned());
    }

    @Test
    @DisplayName("prohibit obtaining unassigned storage")
    void notGetUnassignedStorage() {
        assertThrows(IllegalStateException.class, () -> repository.getStorage());
    }

    @Test
    @DisplayName("init storage with factory")
    void initStorageWithFactory() {
        repository.initStorage(storageFactory);
        assertTrue(repository.isStorageAssigned());
        assertNotNull(repository.getStorage());
    }

    @Test
    @DisplayName("close storage on close")
    void closeStorageOnClose() {
        repository.initStorage(storageFactory);

        final RecordStorage<?> storage = (RecordStorage<?>) repository.getStorage();
        repository.close();

        assertTrue(storage.isClosed());
        assertFalse(repository.isStorageAssigned());
    }

    @Test
    @DisplayName("disconnect from storage on close")
    void disconnectStorageOnClose() {
        repository.initStorage(storageFactory);

        repository.close();
        assertFalse(repository.isStorageAssigned());
    }

    /**
     * Creates three entities in the repository.
     */
    private void createAndStoreEntities() {
        final TenantAwareOperation op = new TenantAwareOperation(tenantId) {
            @Override
            public void run() {
                repository.initStorage(storageFactory);

                createAndStore("Eins");
                createAndStore("Zwei");
                createAndStore("Drei");
            }
        };
        op.execute();
    }

    @Test
    @DisplayName("iterate over entities")
    void iterateOverEntities() {
        createAndStoreEntities();

        final int numEntities = new TenantAwareFunction0<Integer>(tenantId) {
            @Override
            public Integer apply() {
                final List<ProjectEntity> entities = newArrayList(getIterator(tenantId));
                return entities.size();
            }
        }.execute();

        assertEquals(3, numEntities);
    }

    private Iterator<ProjectEntity> getIterator(TenantId tenantId) {
        final TenantAwareFunction0<Iterator<ProjectEntity>> op =
                new TenantAwareFunction0<Iterator<ProjectEntity>>(tenantId) {
                    @Override
                    public Iterator<ProjectEntity> apply() {
                        return repository.iterator(Predicates.alwaysTrue());
                    }
                };
        return op.execute();
    }

    @Test
    @DisplayName("not allow removal in entities iterator")
    void notAllowRemovalInIterator() {
        createAndStoreEntities();
        final Iterator<ProjectEntity> iterator = getIterator(tenantId);
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    private void createAndStore(String entityId) {
        ProjectEntity entity = repository.create(createId(entityId));
        repository.store(entity);
    }
}
