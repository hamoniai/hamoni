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
 */
@QuarkusMain
@ApplicationScoped
@IfBuildProfile("schema-export")
class SchemaExportCommand @Inject constructor(
    private val schemaExportService: SchemaExportService
) : QuarkusApplication {

    private val logger = Logger.getLogger(SchemaExportCommand::class.java)

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

    @param:ConfigProperty(name = "quarkus.datasource.db-kind")
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

                // Export
                val properties = sessionFactory.properties

                // When we configure the properties here, we need to use the Jakarta namespace settings, and be
                // aware of the 'orm' collector which Quarkus maps to. We cannot set these properties in the
                // main application properties because it will automatically run the schema generation, in the
                // Quarkus startup. We need to run manually as we need to update the filter on each run.
                // Additionally, when we call 'schemaManager.create()', it will overwrite the root properties,
                // in a copy of the properties map. Therefore, we need to specify the contributor properties to
                // override that.
                properties[AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION] = Action.NONE
                properties[AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION] = Action.CREATE_ONLY
                properties[AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET] = it
                properties[AvailableSettings.FORMAT_SQL] = true
                properties[AvailableSettings.HBM2DDL_FILTER_PROVIDER] = scope.filterProvider.name

                // Delete the contributor specific overrides so that we rely on the root settings.
                listOf("orm").forEach { contributor ->
                    properties["${AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION}.${contributor}"] = Action.NONE
                    properties["${AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION}.${contributor}"] = Action.CREATE_ONLY
                }

                sessionFactory.schemaManager.create(false)
            }
            path
        }
    }

    /**
     * Write the generated-file header to the SQL that was just written.
     *
     * The generation target overwrites the file, so the header is added after
     * the export rather than before it.
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
