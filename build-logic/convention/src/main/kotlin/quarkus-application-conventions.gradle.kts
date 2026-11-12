/*
 * Hamoni: Cloud workflow platform
 * Copyright (C) 2026  Hamoni Limited
 *
 * Unless you have received this program directly from Hamoni Limited pursuant to
 * the terms of a commercial license agreement with Hamoni Limited, then this
 * program is licensed to you under the terms of version 3 of the GNU Affero
 * General Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import ai.hamoni.gradle.util.getOverridableProperty
import gradle.kotlin.dsl.accessors._2efa669337583f180c8ae88f20ef351a.annotationProcessor
import gradle.kotlin.dsl.accessors._2efa669337583f180c8ae88f20ef351a.compileOnly
import gradle.kotlin.dsl.accessors._2efa669337583f180c8ae88f20ef351a.implementation
import io.quarkus.gradle.tasks.QuarkusDev

/**
 * Conventions for Kotlin extension projects. We will assume that all Quarkus projects use Kotlin for simplicity.
 * The quarkus plugin can only be applied in test for non-core modules, otherwise there will be build errors.
 */
plugins {
    id("quarkus-conventions")
    id("io.quarkus")
    id("org.kordamp.gradle.jandex") // TODO: this needs replacing, the outputs are not Configuration Cache compliant.
}

/**
 * Dependencies for Quarkus
 */
dependencies {
    // Extensions common to all Quarkus applications
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-rest-kotlin")
    implementation("io.quarkus:quarkus-rest-kotlin-serialization")
    implementation("io.quarkus:quarkus-security")
    implementation("io.quarkus:quarkus-smallrye-context-propagation")

    // Messaging: compile only as a real implementation needs to provide the annotations
    compileOnly("io.smallrye.reactive:smallrye-reactive-messaging-api")

    // For logging. Logging classes must be in Java due to a bug in JBoss Logging.
    annotationProcessor("org.jboss.logging:jboss-logging-processor")
    compileOnly("org.jboss.logging:jboss-logging-annotations")

    // Sync and async client for AWS services
    //implementation("software.amazon.awssdk:apache-client")
    //implementation("software.amazon.awssdk:netty-nio-client")
}

/**
 * Very special handling because of a problem in the Quarkus multi-tenant setup. Have to transform some classes
 * before they will allow multi-tenant hibernate.
 */
val agentJar = rootProject.layout.projectDirectory.dir("build-logic/libs").file("quarkus-multi-tenant-agent.jar")
quarkus {
    buildForkOptions {
        jvmArgs.add("-javaagent:${agentJar}")
    }
}

/**
 * Javadoc needs Jandex to complete first
 */
tasks.withType(Javadoc::class.java).configureEach {
    dependsOn(tasks.named("jandex"))
}

/**
 * Configure Quarkus dev to use the correct AWS SSO profiles
 */
tasks.withType<QuarkusDev>().configureEach {
    //getOverridableProperty("aws.default.profile")?.let {
    //    jvmArgs = listOf("-Daws.profile=$it")
    //}
    jvmArguments.add("-javaagent:${agentJar}")
    notCompatibleWithConfigurationCache("Plugin not compatible")
}

/**
 * Disable the Quarkus build tasks for non-main projects. The main project will be in a parent directory of the same
 * name.
 */
if (project.projectDir.name != project.projectDir.parentFile.name) {
    val quarkusDisabledTasks = listOf("quarkusBuild", "quarkusBuildAppModel", "quarkusAppPartsBuild", "quarkusDependenciesBuild")
    tasks.named { it in quarkusDisabledTasks }.configureEach {
        enabled = false
    }
}