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
package ai.hamoni.control_room.service

import ai.hamoni.common.platform.PlatformConfig
import ai.hamoni.control_room.data.AuthenticationProfile
import ai.hamoni.control_room.data.IdentityProviderIndex
import ai.hamoni.control_room.repository.IdentityProviderIndexRepository
import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.quarkus.oidc.OidcSession
import io.quarkus.runtime.annotations.RegisterForReflection
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
class AuthenticationService @Inject constructor(private val oidcSession: OidcSession) {
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
class AuthenticationProfileService @Inject constructor() {
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
    ORGANISATIONS,
}

/**
 * Service for deployment configuration that is not scoped to an individual
 * organisation. This class should be mostly static, so it does not need mutiny wrappers.
 */
@ApplicationScoped
@RegisterForReflection
class ConfigurationService @Inject constructor(platformConfig: PlatformConfig) {
    /**
     * The default organisation identifier
     */
    val defaultOrganisationId: UUID = platformConfig.defaultOrganisation().id()
}

/**
 * Access the identity provider repository. This requires a list of organisations to search in the scope of as
 * arguments. We do not reference the TenantResolver as this should really return the platform schema or null for this
 * service as it's a platform-scope, not organisation scope.
 */
@ApplicationScoped
@RegisterForReflection
class IdentityProviderIndexService @Inject constructor(
    private val identityProviderIndexRepository: IdentityProviderIndexRepository,
) {
    /**
     * Get an index entry, within a specific scope.
     */
    fun get(providerIdentifier: UUID, organisationIdentifiers: Set<UUID>): Uni<IdentityProviderIndex> =
        Panache.withSession {
            identityProviderIndexRepository
                .find(
                    "#IdentityProviderIndex.findInScope",
                    mapOf(
                        "id" to providerIdentifier,
                        "scope" to organisationIdentifiers
                    )
                )
                .firstResult()
        }
}
