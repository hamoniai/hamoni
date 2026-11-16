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

import ai.hamoni.common.platform.PlatformConfig
import ai.hamoni.control_room.OrganisationTenantResolverProducer
import ai.hamoni.control_room.ResourceLogger
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
import io.vertx.mutiny.json.schema.JsonSchema
import io.vertx.mutiny.json.schema.Validator
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets

/*
 * Default initialisers for the organisation.
 */

/**
 * Validates an organisation initialisation document against the versioned JSON
 * Schema.
 *
 * The schema is a classpath resource, so it is versioned with the application.
 */
class OrganisationImportValidator {

    companion object {
        /** The only schema version this application accepts. */
        const val SUPPORTED_SCHEMA_VERSION: Int = 1

        private const val SCHEMA_RESOURCE = "/schema/organisation-init/v1.schema.json"
    }

    private val validator: Validator = createValidator()

    /**
     * Validate [document], failing with [OrganisationInitialisationException]
     * when it does not conform to the supported schema.
     */
    fun validate(document: JsonObject) {
        val result = validator.validate(document)
        if (result.valid != true) {
            throw OrganisationInitialisationException(
                "Organisation init file does not match the schema: ${errors(result)}"
            )
        }

        val version = document.getInteger("schemaVersion")
        if (version != SUPPORTED_SCHEMA_VERSION) {
            throw OrganisationInitialisationException(
                "Unsupported organisation init schema version '$version'"
            )
        }
    }

    /**
     * Build the validator from the schema resource.
     */
    private fun createValidator(): Validator {
        val schemaText = OrganisationImportValidator::class.java
            .getResourceAsStream(SCHEMA_RESOURCE)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw OrganisationInitialisationException(
                "Organisation init schema '$SCHEMA_RESOURCE' not found on the classpath"
            )

        val schema = JsonSchema.of(JsonObject(schemaText))
        return Validator.create(
            schema,
            JsonSchemaOptions()
                .setDraft(Draft.DRAFT202012)
                .setBaseUri("https://hamoni.ai/schema/organisation-init/")
        )
    }

    /**
     * Flatten the validation output into readable error messages.
     */
    private fun errors(result: OutputUnit): String =
        result.errors
            .flatMap { flatten(it) }
            .joinToString("; ")
            .ifBlank { "unknown validation error" }

    /**
     * Flatten one validation output and its children.
     */
    private fun flatten(unit: OutputUnit): List<String> =
        listOfNotNull(unit.error?.let { "${unit.keywordLocation}: $it" }) +
            unit.errors.flatMap { flatten(it) }
}

/**
 * Load and validate the default organisation's init file at startup.
 *
 * The initialiser runs after the database migrator. Decoding the validated
 * document into `Organisation` and persisting it are intentionally not wired
 * yet; the import shape and service design are still being settled.
 *
 * The build property gate exists only to keep this initialiser out of the
 * schema-export profile, which disables Flyway. The initialiser does not itself
 * depend on Flyway; the runtime init-file configuration decides whether it is
 * used.
 */
@ApplicationScoped
@IfBuildProperty(name = "quarkus.flyway.enabled", stringValue = "true", enableIfMissing = true)
class OrganisationInitialiser @Inject constructor(
    private val organisationService: OrganisationService,
    private val organisationTenantResolverProducer: OrganisationTenantResolverProducer,
    private val platformConfig: PlatformConfig,
    private val vertx: Vertx
) {
    /**
     * Initialise the default organisation.
     */
    private fun initialise(): Uni<Boolean> =
        platformConfig.defaultOrganisation().config().orElse(null).takeIf { !it.isNullOrBlank() }?.let { path ->
            // Read and then parse/update
            vertx.fileSystem().exists(path)
                .onItem().transformToUni {
                    if (it == true) {
                        vertx.fileSystem().readFile(path)
                            .onItem().transformToUni { content ->
                                loadDefaultOrganisation(content.toString(StandardCharsets.UTF_8))
                            }
                    } else {
                        Uni.createFrom()
                            .failure { FileNotFoundException("organisation defaults '${path}' not found") }
                    }
                }
        } ?: run {
            Uni.createFrom()
                .failure { FileNotFoundException("no organisation defaults specified") }
        }

    /**
     * Load an organisation from a JSON defaults
     * @param defaults The organisation defaults.
     */
    private fun loadDefaultOrganisation(defaults: String): Uni<Boolean> {
        // FIXME: this needs implementing. OrganisationImportValidator is a pure non-reactive validation.
        // However, it is non-blocking by the nature of having all resources already loaded.
        // This function needs to:
        // 1. Identify the application version and get the schema
        // 2. Load the schema and initialise the validator.
        // 3. Call the validator
        // 4. If valid, then load the organisation and identity providers.
        return Uni.createFrom().failure { NotImplementedError("TODO") }
    }

    /**
     * Bind to the Quarkus startup to initialise the organisation
     */
    @Suppress("unused")
    fun onStart(
        @Observes @Priority(Interceptor.Priority.PLATFORM_AFTER)
        @Suppress("UNUSED_PARAMETER") event: StartupEvent
    ) {
        val defaultOrganisationId = platformConfig.defaultOrganisation().id()
        ResourceLogger.LOGGER.organisationInitialisationUpdate(defaultOrganisationId, "setting default organisation")

        // Initialise the default organisation if required.
        VertxContextSupport.subscribeAndAwait {
            // We have a new request scope here, needs initialising.
            organisationTenantResolverProducer.tenantFallback = defaultOrganisationId.toString()

            organisationService.state()
                .onItem().transformToUni { state ->
                    when (state) {
                        // Skip if already initialised
                        OrganisationState.INITIALISED -> {
                            ResourceLogger.LOGGER.organisationInitialisationUpdate(
                                defaultOrganisationId,
                                "already initialised"
                            )
                            Uni.createFrom().item(false)
                        }

                        // Try to initialise, it might fail the Uni.
                        OrganisationState.NOT_INITIALISED -> initialise()

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
                        e
                    )
                }
        }
    }
}
