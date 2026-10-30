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
import ai.hamoni.control_room.data.Organisation
import ai.hamoni.control_room.data.OrganisationAuthConfiguration
import ai.hamoni.control_room.data.OrganisationConfiguration
import ai.hamoni.control_room.service.ConfigurationService
import ai.hamoni.control_room.service.OrganisationService
import ai.hamoni.control_room.service.OrganisationServiceHydration
import io.quarkus.security.Authenticated
import io.smallrye.mutiny.Uni
import jakarta.annotation.security.PermitAll
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestResponse
import java.util.UUID

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
    private val configurationService: ConfigurationService
) {

    /**
     * Get an organisation, hydrating the related data named by the caller.
     *
     * @param id the organisation identifier
     * @param hydrate the related data to attach to the organisation
     * @return the organisation
     */
    @GET
    @Path("/{id}")
    @Produces(APPLICATION_JSON_UTF8)
    fun get(
        @PathParam("id") id: UUID,
        @RestQuery hydrate: List<OrganisationServiceHydration>
    ): Uni<RestResponse<Organisation>> =
        organisationService.getById(id, hydrate)
            .onItem().transform { data -> RestResponse.ok(data) }

    /**
     * Get an organisation's authentication configuration.
     *
     * @param id the organisation identifier
     * @return the organisation's authentication configuration
     */
    @PermitAll
    @GET
    @Path("/{id}/authConfiguration")
    @Produces(APPLICATION_JSON_UTF8)
    fun getAuthConfiguration(@PathParam("id") id: UUID): Uni<RestResponse<OrganisationAuthConfiguration>> =
        organisationService.getAuthConfigurationById(id)
            .onItem().transform { data -> RestResponse.ok(data) }

    /**
     * Get the default organisation's authentication configuration without
     * exposing the default organisation's identifier to the sign-in page.
     *
     * @return the default organisation's authentication configuration
     */
    @PermitAll
    @GET
    @Path("/default/authConfiguration")
    @Produces(APPLICATION_JSON_UTF8)
    fun getAuthConfigurationForDefault(): Uni<RestResponse<OrganisationAuthConfiguration>> =
        configurationService.getDefaultOrganisationId()
            .onItem().transformToUni { id -> organisationService.getAuthConfigurationById(id) }
            .onItem().transform { data -> RestResponse.ok(data) }

    /**
     * Get an organisation's configuration.
     *
     * @param id the organisation identifier
     * @return the organisation's configuration
     */
    @GET
    @Path("/{id}/configuration")
    @Produces(APPLICATION_JSON_UTF8)
    fun getConfiguration(@PathParam("id") id: UUID): Uni<RestResponse<OrganisationConfiguration>> =
        organisationService.getConfigurationById(id)
            .onItem().transform { data -> RestResponse.ok(data) }
}
