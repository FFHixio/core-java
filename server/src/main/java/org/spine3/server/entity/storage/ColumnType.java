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

package org.spine3.server.entity.storage;

import org.spine3.annotations.SPI;

/**
 * An interface for handling type conversion for the Storage Fields.
 *
 * @param <J> the Java type represented by the column
 * @param <S> the "store as" type of the column
 * @param <R> the type of the record in the database, which holds a single cortege of data and
 *            is consumed by the database upon write
 * @param <C> the type of the column identifier in the {@code R}
 */
@SPI
public interface ColumnType<J, S, R, C> {

    /**
     * Converts the Storage Field specified in
     * the {@link org.spine3.server.entity.Entity Entity} declaration to the type in which the Field
     * is stored.
     *
     * <p>Common example is converting
     * {@link com.google.protobuf.Timestamp com.google.protobuf.Timestamp} into
     * {@link java.util.Date java.util.Date}.
     *
     * @param fieldValue the Storage Field of the initial type
     * @return the Storage Field of the "store as" type
     */
    S convertColumnValue(J fieldValue);

    /**
     * Set the Storage Field value to the database record type.
     *
     * <p>Common example is setting a value to
     * a {@link java.sql.PreparedStatement PreparedStatement} instance into a determined position.
     *
     * @param storageRecord    the database record
     * @param value            the value to store
     * @param columnIdentifier the identifier of the column, e.g. its index
     */
    void setColumnValue(R storageRecord, S value, C columnIdentifier);
}
