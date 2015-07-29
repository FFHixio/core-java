/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.util;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.lang.UnknownTypeInAnyException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class for reading real proto class names from properties file.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class TypeToClassMap {

    /**
     * File, containing Protobuf messages' typeUrls and their appropriate class names.
     * Is generated with Gradle during build process.
     */
    private static final String PROPERTIES_FILE_NAME = "protos.properties";

    private static final Map<TypeName, ClassName> namesMap = new HashMap<>();

    static {
        Properties properties = new Properties();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream resourceStream = classLoader.getResourceAsStream(PROPERTIES_FILE_NAME);

        try {
            properties.load(resourceStream);
        } catch (IOException e) {
            //NOP
        }

        for (String key : properties.stringPropertyNames()) {
            final TypeName typeName = TypeName.of(key);
            final ClassName className = ClassName.of(properties.getProperty(key));
            namesMap.put(typeName, className);
        }
    }

    private TypeToClassMap() {
    }

    /**
     * Retrieves compiled proto's java class name by proto type url
     * to be used to parse {@link Message} from {@link Any}.
     *
     * @param protoType {@link Any} type url
     * @return Java class name
     */
    public static ClassName get(TypeName protoType) {
        if (!namesMap.containsKey(protoType)) {
            throw new UnknownTypeInAnyException(protoType.value());
        }
        final ClassName result = namesMap.get(protoType);
        return result;
    }
}
