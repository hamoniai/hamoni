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

package ai.hamoni.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import javax.inject.Inject
import kotlin.io.path.createDirectories

abstract class GeneratePomPropertiesFileTask @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory
) : DefaultTask() {

    @get:OutputFile
    val outputFile: RegularFileProperty = objectFactory.fileProperty()
        .convention(project.layout.buildDirectory.map { it.dir("tmp/${this.name}").file("pom.properties") })

    @get:Input
    val projectName = objectFactory.property(String::class.java)
        .convention(providerFactory.provider { project.name })

    @get:Input
    val projectGroup = objectFactory.property(String::class.java)
        .convention(providerFactory.provider { project.group.toString() })

    @get:Input
    val projectVersion = objectFactory.property(String::class.java)
        .convention(providerFactory.provider { project.version.toString() })

    @TaskAction
    fun execute() {
        val properties = Properties()
        properties.setProperty("artifactId", projectName.get())
        properties.setProperty("groupId", projectGroup.get())
        properties.setProperty("version", projectVersion.get())

        outputFile.asFile.get().toPath().parent.createDirectories()
        OutputStreamWriter(Files.newOutputStream(outputFile.get().asFile.toPath()), StandardCharsets.UTF_8).use {
            properties.store(it, null)
        }
    }
}