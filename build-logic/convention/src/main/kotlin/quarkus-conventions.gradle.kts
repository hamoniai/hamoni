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

val Project.libs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Conventions for Kotlin projects. We will assume that all Quarkus projects use Kotlin for simplicity.
 */
plugins {
    id("kotlin-conventions")
    //id("aws-conventions")
}

/**
 * A custom configuration for registering the Java agent.
 */
val byteBuddyAgent = configurations.register("byteBuddyAgent")

/**
 * Dependencies for Quarkus
 */
dependencies {
    // Quarkus BOM
    val quarkusBom = libs.findLibrary("platform.quarkus.bom").get()
    implementation(enforcedPlatform(quarkusBom))
    testImplementation(enforcedPlatform(quarkusBom))
    annotationProcessor(enforcedPlatform(quarkusBom))
    kapt(enforcedPlatform(quarkusBom))
    byteBuddyAgent(enforcedPlatform(quarkusBom))

    // Quarkus AWS extensions BOM
    val quarkusAwsExtBom = libs.findLibrary("platform.quarkus.ext.aws.bom").get()
    implementation(enforcedPlatform(quarkusAwsExtBom))
    testImplementation(enforcedPlatform(quarkusAwsExtBom))

    // For logging. Logging classes must be in Java due to a bug in JBoss Logging.
    annotationProcessor("org.jboss.logging:jboss-logging-processor")
    compileOnly("org.jboss.logging:jboss-logging-annotations")

    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-rest-kotlin")
    implementation("io.quarkus:quarkus-rest-kotlin-serialization")
    implementation("io.quarkus:quarkus-security")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Messaging: compile only as a real implementation needs to provide the annotations
    compileOnly("io.smallrye.reactive:smallrye-reactive-messaging-api")

    val extMockk = libs.findLibrary("platform.quarkus.ext.mockk").get()
    testImplementation(extMockk)
    testImplementation("io.quarkus:quarkus-test-vertx")

    // For mocking
    byteBuddyAgent("net.bytebuddy:byte-buddy-agent") { isTransitive = false }
}

/**
 * Enable the Byte Buddy agent
 */
tasks.withType<Test>().configureEach {
    jvmArgs("-javaagent:${byteBuddyAgent.get().incoming.artifacts.artifactFiles.asPath}")
}
