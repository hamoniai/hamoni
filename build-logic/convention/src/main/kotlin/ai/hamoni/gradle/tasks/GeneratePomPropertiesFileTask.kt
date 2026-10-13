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