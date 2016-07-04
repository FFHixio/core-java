/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.protobuf;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.spine3.Internal;
import org.spine3.validate.internal.EntityType;

import javax.annotation.Nullable;
import java.util.Properties;
import java.util.Set;

import static org.spine3.io.IoUtil.loadAllProperties;

/**
 * A map from Protobuf package to {@link EntityType} (the package contains files for this entity type (aggregate, etc)).
 *
 * <p>If there is a {@code state_of} option in the entity state Protobuf message definition,
 * all files in the current package are considered belonging to the specified type of the entity
 * (containing entity state, commands, events, etc).
 *
 * <p>This information is needed when validating a command to check if it is sent to an entity
 * (to check entity ID field in the command message).
 *
 * @author Alexander Litus
 */
@Internal
public class EntityPackagesMap {

    /**
     * A path to a file which contains Protobuf packages and appropriate {@link EntityType}s.
     * Is generated by Gradle during build process.
     */
    private static final String PROPS_FILE_PATH = "entities.properties";

    /**
     * A map from Protobuf package to {@link EntityType}.
     * The package contains files for this type of entity (aggregate, etc).
     *
     * <p>Example:
     * <p>{@code com.example.order} - {@code AGGREGATE}
     */
    private static final ImmutableMap<String, EntityType> packagesMap = buildPackagesMap();

    private EntityPackagesMap() {}

    /**
     * Returns an entity type for the Protobuf package.
     *
     * @param protoPackage a protobuf package name where entity files are located
     * @return a type of the entity to which the package belongs
     */
    @Nullable
    public static EntityType get(String protoPackage) {
        final EntityType result = packagesMap.get(protoPackage);
        return result;
    }

    /**
     * Returns {@code true} if the map contains an entity type for a Protobuf package, {@code false} otherwise.
     *
     * @param protoPackage a protobuf package name where entity files are located
     * @return {@code true} if the package belongs to an entity
     */
    public static boolean contains(String protoPackage) {
        final EntityType result = get(protoPackage);
        final boolean contains = result != null;
        return contains;
    }

    private static ImmutableMap<String, EntityType> buildPackagesMap() {
        final ImmutableMap.Builder<String, EntityType> builder = ImmutableMap.builder();
        final ImmutableSet<Properties> propertiesSet = loadAllProperties(PROPS_FILE_PATH);
        for (Properties properties : propertiesSet) {
            putTo(builder, properties);
        }
        return builder.build();
    }

    private static void putTo(ImmutableMap.Builder<String, EntityType> map, Properties properties) {
        final Set<String> protoPackages = properties.stringPropertyNames();
        for (String protoPackage : protoPackages) {
            final String entityTypeStr = properties.getProperty(protoPackage);
            final EntityType entityType = EntityType.valueOf(entityTypeStr);
            map.put(protoPackage, entityType);
        }
    }
}
