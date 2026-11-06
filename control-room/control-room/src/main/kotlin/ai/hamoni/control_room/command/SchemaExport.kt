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

package ai.hamoni.control_room.command

import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.runtime.LaunchMode
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.hibernate.SessionFactory
import org.hibernate.cfg.AvailableSettings
import org.hibernate.tool.schema.Action
import org.jboss.logging.Logger
import java.io.BufferedWriter
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories


/**
 * The command that generates the platform and organisation schema migrations
 * from the JPA model.
 *
 * The command is meant for development only. It writes both scripts in one run,
 * platform first and organisation second, so the two schemas cannot drift.
 *
 * The non-default [QuarkusMain] name keeps Quarkus dev mode from treating this
 * command as the application's main entry point.
 */
@QuarkusMain(name = "schema-export")
@ApplicationScoped
@IfBuildProfile("schema-export")
class SchemaExportCommand @Inject constructor(
    private val schemaExportService: SchemaExportService
) : QuarkusApplication {

    private val logger = Logger.getLogger(SchemaExportCommand::class.java)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // The command runs as a normal application; the dev profile is only used for its datasource config.
            LaunchMode.set(LaunchMode.NORMAL)
            Quarkus.run(SchemaExportCommand::class.java, *args)
            // The application otherwise keeps a thread pool alive and prevents the JVM from exiting.
            Quarkus.asyncExit()
        }
    }

    /**
     * Run the export, returning zero when both scripts were written.
     */
    override fun run(vararg args: String): Int {
        schemaExportService.export()
            .forEach { logger.infof("Generated schema migration: %s", it) }
        return 0
    }

}

/**
 * Generate the platform and organisation schema migration scripts from the JPA
 * model.
 */
@ApplicationScoped
@IfBuildProfile("schema-export")
class SchemaExportService @Inject constructor(
    private val sessionFactory: SessionFactory,

    @param:ConfigProperty(name = "quarkus.application.version")
    private val applicationVersion: String,

    @param:ConfigProperty(name = "hamoni.schema-export.db-kind")
    private val databaseKind: String,

    @param:ConfigProperty(name = "hamoni.schema-export.output-directory")
    private val outputDirectory: String
) {
    /**
     * Export both schemas sequentially.
     *
     * @return the files that were written, in export order
     */
    fun export(): List<Path> {
        val migrationVersion = flywayVersion(applicationVersion)
        val outputRoot = Paths.get(outputDirectory).toAbsolutePath().normalize()

        // Always export both scopes in one run. Platform first, organisation second.
        return listOf(SchemaScope.PLATFORM, SchemaScope.ORGANISATION).map { scope ->
            val path = outputRoot
                .resolve(scope.directoryName)
                .resolve(databaseKind)
                .resolve("V${migrationVersion}__schema.sql")

            path.parent.createDirectories()
            path.bufferedWriter(Charsets.UTF_8).use {
                writeHeader(it, scope, migrationVersion)

                val properties = sessionFactory.properties

                // Set the Jakarta-namespace Hibernate properties on the live session factory for this one-shot
                // command. They cannot live in application.properties because Quarkus would run schema generation
                // at startup, and the filter and target change for each database and scope.
                properties[AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION] = Action.NONE
                properties[AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION] = Action.CREATE_ONLY
                properties[AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET] = it
                properties[AvailableSettings.FORMAT_SQL] = true
                properties[AvailableSettings.HBM2DDL_FILTER_PROVIDER] = scope.filterProvider.name

                // `schemaManager.create()` copies the root properties with the database action already set, so
                // the contributor-specific actions must match the root actions or the script target is bypassed.
                listOf("orm").forEach { contributor ->
                    properties["${AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION}.${contributor}"] = Action.NONE
                    properties["${AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION}.${contributor}"] = Action.CREATE_ONLY
                }

                // false: do not create database schemas; Flyway creates the schema and applies the script.
                sessionFactory.schemaManager.create(false)
            }
            path
        }
    }

    /**
     * Write the generated-file header before Hibernate appends the DDL.
     *
     * The same writer is passed to Hibernate as the script target, so the header
     * remains at the top of the file.
     */
    private fun writeHeader(writer: BufferedWriter, scope: SchemaScope, migrationVersion: String) {
        writer.write("""
            -- Generated by the Hamoni schema export command from the JPA model.
            -- Application version: $applicationVersion
            -- Migration version: $migrationVersion
            -- Schema: ${scope.directoryName}
            -- Database: $databaseKind
            -- Do not edit this file by hand. Regenerate it with the schema export command.
            """.trimIndent() + "\n"
        )
    }
}

/**
 * Reduce the application version to a version Flyway accepts.
 *
 * Flyway 12 accepts only digits and dots in a version, so `1.0.0-SNAPSHOT`
 * becomes `1.0.0`. The full application version remains in the generated file's
 * header.
 */
internal fun flywayVersion(applicationVersion: String): String {
    val version = applicationVersion.trim()
        .takeWhile { it.isDigit() || it == '.' }
        .trimEnd('.')

    require(version.isNotEmpty()) {
        "Cannot derive a Flyway version from application version '$applicationVersion'."
    }

    return version
}
