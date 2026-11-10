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

includeBuild("build-logic")
rootProject.name="hamoni"

/**
 * Find all projects under a path and add them
 */
fun File.addProjects() {
    this.walkTopDown()
        .maxDepth(2)
        .onEnter { file ->
            !(file.name.startsWith(".") || file.name == "build" || file.name == "gradle")
        }
        .filter { file ->
            file.name == "build.gradle.kts"
        }
        .forEach { buildFile ->
            val projectDir = buildFile.parentFile
            val projectName = ":${this.name}:${projectDir.name}"
            include(projectName)
            project(projectName).projectDir = projectDir
        }
}

val rootProjectDir = rootProject.projectDir
val extensionsDir = File(rootProjectDir, "extensions")
val deploymentSuffix = "-deployment"

// Add all extensions dynamically.
extensionsDir.walkTopDown()
    .maxDepth(2)
    .onEnter { file ->
        file == extensionsDir || file.name.endsWith(deploymentSuffix)
    }
    .filter { file ->
        file.name == "build.gradle.kts" && file.parentFile != rootProjectDir
    }
    .forEach { buildFile ->
        val depProjectDir = buildFile.parentFile
        val depProjectName = ":${extensionsDir.name}:${depProjectDir.name}"
        val impProjectDir = File(buildFile.parentFile.parentFile, buildFile.parentFile.name.removeSuffix(deploymentSuffix))
        val impProjectName = ":${extensionsDir.name}:${depProjectDir.name.removeSuffix(deploymentSuffix)}"

        include(depProjectName)
        project(depProjectName).projectDir = depProjectDir
        include(impProjectName)
        project(impProjectName).projectDir = impProjectDir
    }

// Add all projects found dynamically, but collapse all to the top level.
File(rootProjectDir, "common").addProjects()
File(rootProjectDir, "control-room").addProjects()
File(rootProjectDir, "process-runner").addProjects()
