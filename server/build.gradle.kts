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

import io.spine.gradle.internal.Deps

val spineBaseVersion: String by extra

dependencies {
    api(project(":client"))

    implementation(Deps.grpc.protobuf)
    implementation(Deps.grpc.core)

    testAnnotationProcessor(Deps.build.autoService.processor)
    testCompileOnly(Deps.build.autoService.annotations)
    testImplementation(Deps.grpc.nettyShaded)
    testImplementation("io.spine:spine-testlib:$spineBaseVersion")
    testImplementation(project(path = ":core", configuration = "testArtifacts"))
    testImplementation(project(path = ":client", configuration = "testArtifacts"))
    testImplementation(project(":testutil-server"))
}

apply {
    from(Deps.scripts.testArtifacts(project))
    from(Deps.scripts.publishProto(project))
}

// Copies the documentation files to the Javadoc output folder.
// Inspired by https://discuss.gradle.org/t/do-doc-files-work-with-gradle-javadoc/4673
tasks.javadoc {
    doLast {
        copy {
            from("src/main/docs")
            into("$buildDir/docs/javadoc")
        }
    }
}
