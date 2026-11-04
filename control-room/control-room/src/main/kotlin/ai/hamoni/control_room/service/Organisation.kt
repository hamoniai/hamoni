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

import ai.hamoni.control_room.data.Organisation
import ai.hamoni.control_room.data.OrganisationIdentityProvider
import ai.hamoni.control_room.data.OrganisationSetting
import ai.hamoni.control_room.repository.OrganisationRepository
import io.quarkus.runtime.annotations.RegisterForReflection
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * All frontend organisation specific services go in here. Services that are common to the frontend and backend
 * should go into one of the common projects.
 *
 * In all services, the authorisation should be checked before making data calls/accessing cache objects. The
 * authorisation itself uses a cache because it is accessed very frequently.
 */


/**
 * Service for organisation data.
 *
 * This service purposely does not have a list()/search() function as it would potentially be a large security hole.
 * The external provisioning system (for the provision API) knows the state of organisations. This also does not make
 * sense because there will only ever be one organisation record in the organisation table. The organisation
 * routing is done by Hibernate's tenant separation.
 *
 */
@ApplicationScoped
@RegisterForReflection
class OrganisationService @Inject constructor(
    private val organisationRepository: OrganisationRepository
) {

    /**
     * Get an organisation and attach the related data the caller asked for. The organisation id comes from the header
     * there should be no reason to cross over organisations.
     *
     * @param hydrate the related data to attach to the organisation
     * @return the organisation with the requested data attached
     */
    fun get(hydrate: List<OrganisationServiceHydration> = emptyList()): Uni<Organisation> {
        //checkAuthorisation()
        //organisationRepository.findById(id)
        TODO()
    }

    /**
     * Get an organisation's identity providers.
     *
     * This service is used by the public API, but requires that we do not return any information about the
     * organisation since we do not know whether the person authenticating actually has permission to the organisation
     * yet.
     *
     * @return the organisation authentication configuration
     */
    fun getIdentityProviders(): Uni<List<OrganisationIdentityProvider>> {
        TODO()
    }

    /**
     * Get an organisation's settings. ID is determined from the organisation header.
     *
     * @return the organisation configuration
     */
    fun getSettings(): Uni<List<OrganisationSetting>> {
        TODO()
    }
}

/**
 * The related data [OrganisationService] can attach to an organisation.
 */
@Serializable
enum class OrganisationServiceHydration {
    @SerialName("settings")
    CONFIGURATION,

    @SerialName("identityProviders")
    IDENTITY_PROVIDERS
}
