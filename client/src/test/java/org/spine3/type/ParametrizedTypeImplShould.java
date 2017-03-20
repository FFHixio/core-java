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

package org.spine3.type;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Illia Shepilov
 */
public class ParametrizedTypeImplShould {

    @Test
    public void return_raw_type() {
        final ParameterizedType type = ParametrizedTypeImpl.make(List.class,
                                                                 new Type[]{Integer.class});
        assertEquals(List.class, type.getRawType());
    }

    @Test
    public void return_owner_type() {
        final ParameterizedType type = ParametrizedTypeImpl.make(List.class,
                                                                 new Type[]{Integer.class},
                                                                 ParametrizedTypeImpl.class);
        assertEquals(ParametrizedTypeImpl.class, type.getOwnerType());
    }

    @Test
    public void return_type_arguments() {
        final Type[] typeArguments = {Integer.class};
        final ParameterizedType type = ParametrizedTypeImpl.make(List.class,
                                                                 typeArguments);
        assertArrayEquals(typeArguments, type.getActualTypeArguments());
    }

    @Test
    public void have_smart_equals() {
        final ParameterizedType firstType = ParametrizedTypeImpl.make(List.class,
                                                                      new Type[]{Integer.class},
                                                                      ParametrizedTypeImpl.class);
        final ParameterizedType secondType = ParametrizedTypeImpl.make(List.class,
                                                                       new Type[]{Integer.class},
                                                                       ParametrizedTypeImpl.class);
        new EqualsTester().addEqualityGroup(firstType, secondType)
                          .testEquals();
    }
}
