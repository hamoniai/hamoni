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

import ai.hamoni.common.core.exception.EntityNotFoundException
import ai.hamoni.common.platform.PlatformConfig
import ai.hamoni.control_room.data.AuthenticationProfile
import ai.hamoni.control_room.data.OrganisationIdentityProvider
import ai.hamoni.control_room.data.OrganisationIdentityProviderHeaderWithSource
import ai.hamoni.control_room.repository.IdentityProviderIndexRepository
import ai.hamoni.control_room.repository.OrganisationIdentityProviderRepository
import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.quarkus.oidc.OidcSession
import io.quarkus.oidc.OidcTenantConfig
import io.quarkus.runtime.annotations.RegisterForReflection
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hibernate.reactive.mutiny.Mutiny
import java.util.*

/*
 * All frontend platform services go in here. Services that are common to the frontend and backend should go into one
 * of the common projects.
 *
 * In all services, the authorisation should be checked before making data calls/accessing cache objects. The
 * authorisation itself uses a cache because it is accessed very frequently.
 */


/**
 * Service for authentication activities
 */
@ApplicationScoped
@RegisterForReflection
class AuthenticationService @Inject constructor(
    private val oidcSession: OidcSession,
    private val identityProviderIndexRepository: IdentityProviderIndexRepository,
    private val organisationIdentityProviderRepository: OrganisationIdentityProviderRepository,
    private val sessionFactory: Mutiny.SessionFactory
) {

    /**
     * Get the OIDC configuration for a specific identity profile. This is called from the security context, but will
     * be on an unauthenticated request. Need to establish the tenant before getting the configuration.
     *
     * Needs to return the full object as this get mapped into a [OidcTenantConfig] object.
     *
     * This internal service only.
     */
    fun getIdentityProviderById(identityProviderId: UUID): Uni<OrganisationIdentityProvider> =
        identityProviderIndexRepository.findById(identityProviderId)
            .onItem().transformToUni { index ->
                if (null != index) {
                    sessionFactory.withSession(index.organisationId.toString()) {
                        organisationIdentityProviderRepository.findById(identityProviderId)
                    }
                } else {
                    Uni.createFrom().nullItem()
                }
            }
            .onItem().transformToUni { data ->
                if (null == data) {
                    Uni.createFrom().failure(EntityNotFoundException(AuthenticationService::getIdentityProviderById, "identity provider '${identityProviderId}' not found"))
                } else if (!data.enabled) {
                    Uni.createFrom().failure(EntityNotFoundException(AuthenticationService::getIdentityProviderById, "identity provider '${identityProviderId}' not enabled"))
                } else {
                    Uni.createFrom().item(data)
                }
            }

    /**
     * Get a list of all the identity providers for an organisation. This is here and not in OrganisationService as it
     * is a special implementation. Basically, we need to establish a new session in the context of the organisation
     * and get the providers from there. However, this is very careful to only return very limited information.
     *
     * This is unauthenticated, by will return the full list for any organisation. However, it will check the config
     * to see if it needs to add the default.
     *
     * This used on public calls so must be careful of information.
     */
    fun listIdentityProvidersByOrganisationId(organisationId: UUID): Uni<List<OrganisationIdentityProviderHeaderWithSource>> =
        //sessionFactory.withSession(organisationId.toString()) {
        Panache.withSession {
            organisationIdentityProviderRepository.findEnabled()
                .project(OrganisationIdentityProviderHeaderWithSource::class.java)
                .list()
        }
        //} // TODO: add in the default ones. Might need to query the full organisation. Or we create a special access
    // pattern in the organisation repository to get a specific setting, which might be more useful, because it can
    // directly return the value, or a default.

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
 * Service for authentication profile services
 */
@ApplicationScoped
@RegisterForReflection
class AuthenticationProfileService @Inject constructor(
) {

    /**
     * Get the current logged-in authentication profile.
     */
    fun profile(hydrate: List<AuthenticationProfileServiceHydration>): Uni<AuthenticationProfile> {
        // Check the current principle id, and get the profile associated.
        TODO("Waiting")
    }
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
class ConfigurationService @Inject constructor(
    private val platformConfig: PlatformConfig
) {
    /**
     * Get the default organisation's identifier. This is a wrapper around the configuration so that services
     * have a common entry point. The startup will ensure this is never null.
     *
     * @return the identifier of the deployment's default organisation
     */
    fun getDefaultOrganisationId(): Uni<UUID> =
        Uni.createFrom().item(platformConfig.defaultOrganisation().id())
}
