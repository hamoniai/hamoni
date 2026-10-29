package ai.hamoni.control_room.resource

import ai.hamoni.common.platform.APPLICATION_JSON_UTF8
import ai.hamoni.control_room.data.AuthenticationProfile
import ai.hamoni.control_room.data.AuthenticationProfileOrganisation
import ai.hamoni.control_room.service.AuthenticationProfileService
import ai.hamoni.control_room.service.AuthenticationProfileServiceHydration
import io.quarkus.security.Authenticated
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.*
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestResponse

/**
 * Authentication resources/services
 */
@Authenticated
@Path("/auth")
class AuthenticationResource @Inject constructor(
        private val identity: SecurityIdentity,
        private val authenticationProfileService: AuthenticationProfileService
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
     * FIXME: Need to check teh return but do it safely because we might wan tto allow a non
     * local redurect if in dev mode, but not in production. Basically we need to sanitise
     * the returnUrl if provided, if not, then just return to /
     */
    @GET
    @Path("/login")
    fun login(@RestQuery returnUrl: String): Uni<RestResponse<Void>> =
        Uni.createFrom().item {
            RestResponse.ResponseBuilder.create<Void>(RestResponse.Status.SEE_OTHER)
                //.header(LOCATION_HEADER, nextLoginStage(next).toString())
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
     * Logout locally, clearing the Quarkus session cookie.
     */
    @GET
    @Path("/logout")
    @Produces(APPLICATION_JSON_UTF8)
    fun logout(): Uni<RestResponse<Boolean>> =
        authenticationProfileService.logout()
            .onItem().transform { _ ->
                RestResponse.ResponseBuilder.create<Boolean>(RestResponse.Status.OK)
                    .entity(true)
                    .build()
            }
}
