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

/**
 * Turn a plugin version library reference into a dependency
 */
fun Provider<PluginDependency>.toDependency(): Provider<Dependency> {
    return this.map { project.dependencies.create("${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}") }
}

/**
 * Plugins used by the build script
 */
plugins {
    `kotlin-dsl`
}

/**
 * Default repositories
 */
repositories {
    mavenCentral()
    gradlePluginPortal()
}

/**
 * Dependencies
 */
dependencies {
    // Set the versions of plugins used by the build
    implementation(libs.plugins.kotlin.core.toDependency())
    implementation(libs.plugins.kotlin.allopen.toDependency())
    implementation(libs.plugins.kotlin.serialization.toDependency())
    implementation(libs.plugins.platform.quarkus.application.toDependency())
    implementation(libs.plugins.platform.quarkus.extension.toDependency())
    implementation(libs.plugins.util.jandex.toDependency())

    // Support for integrating with AWS directly in the build. The AWS services BOM imports the AWS BOM
    implementation(enforcedPlatform(libs.platform.quarkus.bom))
    //implementation("software.amazon.awssdk:sts:2.46.17")
    //implementation("software.amazon.awssdk:sso:2.46.17")
    //implementation("software.amazon.awssdk:ssooidc:2.46.17")
}