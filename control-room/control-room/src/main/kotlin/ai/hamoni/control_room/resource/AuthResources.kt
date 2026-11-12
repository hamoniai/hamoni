package ai.hamoni.control_room.resource

import ai.hamoni.common.core.APPLICATION_JSON_UTF8
import ai.hamoni.control_room.LANDING_PATH
import ai.hamoni.control_room.data.AuthenticationProfile
import ai.hamoni.control_room.data.AuthenticationProfileOrganisation
import ai.hamoni.control_room.data.OrganisationIdentityProviderHeaderWithSource
import ai.hamoni.control_room.service.AuthenticationProfileService
import ai.hamoni.control_room.service.AuthenticationProfileServiceHydration
import ai.hamoni.control_room.service.AuthenticationService
import ai.hamoni.control_room.service.ConfigurationService
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.annotation.security.PermitAll
import jakarta.inject.Inject
import jakarta.validation.constraints.Pattern
import jakarta.ws.rs.*
import jakarta.ws.rs.core.HttpHeaders
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestResponse
import java.util.UUID

/**
 * Authentication resources/services
 */
@Authenticated
@Path("/auth")
class AuthenticationResource @Inject constructor(
    private val identity: SecurityIdentity,
    private val authenticationService: AuthenticationService,
    private val authenticationProfileService: AuthenticationProfileService,
    private val configurationService: ConfigurationService
) {

    /**
     * Start the OIDC authorization code flow, or continue to the next login stage
     * when the person is already authenticated.
     *
     * The frontend must navigate the whole browser to this endpoint rather than
     * calling it with a JavaScript request. The optional provider is consumed by
     * the tenant resolver before authentication; the next path is the frontend
     * route shown once the code exchange has completed.
     *
     * The login entry point is deliberately public: it starts the authorization
     * code flow, so there is no authenticated caller yet. The optional
     * `returnUrl` must be a local path; anything else is rejected.
     */
    @GET
    @Path("/login")
    fun login(
        @RestQuery
        @Pattern(regexp = "^/(?!/).*$", message = "must be a local path")
        returnUrl: String?
    ): Uni<RestResponse<Void>> =
        Uni.createFrom().item {
            RestResponse.ResponseBuilder.create<Void>(RestResponse.Status.SEE_OTHER)
                .header(HttpHeaders.LOCATION, returnUrl ?: LANDING_PATH)
                .build()
        }

    /**
     * Get the current authentication profile
     */
    @GET
    @Path("/profile")
    @Produces(APPLICATION_JSON_UTF8)
    fun profile(@RestQuery hydrate: List<AuthenticationProfileServiceHydration>): Uni<RestResponse<AuthenticationProfile>> =
        authenticationProfileService.profile(hydrate)
            .onItem().transform { data ->
                RestResponse.ResponseBuilder.create<AuthenticationProfile>(RestResponse.Status.OK)
                    .entity(data)
                    .build()
            }

    /**
     * Get the current authentication profile
     */
    @GET
    @Path("/profile/organisations")
    @Produces(APPLICATION_JSON_UTF8)
    fun profileOrganisations(): Uni<RestResponse<List<AuthenticationProfileOrganisation>>> =
        authenticationProfileService.profile(listOf(AuthenticationProfileServiceHydration.ORGANISATIONS))
            .onItem().transform { data ->
                RestResponse.ResponseBuilder.create<List<AuthenticationProfileOrganisation>>(RestResponse.Status.OK)
                    .entity(data.organisations)
                    .build()
            }

    /**
     * Get the identity provider information for an organisation. This is used to support the login page. This service
     * ignores any organisation header. Since this service is unauthenticated, and potentially cross-organisation, and
     * therefore will likely need to establish a new session, we will call into the authentication service, instead of
     * the organisation service.
     *
     * Kotlin serialisation is dumb here. An invalid UUID gets mapped to null, rather than an invalid input, so the
     * error message will be strange.
     *
     * If the [organisationId] parameter is not provided, the result will be the default organisation's identity providers.
     */
    @PermitAll
    @GET
    @Path("/identityProviders")
    @Produces(APPLICATION_JSON_UTF8)
    fun getIdentityProviders(@QueryParam("organisationId") organisationId: UUID?): Uni<RestResponse<List<OrganisationIdentityProviderHeaderWithSource>>> =
        if (organisationId == null) { configurationService.getDefaultOrganisationId() } else { Uni.createFrom().item { organisationId } }
            .onItem().transformToUni { authenticationService.listIdentityProvidersByOrganisationId(it) }
            .onItem().transform { data -> RestResponse.ok(data) }

    /**
     * Logout locally, clearing the Quarkus session cookie.
     */
    @GET
    @Path("/logout")
    @Produces(APPLICATION_JSON_UTF8)
    fun logout(): Uni<RestResponse<Boolean>> =
        authenticationService.logout()
            .onItem().transform { _ ->
                RestResponse.ResponseBuilder.create<Boolean>(RestResponse.Status.OK)
                    .entity(true)
                    .build()
            }
}
