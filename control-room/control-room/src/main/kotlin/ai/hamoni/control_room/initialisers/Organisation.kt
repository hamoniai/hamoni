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
import ai.hamoni.control_room.data.OrganisationState
import ai.hamoni.control_room.exception.OrganisationInitialisationException
import ai.hamoni.control_room.service.OrganisationService
import io.quarkus.arc.properties.IfBuildProperty
import io.quarkus.runtime.StartupEvent
import io.quarkus.vertx.VertxContextSupport
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.core.Vertx
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
 * Load and validate the default organisation's init file at startup.
 *
 * The initialiser runs after the database migrator. Decoding the validated
 * document into `Organisation` and persisting it are intentionally not wired
 * yet; the import shape and service design are still being settled.
 *
 * The build property gate exists only to keep this initialiser out of the
 * cmd profile, which disables Flyway. The initialiser does not itself
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
        // FIXME: this needs implementing. This function needs to:
        // 1. Validate the defaults against the generated import schema.
        // 2. Load the organisation and identity providers.
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
                        OrganisationState.ACTIVE -> {
                            ResourceLogger.LOGGER.organisationInitialisationUpdate(
                                defaultOrganisationId,
                                "already initialised"
                            )
                            Uni.createFrom().item(false)
                        }

                        // Disabled - should be rare
                        OrganisationState.DISABLED -> Uni.createFrom().failure {
                            OrganisationInitialisationException("default organisation is disabled")
                        }

                        // Try to initialise, it might fail the Uni.
                        OrganisationState.NOT_INITIALISED -> initialise()

                        // Some error, do not boot.
                        else -> Uni.createFrom().failure {
                            OrganisationInitialisationException("default organisation has an error")
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
                .onFailure().recoverWithNull()
        }
    }
}
