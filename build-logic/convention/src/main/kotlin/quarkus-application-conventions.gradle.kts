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
    // Sync and async client for AWS services
    //implementation("software.amazon.awssdk:apache-client")
    //implementation("software.amazon.awssdk:netty-nio-client")
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