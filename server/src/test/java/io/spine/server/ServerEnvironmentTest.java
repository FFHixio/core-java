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

package io.spine.server;

import io.spine.server.sharding.Sharding;
import io.spine.server.sharding.UniformAcrossAllShards;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.DeploymentDetector.APP_ENGINE_ENVIRONMENT_DEVELOPMENT_VALUE;
import static io.spine.server.DeploymentDetector.APP_ENGINE_ENVIRONMENT_PATH;
import static io.spine.server.DeploymentDetector.APP_ENGINE_ENVIRONMENT_PRODUCTION_VALUE;
import static io.spine.server.DeploymentType.APPENGINE_CLOUD;
import static io.spine.server.DeploymentType.APPENGINE_EMULATOR;
import static io.spine.server.DeploymentType.STANDALONE;
import static io.spine.server.ServerEnvironment.resetDeploymentType;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings("deprecation") // Need to test deprecated API of `ServerEnvironment`.
@DisplayName("ServerEnvironment utility should")
class ServerEnvironmentTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(ServerEnvironment.class);
    }

    @Test
    @DisplayName("tell when not running under AppEngine")
    void tellIfNotInAppEngine() {
        // Tests are not run by AppEngine by default.
        assertFalse(ServerEnvironment.getInstance().isAppEngine());
    }

    @Test
    @DisplayName("obtain AppEngine version as optional string")
    void getAppEngineVersion() {
        // By default we're not running under AppEngine.
        assertFalse(ServerEnvironment.getInstance()
                                     .appEngineVersion()
                                     .isPresent());
    }

    @Test
    @DisplayName("return disabled sharding by default")
    void returnSingleShardStrategyDefault() {
        assertFalse( ServerEnvironment.getInstance().sharding().enabled());
    }

    @Test
    @DisplayName("allow to customize sharding mechanism")
    void allowToCustomizeShardingStrategy() {
        Sharding newSharding = Sharding.newBuilder()
                                       .setStrategy(UniformAcrossAllShards.forNumber(42))
                                       .build();
        ServerEnvironment environment = ServerEnvironment.getInstance();
        Sharding defaultValue = environment.sharding();
        environment.setSharding(newSharding);
        assertEquals(newSharding, environment.sharding());

        // Restore the default value.
        environment.setSharding(defaultValue);
    }

    @Test
    @DisplayName("tell when not running without any specific server environment")
    void tellIfStandalone() {
        // Tests are not run by AppEngine by default.
        assertEquals(STANDALONE, ServerEnvironment.getDeploymentType());
    }

    @Nested
    @DisplayName("when running on App Engine cloud infrastructure")
    class OnProdAppEngine extends WithAppEngineEnvironment {

        OnProdAppEngine() {
            super(APP_ENGINE_ENVIRONMENT_PRODUCTION_VALUE);
        }

        @Test
        @DisplayName("obtain AppEngine environment GAE cloud infrastructure server environment")
        void receivesCloudEnvironment() {
            assertEquals(APPENGINE_CLOUD, ServerEnvironment.getDeploymentType());
        }

        @Test
        @DisplayName("cache the property value")
        void cachesValue() {
            assertEquals(APPENGINE_CLOUD, ServerEnvironment.getDeploymentType());
            setGaeEnvironment("Unrecognized Value");
            assertEquals(APPENGINE_CLOUD, ServerEnvironment.getDeploymentType());
        }
    }

    @Nested
    @DisplayName("when running on App Engine local server")
    class OnDevAppEngine extends WithAppEngineEnvironment {

        OnDevAppEngine() {
            super(APP_ENGINE_ENVIRONMENT_DEVELOPMENT_VALUE);
        }

        @Test
        @DisplayName("obtain AppEngine environment GAE local dev server environment")
        void receivesEmulatorEnvironment() {
            assertEquals(APPENGINE_EMULATOR, ServerEnvironment.getDeploymentType());
        }
    }

    @Nested
    @DisplayName("when running with invalid App Engine environment property")
    class InvalidGaeEnvironment extends WithAppEngineEnvironment {

        InvalidGaeEnvironment() {
            super("InvalidGaeEnvironment");
        }

        @Test
        @DisplayName("receive STANDALONE deployment type")
        void receivesStandalone() {
            assertEquals(STANDALONE, ServerEnvironment.getDeploymentType());
        }
    }

    @SuppressWarnings({
            "AccessOfSystemProperties" /* Testing the configuration loaded from System properties. */,
            "AbstractClassWithoutAbstractMethods" /* A test base with setUp and tearDown. */
    })
    abstract class WithAppEngineEnvironment {

        private final String targetEnvironment;

        private String initialValue;

        WithAppEngineEnvironment(String targetEnvironment) {
            this.targetEnvironment = targetEnvironment;
        }

        @BeforeEach
        void setUp() {
            initialValue = System.getProperty(APP_ENGINE_ENVIRONMENT_PATH);
            setGaeEnvironment(targetEnvironment);
            resetDeploymentType();
        }

        @AfterEach
        void tearDown() {
            if (initialValue == null) {
                System.clearProperty(APP_ENGINE_ENVIRONMENT_PATH);
            } else {
                setGaeEnvironment(initialValue);
            }
            resetDeploymentType();
        }

        void setGaeEnvironment(String value) {
            System.setProperty(APP_ENGINE_ENVIRONMENT_PATH, value);
        }
    }
}
