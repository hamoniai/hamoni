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

package ai.hamoni.control_room.initialisers

import ai.hamoni.common.core.ORGANISATION_SCHEMA
import ai.hamoni.common.core.PLATFORM_SCHEMA
import ai.hamoni.common.core.util.isValidUuid
import ai.hamoni.common.platform.PlatformConfig
import ai.hamoni.control_room.ResourceLogger
import io.quarkus.arc.properties.IfBuildProperty
import io.quarkus.flyway.FlywayConfigurationCustomizer
import io.quarkus.flyway.FlywayDataSource
import io.quarkus.flyway.runtime.FlywayContainer
import io.quarkus.flyway.runtime.FlywayContainerProducer
import io.quarkus.runtime.StartupEvent
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.interceptor.Interceptor
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.ResourceProvider
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.resource.LoadableResource
import java.sql.SQLException
import javax.sql.DataSource


/*
 * Custom initialisers for database related activities.
 */

const val COMMON_PATH = "common"

/**
 * Custom database initialiser and migrator. This is required because of the schema separation. In Hamoni,
 * there are two distinct schemas,
 * 1. The common schema, defined in 'ai.hamoni.common.core.GLOBAL_SCHEMA'
 * 2. The organisation specific schema, which will be the UUID of the organisation.
 *
 * Each schema will have its own Flyway tracking. There are also some general rules about database schemas and migration
 * 1. Don't make breaking changes. for example, if a column is renamed, then duplicate the column, mark the old one as
 *    depreciated, then in the next release, remove the old column. This is because Kubernetes does a rolling update,
 *    and a breaking change would cause error 500s from all the other control room instances that are still running.
 *
 * This is not the most efficient way to initialise because it scans the list of migrations on each schema, however
 * it aligns closest with Quarkus' methodology on initialising Flyway. This means that all the configuration properties
 * still work as expected.
 */
@ApplicationScoped
@IfBuildProperty(name = "quarkus.flyway.enabled", stringValue = "true", enableIfMissing = true)
class DatabaseUpdater @Inject constructor (
    private val platformConfig: PlatformConfig,
    private val dataSource: DataSource,
    private val flywayContainerProducer: FlywayContainerProducer,
    private val defaultFlywayContainer: FlywayContainer,
    @param:FlywayDataSource(ORGANISATION_SCHEMA)
    private val schemaSelector: FlywayConfigurationSchemaCustomizer,

    @param:ConfigProperty(name = "quarkus.datasource.db-kind")
    private val dataSourceKind: String,
) {

    /**
     * Bind the migration to the Quarkus startup. Use the properties from the default container.
     */
    @Suppress("unused")
    fun onStart(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) @Suppress("UNUSED_PARAMETER") event: StartupEvent) {
        // The defaultFlywayContainer is the main one that is a pseudo setup.
        val platformContainer = flywayContainerProducer.createFlyway(
            dataSource,
            PLATFORM_SCHEMA,
            defaultFlywayContainer.isHasMigrations,
            defaultFlywayContainer.isCreatePossible,
            defaultFlywayContainer.resourceLocations
                .filter { it.contains("/${PLATFORM_SCHEMA}/") }
                .flatMap { base -> base.removeSuffix("/").let { listOf("${it}/${dataSourceKind}/", "${it}/${COMMON_PATH}/") } }
                .toSet()
        )

        try {
            platformContainer.flyway.migrate()
        } catch (e: FlywayException) {
            ResourceLogger.LOGGER.databaseMigrationError(schemaSelector.schema, e)
            throw e
        }

        // Create a set of organisations to update
        val organisationIds = mutableSetOf<String>()

        // To get the list of schemas, we need to perform a low level query on the datasource. Hibernate etc does
        // not expose this, and it is very early in the boot cycle. This will use the blocking drivers during startup.
        try {
            dataSource.connection.use { connection ->
                val metaData = connection.metaData

                metaData.schemas.use { schemas ->
                    while (schemas.next()) {
                        schemas.getString("TABLE_SCHEM")?.apply {
                            if (this.isValidUuid()) {
                                organisationIds.add(this)
                            }
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            ResourceLogger.LOGGER.schemaQueryError(e)
            throw e
        }

        // Special catch, if the default schema is not already created, and there is no initialiser script, do not
        // create it. It may be a mistake. The Organisation initialiser will actually report and throw the error. We
        // check this because if there is an initialiser, then we want to add the schema so that it is created/upgraded
        // which is essentially a first load scenario.
        val defaultOrganisationId = platformConfig.defaultOrganisation().id().toString()
        if (defaultOrganisationId !in organisationIds &&
            !platformConfig.defaultOrganisation().config().orElse(null).isNullOrBlank()) {
            organisationIds.add(defaultOrganisationId)
        }

        // Enumerate over all the organisation schemas. If there is a failure, then fail all.
        try {
            organisationIds.forEach { organisationId ->
                // Create a Flyway container for each organisation which will copy the default. We need a new container
                // because the Flyway object is created when we create the container.
                schemaSelector.schema = organisationId
                val organisationContainer = flywayContainerProducer.createFlyway(
                    dataSource,
                    ORGANISATION_SCHEMA,
                    defaultFlywayContainer.isHasMigrations,
                    defaultFlywayContainer.isCreatePossible,
                    defaultFlywayContainer.resourceLocations
                        .filter { it.contains("/${ORGANISATION_SCHEMA}/") }
                        .flatMap { base -> base.removeSuffix("/").let { listOf("${it}/${dataSourceKind}/", "${it}/${COMMON_PATH}/") } }
                        .toSet()
                )

                organisationContainer.flyway.migrate()
            }
        } catch (e: FlywayException) {
            ResourceLogger.LOGGER.databaseMigrationError(schemaSelector.schema, e)
            throw e
        }
    }
}

/**
 * A customiser to set the schema for a given iteration.
 */
@Singleton
@FlywayDataSource(ORGANISATION_SCHEMA)
class FlywayConfigurationSchemaCustomizer : FlywayConfigurationCustomizer {

    /**
     * This is set to the global schema by default so that the default FlywayContainer has the correct settings.
     */
    var schema: String = ""

    override fun customize(configuration: FluentConfiguration) {
        configuration
            .defaultSchema(schema)
            .schemas(schema)
    }
}

/**
 * A customiser filter the paths for a config (organisation)
 */
@Singleton
@FlywayDataSource(ORGANISATION_SCHEMA)
class FlywayConfigurationOrganisationResourcesCustomizer @Inject constructor (
    @param:ConfigProperty(name = "quarkus.datasource.db-kind")
    private val dataSourceKind: String
) : FlywayConfigurationCustomizer {

    override fun customize(configuration: FluentConfiguration) {
        configuration.resourceProvider(
            FilteringResourceProvider(
                configuration.resourceProvider,
                ORGANISATION_SCHEMA,
                dataSourceKind
            )
        )
    }
}

/**
 * A customiser filter the paths for a config (platform)
 */
@Singleton
@FlywayDataSource(PLATFORM_SCHEMA)
class FlywayConfigurationPlatformResourcesCustomizer @Inject constructor (
    @param:ConfigProperty(name = "quarkus.datasource.db-kind")
    private val dataSourceKind: String
) : FlywayConfigurationCustomizer {

    override fun customize(configuration: FluentConfiguration) {
        configuration.resourceProvider(
            FilteringResourceProvider(
                configuration.resourceProvider,
                PLATFORM_SCHEMA,
                dataSourceKind
            )
        )
    }
}

/**
 * A filtering resource provider based on the schema and database type.
 *
 * We need to do this because there's a bug(?) in Quarkus. The Flyway resources are scanned at build time, and
 * as the second and third Flyway instances are not active, their resources are not found. It seems that underlying,
 * all Flyway instances get the same set of resources, and the FlywayContainer works as a filter. However, this means
 * that all three Flyway instances get the same resources, casuing a duplicate problem. This filters that list
 * so that each Flyway instance only gets the specific resources it needs.
 */
class FilteringResourceProvider(
    private val parent: ResourceProvider,
    schema: String,
    dataSourceType: String
) : ResourceProvider {
    private val commonPath = "/${schema}/${COMMON_PATH}/"
    private val typePath = "/${schema}/${dataSourceType}/"

    override fun getResource(name: String): LoadableResource? =
        parent.getResource(name)?.takeIf { filterMatches(it) }

    override fun getResources(
        prefix: String,
        suffixes: Array<out String>
    ): Collection<LoadableResource> =
        parent
            .getResources(prefix, suffixes)
            .filter { filterMatches(it) }

    /**
     * Filter the resources
     */
    private fun filterMatches(loadableResource: LoadableResource): Boolean =
        loadableResource.absolutePath.contains(commonPath) ||
                loadableResource.absolutePath.contains(typePath)
}
