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

package io.spine.server.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.client.EntityId;
import io.spine.client.OrderBy;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.server.BoundedContext;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityColumnCache;
import io.spine.server.entity.storage.EntityQueries;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordReadRequest;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validate.Validate.checkValid;

/**
 * The base class for repositories that store entities as records.
 *
 * <p>Such a repository is backed by {@link RecordStorage}.
 * Entity states are stored as {@link EntityRecord}s.
 *
 * @param <S>
 *         the type of entity state messages
 */
public abstract class RecordBasedRepository<I, E extends Entity<I, S>, S extends Message>
        extends Repository<I, E> {

    /** Creates a new instance. */
    protected RecordBasedRepository() {
        super();
    }

    /**
     * Obtains {@link EntityFactory} associated with this repository.
     */
    protected abstract EntityFactory<E> entityFactory();

    /**
     * Obtains {@link StorageConverter} associated with this repository.
     */
    protected abstract StorageConverter<I, E, S> storageConverter();

    /**
     * Ensures that the repository has the storage.
     *
     * @return storage instance
     * @throws IllegalStateException
     *         if the storage is null
     */
    protected RecordStorage<I> recordStorage() {
        @SuppressWarnings("unchecked") // OK as we control the creation in createStorage().
        RecordStorage<I> storage = (RecordStorage<I>) storage();
        return storage;
    }

    /**
     * Initializes the repository by caching {@link Column} definitions of
     * the {@link Entity} class managed by this repository.
     *
     * @param context
     *         the Bounded Context of this repository
     */
    @Override
    @OverridingMethodsMustInvokeSuper
    public void registerWith(BoundedContext context) {
        checkNotNull(context);
        super.registerWith(context);
        cacheEntityColumns();
    }

    @Override
    public E create(I id) {
        E result = entityFactory().create(id);
        return result;
    }

    @Override
    public void store(E entity) {
        EntityRecordWithColumns record = toRecord(entity);
        RecordStorage<I> storage = recordStorage();
        storage.write(entity.id(), record);
    }

    @Override
    public Iterator<E> iterator(Predicate<E> filter) {
        Iterator<E> allEntities = loadAll(ResponseFormat.getDefaultInstance());
        Iterator<E> result = filter(allEntities, filter::test);
        return result;
    }

    @Override
    protected RecordStorage<I> createStorage() {
        StorageFactory sf = defaultStorageFactory();
        RecordStorage<I> result = sf.createRecordStorage(context().spec(), entityClass());
        return result;
    }

    /**
     * Stores {@linkplain Entity Entities} in bulk.
     *
     * <p>Note: The storage must be assigned before calling this method.
     *
     * @param entities the {@linkplain Entity Entities} to store
     */
    public void store(Collection<E> entities) {
        Map<I, EntityRecordWithColumns> records = newHashMapWithExpectedSize(entities.size());
        for (E entity : entities) {
            EntityRecordWithColumns recordWithColumns = toRecord(entity);
            records.put(entity.id(), recordWithColumns);
        }
        recordStorage().write(records);
    }

    /**
     * Finds an entity with the passed ID.
     *
     * @param id the ID of the entity to find
     * @return the entity or {@link Optional#empty()} if there is no entity with such ID
     */
    @Override
    public Optional<E> find(I id) {
        Optional<EntityRecord> record = findRecord(id);
        return record.map(this::toEntity);
    }

    /**
     * Finds an entity with the passed ID even if the entity is
     * {@linkplain WithLifecycle#isActive() active}.
     *
     * @param id the ID of the entity to find
     * @return the entity or {@link Optional#empty()} if there is no entity with such ID,
     *         or the entity is not active
     */
    public Optional<E> findActive(I id) {
        Optional<EntityRecord> record = findRecord(id);
        Optional<E> result = record.filter(WithLifecycle::isActive)
                                   .map(this::toEntity);
        return result;
    }

    /**
     * Finds a record and returns it if its {@link LifecycleFlags} don't make it
     * {@linkplain WithLifecycle#isActive() active}.
     */
    private Optional<EntityRecord> findRecord(I id) {
        RecordStorage<I> storage = recordStorage();
        RecordReadRequest<I> request = new RecordReadRequest<>(id);
        Optional<EntityRecord> found = storage.read(request);
        if (!found.isPresent()) {
            return Optional.empty();
        }
        EntityRecord record = found.get();
        return Optional.of(record);
    }

    /**
     * Loads an entity by the passed ID or creates a new one, if the entity was not found.
     *
     * <p>An entity will be loaded whether its {@linkplain WithLifecycle#isActive() active} or not.
     *
     * <p>The new entity is created if and only if there is no record with the corresponding ID.
     *
     * @param id the ID of the entity to load
     * @return the entity with the specified ID
     */
    protected E findOrCreate(I id) {
        Optional<EntityRecord> record = findRecord(id);
        E result = record.map(this::toEntity)
                         .orElseGet(() -> create(id));
        return result;
    }

    @VisibleForTesting
    public Iterator<E> loadAll(ResponseFormat format) {
        Iterator<EntityRecord> records = loadAllRecords(format);
        Function<EntityRecord, E> toEntity = storageConverter().reverse();
        Iterator<E> result = transform(records, toEntity::apply);
        return result;
    }

    /**
     * Loads all the entities in this repository with IDs,
     * contained within the passed {@code ids} values.
     *
     * <p>Provides a convenience wrapper around multiple invocations of
     * {@link #find(Object)}. Descendants may optimize the execution of this
     * method, choosing the most suitable way for the particular storage engine used.
     *
     * <p>The result only contains those entities which IDs are contained inside
     * the passed {@code ids}. The resulting collection is always returned
     * with no {@code null} values.
     *
     * <p>The order of objects in the result is not guaranteed to be the same
     * as the order of IDs passed as argument.
     *
     * <p>If the IDs contain duplicates, the result may also contain duplicates
     * depending on a particular implementation.
     *
     * <p>Note: The storage must be assigned before calling this method.
     *
     * @param ids
     *         entity IDs to search for
     * @param fieldMask
     *         the entity state fields to load; note that the resulting state must be valid,
     *         otherwise an exception is thrown
     * @return all the entities in this repository with the IDs matching the given {@code Iterable}
     */
    public Iterator<E> loadAll(Iterable<I> ids, FieldMask fieldMask) {
        RecordStorage<I> storage = recordStorage();
        Iterator<@Nullable EntityRecord> records = storage.readMultiple(ids, fieldMask);
        Iterator<EntityRecord> presentRecords = filter(records, Objects::nonNull);
        Function<EntityRecord, E> toEntity = storageConverter().reverse();
        Iterator<E> result = transform(presentRecords, toEntity::apply);
        return result;
    }

    /**
     * Obtains iterator over all present {@linkplain EntityRecord entity records}.
     *
     * <p>The resulting entity states have only the specified in the {@code mask} fields. However,
     * if the {@code mask} is empty, all the fields are retrieved.
     *
     * @return an iterator over all records
     */
    @Internal
    public Iterator<EntityRecord> loadAllRecords(ResponseFormat format) {
        checkNotNull(format);
        RecordStorage<I> storage = recordStorage();
        Iterator<EntityRecord> records = storage.readAll(format);
        return records;
    }

    /**
     * Finds the entities passing the given filters and applies the given {@link FieldMask}
     * to the results.
     *
     * <p>A number of elements to retrieve can be limited to a certain number. Order in
     * which to look for and return results in is specified by the {@link OrderBy}.
     *
     * <p>Field mask is applied according to <a href="https://goo.gl/tW5wIU">FieldMask specs</a>.
     *
     * <p>The field paths in the entity column field filters are specified
     * to contain a single path member - the name of the entity column.
     *
     * <p>The filtering process is delegated to the underlying {@link RecordStorage}.
     *
     * <p>Note: The storage must be assigned before calling this method.
     *
     * @param filters
     *         the entity filters
     * @param format
     *         the expected format of the query response
     * @return all the entities in this repository passed through the filters
     * @see EntityQuery
     */
    public Iterator<E> find(TargetFilters filters, ResponseFormat format) {
        checkNotNull(filters);
        checkNotNull(format);

        Iterator<EntityRecord> records = findRecords(filters, format);
        Function<EntityRecord, E> toEntity = storageConverter().reverse();
        Iterator<E> result = transform(records, toEntity::apply);
        return result;
    }

    /**
     * Obtains iterator over {@linkplain EntityRecord entity records} matching the passed filters.
     *
     * @param filters
     *         entity filters
     *         the mask to apply to the entities
     * @return the iterator over the matching records
     */
    @Internal
    public Iterator<EntityRecord> findRecords(TargetFilters filters, ResponseFormat format) {
        checkNotNull(filters);
        checkValid(filters);
        checkNotNull(format);

        RecordStorage<I> storage = recordStorage();
        EntityQuery<I> entityQuery = EntityQueries.from(filters, storage);
        Iterator<EntityRecord> records = storage.readAll(entityQuery, format);
        return records;
    }

    /**
     * Converts the passed entity into the record.
     */
    @VisibleForTesting
    EntityRecordWithColumns toRecord(E entity) {
        EntityRecord entityRecord = storageConverter().convert(entity);
        checkNotNull(entityRecord);
        EntityRecordWithColumns result =
                EntityRecordWithColumns.create(entityRecord, entity, recordStorage());
        return result;
    }

    private E toEntity(EntityRecord record) {
        E result = storageConverter().reverse()
                                     .convert(record);
        checkNotNull(result);
        return result;
    }

    /**
     * Retrieves the {@link EntityColumnCache} used by this repository's
     * {@linkplain RecordStorage storage}.
     *
     * @return the entity column cache from the storage
     * @throws IllegalStateException
     *         if the {@link EntityColumnCache} is not supported by this repository's storage
     */
    private EntityColumnCache columnCache() {
        return recordStorage().entityColumnCache();
    }

    /**
     * Caches {@link Column} definitions of the {@link Entity} class managed by this repository.
     *
     * <p>The process of caching columns also acts as a check of {@link Column} definitions,
     * because {@linkplain Column columns} with incorrect definitions cannot be retrieved and
     * stored.
     *
     * <p>If {@link Column} definitions are incorrect, the {@link IllegalStateException} is thrown.
     *
     * @throws IllegalStateException
     *         if the entity column definitions are incorrect
     */
    private void cacheEntityColumns() {
        columnCache().ensureColumnsCached();
    }

    /**
     * Transforms an instance of {@link EntityId} into an identifier
     * of the required type.
     *
     * @param <I>
     *         the target type of identifiers
     */
    @VisibleForTesting
    static class EntityIdFunction<I> implements Function<EntityId, I> {

        private final Class<I> expectedIdClass;

        EntityIdFunction(Class<I> expectedIdClass) {
            this.expectedIdClass = expectedIdClass;
        }

        @Override
        public @Nullable I apply(@Nullable EntityId input) {
            checkNotNull(input);
            Any idAsAny = input.getId();

            TypeUrl typeUrl = TypeUrl.ofEnclosed(idAsAny);
            Class messageClass = typeUrl.toJavaClass();
            checkIdClass(messageClass);

            Message idAsMessage = unpack(idAsAny);

            @SuppressWarnings("unchecked")
                // As the message class is the same as expected, the conversion is safe.
            I id = (I) idAsMessage;
            return id;
        }

        private void checkIdClass(Class messageClass) {
            boolean classIsSame = expectedIdClass.equals(messageClass);
            if (!classIsSame) {
                throw newIllegalStateException(
                        "Unexpected ID class encountered: `%s`. Expected: `%s`.",
                        messageClass.getCanonicalName(), expectedIdClass.getCanonicalName()
                );
            }
        }
    }
}
