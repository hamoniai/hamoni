import ai.hamoni.gradle.util.getGitRepository
import org.antora.gradle.AntoraTask

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

plugins {
    id("base")
    id("base-conventions")
    alias(libs.plugins.docs.antora)
}

/**
 * Connect the Antora task into the build.
 */
tasks.named("assemble") {
    dependsOn("antora")
}

/**
 * Configure the Antora build
 */
antora {
    playbook = file("antora-playbook.yml")
    packages = mapOf(
        "@antora/collector-extension" to "latest",
        "asciidoctor-kroki" to "latest-0"
    )

    // References in need to be static.
    setOptions(
        mapOf(
            "attributes" to mapOf(
                "project-version" to project.version.toString(),
                "hamoni-org-url" to "https://github.com/hamoniai",
                "design-system-url" to "https://github.com/hamoniai/hamoni-design-system",
                "openapi-url" to "#",
            )
        )
    )
}

/**
 * Need to configure each of the Antora tasks with options individually because the Git URL comes from a provider
 * so we need to supply at task configuration.
 */
tasks.withType<AntoraTask>().configureEach {
    val gitUrlProvider = project.getGitRepository()
    args.addAll(
        gitUrlProvider.map { gitUrl ->
            listOf(
                "--attribute",
                "project-clone-url=${gitUrl}",
                "--attribute",
                "project-base-url=${gitUrl.replace(".git$".toRegex(), "")}",
            )
        }
    )
}

/**
 * Additional clean tasks
 */
val cleanNodeTask = project.tasks.register<Delete>("cleanNode") {
    val base = project.layout.projectDirectory
    delete(
        base.dir(".gradle"),
    )
}
project.tasks.named("clean").configure {
    dependsOn(cleanNodeTask)
}
