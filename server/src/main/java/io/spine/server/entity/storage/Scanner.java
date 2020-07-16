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

package io.spine.server.entity.storage;

import com.google.common.collect.ImmutableSet;
import io.spine.base.EntityState;
import io.spine.query.CustomColumn;
import io.spine.query.EntityColumn;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;

import java.lang.reflect.Method;
import java.util.Set;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Scans and extracts the {@link EntityRecordSpec specification} of the stored record
 * from the passed {@link io.spine.server.entity.Entity Entity}.
 */
final class Scanner<I, S extends EntityState<I>, E extends Entity<I, S>>  {

    /**
     * The name of the nested class generated by the Spine compiler as a container of
     * the entity column definitions.
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")   // coincidental duplication
    private static final String COLS_NESTED_CLASSNAME = "Column";

    /**
     * The name of the method inside the column container class generated by the Spine compiler.
     *
     * <p>The method returns all the definitions of the columns for this state class.
     */
    private static final String COL_DEFS_METHOD_NAME = "columnDefinitions";

    /**
     * The target entity class.
     */
    private final EntityClass<E> entityClass;

    Scanner(EntityClass<E> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Obtains the {@linkplain SystemColumn system} columns of the class.
     */
    ImmutableSet<CustomColumn<E, ?>> systemColumns() {
        ImmutableSet.Builder<CustomColumn<E, ?>> columns = ImmutableSet.builder();
        Class<?> entityClazz = entityClass.value();
        Method[] methods = entityClazz.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(SystemColumn.class)) {
                SystemColumn annotation = method.getAnnotation(SystemColumn.class);
                EntityRecordColumn columnImpl = annotation.impl();
                @SuppressWarnings("unchecked")      // Ensured by the declaration.
                CustomColumn<E, ?> column = (CustomColumn<E, ?>) columnImpl.get();
                columns.add(column);
            }
        }
        return columns.build();
    }

    /**
     * Obtains the {@linkplain SimpleColumn entity-state-based} columns of the class.
     */
    @SuppressWarnings("OverlyBroadCatchBlock")  // Treating all exceptions equally.
    ImmutableSet<EntityColumn<S, ?>> simpleColumns() {
        Class<? extends EntityState<?>> stateClass = entityClass.stateClass();
        Class<?> columnClass = findColumnsClass(stateClass);
        try {
            Method getDefinitions = columnClass.getDeclaredMethod(COL_DEFS_METHOD_NAME);
            @SuppressWarnings("unchecked")  // ensured by the Spine code generation.
            Set<EntityColumn<S, ?>> columns =
                    (Set<EntityColumn<S, ?>>) getDefinitions.invoke(null);
            return ImmutableSet.copyOf(columns);
        } catch (Exception e) {
            throw newIllegalStateException(
                    e,
                    "Error fetching the declared columns by invoking the `%s.%s()` method" +
                            " of the entity state type `%s`.",
                    COLS_NESTED_CLASSNAME, COL_DEFS_METHOD_NAME, stateClass.getName());
        }
    }

    private static Class<?> findColumnsClass(Class<? extends EntityState<?>> stateClass) {
        Class<?>[] innerClasses = stateClass.getDeclaredClasses();
        Class<?> columnClass = null;
        for (Class<?> aClass : innerClasses) {
            if(aClass.getSimpleName().equals(COLS_NESTED_CLASSNAME)) {
                columnClass = aClass;
            }
        }
        if(columnClass == null) {
            throw newIllegalStateException(
                    "Cannot find the nested `%s` class which declares the entity column" +
                            " for the entity state type `%s`.",
                    COLS_NESTED_CLASSNAME, stateClass.getName());
        }
        return columnClass;
    }
}
