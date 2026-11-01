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

package ai.hamoni.gradle.util

import ai.hamoni.gradle.GITHUB_GIT_PREFIX
import ai.hamoni.gradle.GITHUB_HTTPS_PREFIX
import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * Create an overridable property which prefers: environment -> Gradle property -> project property
 */
fun Project.getOverridableProperty(key: String): String? {
    val envKey = key.uppercase().replace(".", "_")
    return System.getenv(envKey) ?: providers.systemProperty(key).orNull ?: project.findProperty(key) as String?
}

/**
 * Get the git repository for a project. Cannot access 'Project' without breaking the configuration cache.
 */
fun Project.getGitRepository(mapToHttps: Boolean = true): Provider<String> {
    return this.providers.exec {
        commandLine("git", "config", "--get", "remote.origin.url")
    }.standardOutput.asText.map { url ->
        url.trim().let {
            if (mapToHttps && it.startsWith(GITHUB_GIT_PREFIX)) {
                it.replaceFirst(GITHUB_GIT_PREFIX, GITHUB_HTTPS_PREFIX)
            } else {
                it
            }
        }
    }
}