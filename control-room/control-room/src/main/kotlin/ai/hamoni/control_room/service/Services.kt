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

package ai.hamoni.control_room.service

import ai.hamoni.control_room.data.AuthenticationProfile
import ai.hamoni.control_room.data.Organisation
import ai.hamoni.control_room.data.OrganisationAuthConfiguration
import ai.hamoni.control_room.data.OrganisationConfiguration
import io.quarkus.oidc.OidcSession
import io.quarkus.oidc.OidcTenantConfig
import io.quarkus.runtime.annotations.RegisterForReflection
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/*
 * All frontend specific services go in here. Services that are common to the frontend and backend should go into one
 * of the common projects.
 */


/**
 * Service for authentication profile logic
 */
@ApplicationScoped
@RegisterForReflection
class AuthenticationProfileService @Inject constructor(
    private val oidcSession: OidcSession
) {

    /**
     * Get the current logged-in authentication profile.
     */
    fun profile(hydrate: List<AuthenticationProfileServiceHydration>): Uni<AuthenticationProfile> {
        // Check the current principle id, and get the profile associated.
        TODO()
    }

    /**
     * Logout the current authentication profile locally.
     *
     * This clears the Quarkus session cookie only; it deliberately leaves the
     * provider's own session intact (for example, the person remains signed in to
     * Keycloak). Provider-initiated logout is a separate decision.
     */
    fun logout(): Uni<Void> = oidcSession.logout()
}

/**
 * hydration types for AuthenticationProfileService
 */
@Serializable
enum class AuthenticationProfileServiceHydration {
    @SerialName("organisations")
    ORGANISATIONS
}

/**
 * Service for deployment configuration that is not scoped to an individual
 * organisation.
 */
@ApplicationScoped
@RegisterForReflection
class ConfigurationService {

    companion object {
        /**
         * The default organisation identifier.
         *
         * This is a temporary holding value: how the default organisation is
         * identified at start-up has not been decided, so the all-zero UUID is
         * returned until that decision is made.
         */
        private val DEFAULT_ORGANISATION_ID: UUID =
            UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    /**
     * Get the default organisation's identifier.
     *
     * @return the identifier of the deployment's default organisation
     */
    fun getDefaultOrganisationId(): Uni<UUID> =
        Uni.createFrom().item(DEFAULT_ORGANISATION_ID)
}

/**
 * Service for OIDC provider lookups.
 */
@ApplicationScoped
@RegisterForReflection
class OidcProviderConfigurationService {

    /**
     * Get a provider by identifier.
     * This might need to get a value from the database of the know provider and then map it to the well known hosts.
     * This is also called very frequently, so probably needs a cache.
     *
     * @param id The identifier of the provider
     * @return the tenant configuration, or null to use the default tenant.
     */
    fun getById(id: UUID): Uni<OidcTenantConfig> =
        TODO()
}

/**
 * Service for organisation data.
 */
@ApplicationScoped
@RegisterForReflection
class OrganisationService {

    /**
     * Get an organisation and attach the related data the caller asked for.
     *
     * The organisation data layer has not been designed yet, so this method is
     * deliberately left unimplemented. The hydration options describe the data
     * the caller wants once the model settles.
     *
     * @param id the organisation identifier
     * @param hydrate the related data to attach to the organisation
     * @return the organisation with the requested data attached
     */
    fun getById(id: UUID, hydrate: List<OrganisationServiceHydration> = emptyList()): Uni<Organisation> {
        TODO()
    }

    /**
     * Get an organisation's configuration.
     *
     * The configuration data layer has not been designed yet, so this method is
     * deliberately left unimplemented.
     *
     * @param id the organisation identifier
     * @return the organisation configuration
     */
    fun getConfigurationById(id: UUID): Uni<OrganisationConfiguration> {
        TODO()
    }

    /**
     * Get an organisation's authentication configuration.
     *
     * The authentication configuration data layer has not been designed yet, so
     * this method is deliberately left unimplemented.
     *
     * @param id the organisation identifier
     * @return the organisation authentication configuration
     */
    fun getAuthConfigurationById(id: UUID): Uni<OrganisationAuthConfiguration> {
        TODO()
    }
}

/**
 * The related data [OrganisationService] can attach to an organisation.
 */
@Serializable
enum class OrganisationServiceHydration {
    @SerialName("configuration")
    CONFIGURATION,

    @SerialName("authConfiguration")
    AUTH_CONFIGURATION
}
