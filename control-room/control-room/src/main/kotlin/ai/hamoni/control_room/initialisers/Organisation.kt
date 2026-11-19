/*
 * Hamoni: Cloud workflow platform
 * Copyright (C) 2026 Hamoni Limited
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

import ai.hamoni.common.platform.PlatformConfig
import ai.hamoni.control_room.OrganisationTenantResolverProducer
import ai.hamoni.control_room.ResourceLogger
import ai.hamoni.control_room.data.Organisation
import ai.hamoni.control_room.data.OrganisationState
import ai.hamoni.control_room.exception.OrganisationInitialisationException
import ai.hamoni.control_room.service.OrganisationService
import io.quarkus.arc.properties.IfBuildProperty
import io.quarkus.runtime.StartupEvent
import io.quarkus.vertx.VertxContextSupport
import io.smallrye.mutiny.Uni
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.Draft
import io.vertx.json.schema.JsonSchemaOptions
import io.vertx.json.schema.OutputUnit
import io.vertx.mutiny.core.Vertx
import io.vertx.mutiny.core.buffer.Buffer
import io.vertx.mutiny.json.schema.JsonSchema
import io.vertx.mutiny.json.schema.Validator
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor
import kotlinx.serialization.json.Json
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets

/*
 * Default initialisers for the organisation.
 */

/**
 * Validates an organisation initialisation document against the generated JSON
 * Schema, then deserialises the object into an actual [Organisation].
 */
internal class OrganisationLoader(private val json: Json, private val validator: Validator) {
    companion object {
        /**
         * Load the schema, and contents and return the initialised object.
         */
        fun createLoader(json: Json, schema: String) = OrganisationLoader(
            json,
            Validator.create(
                JsonSchema.of(JsonObject(schema)),
                JsonSchemaOptions()
                    .setDraft(Draft.DRAFT202012)
                    .setBaseUri("https://hamoni.ai/schema/organisation-init/")
            )
        )
    }

    /**
     * Flatten the validation output into readable error messages.
     */
    private fun errors(result: OutputUnit): String = result.errors
        ?.filterNotNull()
        ?.flatMap { flatten(it) }
        ?.joinToString("; ")
        ?.ifBlank { result.error ?: "unknown validation error" }
        ?: result.error
        ?: "unknown validation error"

    /**
     * Flatten one validation output and its children.
     */
    private fun flatten(unit: OutputUnit): List<String> =
        listOfNotNull(unit.error?.let { "${unit.keywordLocation}: $it" }) +
            unit.errors.orEmpty().filterNotNull().flatMap { flatten(it) }

    /**
     * Validate a content string, and the load the organisation. Even though the schema validator is reactive, all
     * the refences are loaded into memory so that validation and extraction can happen without blocking.
     */
    fun loadOrganisation(content: String): Organisation = with(JsonObject(content)) {
        validator.validate(this).takeIf { !it.valid }?.let { result ->
            throw OrganisationInitialisationException(
                "Unable to validate the organisation initialiser JSON: ${errors(result)}",
            )
        }

        this.getJsonObject("defaultOrganisation")?.let { obj ->
            json.decodeFromString<Organisation>(obj.encode())
        } ?: throw OrganisationInitialisationException(
            "Organisation initialiser JSON does not contain a default organisation"
        )
    }
}

/**
 * Load and validate the default organisation's init file at startup.
 *
 * The initialiser runs after the database migrator. The schema is loaded from
 * the same property used by the JSON schema export command, so the runtime and
 * the generator cannot disagree about the schema location.
 *
 * The build property gate exists only to keep this initialiser out of the
 * cmd profile, which disables Flyway. The initialiser does not itself
 * depend on Flyway; the runtime init-file configuration decides whether it is
 * used.
 */
@ApplicationScoped
@IfBuildProperty(name = "quarkus.flyway.enabled", stringValue = "true", enableIfMissing = true)
class OrganisationInitialiser
@Inject
constructor(
    private val organisationService: OrganisationService,
    private val organisationTenantResolverProducer: OrganisationTenantResolverProducer,
    private val platformConfig: PlatformConfig,
    private val vertx: Vertx,
    @param:ConfigProperty(name = "hamoni.initialiser.organisation.schema") private val schemaPath: String,
    private val json: Json,
) {

    /**
     * Get the contents of the organisation loader
     */
    private fun getContent(): Uni<Buffer> = platformConfig
        .defaultOrganisation()
        .config()
        .orElse(null)
        .takeIf { !it.isNullOrBlank() }
        ?.let { path ->
            vertx
                .fileSystem()
                .exists(path)
                .onItem()
                .transformToUni {
                    if (it == true) {
                        vertx.fileSystem()
                            .readFile(path)
                    } else {
                        Uni.createFrom()
                            .failure { FileNotFoundException("organisation defaults '$path' not found") }
                    }
                }
        }
        ?: Uni
            .createFrom()
            .failure { FileNotFoundException("no organisation defaults specified") }

    /**
     * Initialise the default organisation. To initialise, we need to load both the defaults file and the
     * schema to pass to the validator.
     */
    private fun initialise(): Uni<Boolean> {
        val schema =
            vertx
                .fileSystem()
                .readFile(schemaPath)
                .onFailure().transform { e ->
                    OrganisationInitialisationException("Unable to load import schema", e)
                }

        return Uni.combine().all()
            .unis(
                schema,
                getContent()
                    .onFailure().transform { e ->
                        OrganisationInitialisationException("Unable to load organisation defaults", e)
                    }
            )
            .with { schemaBuffer, contentBuffer ->
                val loader = OrganisationLoader.createLoader(
                    json,
                    schemaBuffer.toString(StandardCharsets.UTF_8)
                )
                loader.loadOrganisation(contentBuffer.toString(StandardCharsets.UTF_8))
            }
            .onItem().transformToUni {
                organisationService.create(it)
            }
            .onItem().transform {
                true
            }
    }

    /**
     * Bind to the Quarkus startup to initialise the organisation.
     */
    @Suppress("unused")
    fun onStart(
        @Observes @Priority(Interceptor.Priority.PLATFORM_AFTER)
        @Suppress("UNUSED_PARAMETER") event: StartupEvent,
    ) {
        val defaultOrganisationId = platformConfig.defaultOrganisation().id()
        ResourceLogger.LOGGER.organisationInitialisationUpdate(
            defaultOrganisationId,
            "setting default organisation",
        )

        // Initialise the default organisation if required.
        VertxContextSupport.subscribeAndAwait {
            // We have a new request scope here, needs initialising.
            organisationTenantResolverProducer.tenantFallback = defaultOrganisationId.toString()

            organisationService.state()
                .onItem().transformToUni { state ->
                    when (state) {
                        // Skip if already initialised
                        OrganisationState.ACTIVE -> {
                            ResourceLogger.LOGGER.organisationInitialisationUpdate(
                                defaultOrganisationId,
                                "already initialised",
                            )
                            Uni.createFrom().item(false)
                        }

                        // Disabled - should be rare
                        OrganisationState.DISABLED -> {
                            Uni.createFrom().failure {
                                OrganisationInitialisationException("default organisation is disabled")
                            }
                        }

                        // Try to initialise, it might fail the Uni.
                        OrganisationState.NOT_INITIALISED -> {
                            initialise()
                        }

                        // Some error, do not boot.
                        else -> {
                            Uni.createFrom().failure {
                                OrganisationInitialisationException("default organisation has an error")
                            }
                        }
                    }
                }
                .onFailure().invoke { e ->
                    ResourceLogger.LOGGER.organisationInitialisationError(
                        defaultOrganisationId,
                        e.message,
                        e,
                    )
                }
                .onFailure().recoverWithNull()
        }
    }
}
