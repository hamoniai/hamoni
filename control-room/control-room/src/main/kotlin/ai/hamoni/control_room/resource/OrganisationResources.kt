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

package ai.hamoni.control_room.resource

import ai.hamoni.common.core.APPLICATION_JSON_UTF8
import ai.hamoni.control_room.PlatformTenantResolver
import ai.hamoni.control_room.data.Organisation
import ai.hamoni.control_room.data.OrganisationIdentityProvider
import ai.hamoni.control_room.data.OrganisationSetting
import ai.hamoni.control_room.service.OrganisationService
import ai.hamoni.control_room.service.OrganisationServiceHydration
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestResponse

/**
 * The organisation interface.
 *
 * The organisation and its configuration are business calls (group B) and need
 * an authenticated caller. The authentication configuration is read by the
 * sign-in page before anyone can authenticate, so those methods are public
 * (group C).
 */
@Authenticated
@Path("/organisation")
class OrganisationResource @Inject constructor(
    private val organisationService: OrganisationService,
) {

    /**
     * Get an organisation, hydrating the related data named by the caller.
     *
     * Getting an organisation only happens from within the organisation context. So there is no path parameter.
     * This also means that all the security is managed from the [PlatformTenantResolver] class.
     * There should never be a case where we need to perform actions on organisation from a different organisation.
     *
     * @param hydrate the related data to attach to the organisation
     * @return the organisation
     */
    @GET
    @Produces(APPLICATION_JSON_UTF8)
    fun get(
        @RestQuery hydrate: List<OrganisationServiceHydration>
    ): Uni<RestResponse<Organisation>> =
        organisationService.get(hydrate)
            .onItem().transform { data -> RestResponse.ok(data) }

    /**
     * Get an organisation's identity providers. This service is for managing the identity providers. For the
     * un-authenticated way to get identity providers, for the login page, use '/auth/identityProviders'.
     *
     * It should be noted that the name appears on the login page, so when choosing names for the identity providers.
     *
     * @return the organisation's identity providers. This will only return the header projection, which contains
     * non-sensitive information. To get the full information, access specifically by identifier.
     */
    @GET
    @Path("/identityProviders")
    @Produces(APPLICATION_JSON_UTF8)
    fun getIdentityProviders(): Uni<RestResponse<List<OrganisationIdentityProvider>>> =
        organisationService.getIdentityProviders()
            .onItem().transform { data -> RestResponse.ok(data) }

    /**
     * Get an organisation's settings.
     *
     * @return the organisation's configuration
     */
    @GET
    @Path("/settings")
    @Produces(APPLICATION_JSON_UTF8)
    fun getSettings(): Uni<RestResponse<List<OrganisationSetting>>> =
        organisationService.getSettings()
            .onItem().transform { data -> RestResponse.ok(data) }
}
