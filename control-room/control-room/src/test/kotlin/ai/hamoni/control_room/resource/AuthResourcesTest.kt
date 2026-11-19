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
package ai.hamoni.control_room.resource

import ai.hamoni.control_room.API_PATH
import ai.hamoni.control_room.LANDING_PATH
import ai.hamoni.control_room.LOGIN_PATH
import ai.hamoni.control_room.OidcTenantConfigResolver
import ai.hamoni.control_room.SecurityHelpers
import ai.hamoni.control_room.data.AuthenticationProfile
import ai.hamoni.control_room.data.AuthenticationProfileOrganisation
import ai.hamoni.control_room.data.OrganisationIdentityProvider
import ai.hamoni.control_room.data.OrganisationIdentityProviderHeaderWithSource
import ai.hamoni.control_room.service.AuthenticationProfileService
import ai.hamoni.control_room.service.AuthenticationProfileServiceHydration
import ai.hamoni.control_room.service.AuthenticationService
import io.mockk.every
import io.mockk.verify
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.smallrye.mutiny.Uni
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Behaviour of the authentication entry points.
 */
@QuarkusTest
class AuthResourcesTest {
    @InjectMock
    lateinit var authenticationService: AuthenticationService

    @InjectMock
    lateinit var authenticationProfileService: AuthenticationProfileService

    @InjectMock
    lateinit var securityHelpers: SecurityHelpers

    private val organisationId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val providerId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Test
    @TestSecurity(user = "test")
    fun `a login without a return url redirects to the landing path`() {
        // Given
        givenKnownProvider()

        // When
        val response =
            given()
                .redirects().follow(false)
                .queryParam(OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER, providerId)
                .`when`().get(LOGIN_PATH)

        // Then
        response.then()
            .statusCode(303)
            .header("Location", equalTo(LANDING_PATH))
    }

    @Test
    @TestSecurity(user = "test")
    fun `a local return url redirects to that path`() {
        // Given
        givenKnownProvider()
        val returnUrl = "/inbox"

        // When
        val response =
            given()
                .redirects().follow(false)
                .queryParam(OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER, providerId)
                .queryParam("returnUrl", returnUrl)
                .`when`().get(LOGIN_PATH)

        // Then
        response.then()
            .statusCode(303)
            .header("Location", equalTo(returnUrl))
    }

    @Test
    @TestSecurity(user = "test")
    fun `a non-local return url is rejected`() {
        // Given
        givenKnownProvider()

        // When
        val response =
            given()
                .redirects().follow(false)
                .queryParam(OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER, providerId)
                .queryParam("returnUrl", "https://example.com")
                .`when`().get(LOGIN_PATH)

        // Then
        response.then().statusCode(400)
    }

    @Test
    @TestSecurity(user = "test")
    fun `a protocol-relative return url is rejected`() {
        // Given
        givenKnownProvider()

        // When
        val response =
            given()
                .redirects().follow(false)
                .queryParam(OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER, providerId)
                .queryParam("returnUrl", "//example.com")
                .`when`().get(LOGIN_PATH)

        // Then
        response.then().statusCode(400)
    }

    @Test
    fun `a protected authentication call without a session returns 401`() {
        // When
        val response =
            given()
                .redirects().follow(false)
                .`when`().get("${API_PATH}auth/profile")

        // Then
        response.then().statusCode(401)
    }

    @Test
    fun `identity providers without an organisation use the default organisation`() {
        // Given
        val provider = providerHeader()
        every { securityHelpers.listIdentityProviders() } returns
            Uni.createFrom().item(listOf(provider))

        // When
        val response =
            given()
                .`when`().get("${API_PATH}auth/identityProviders")

        // Then
        response.then()
            .statusCode(200)
            .body("[0].name", equalTo("Example provider"))
    }

    @Test
    fun `identity providers with an organisation use that organisation`() {
        // Given
        val provider = providerHeader()
        every { securityHelpers.listIdentityProviders() } returns
            Uni.createFrom().item(listOf(provider))

        // When
        val response =
            given()
                .queryParam("organisationId", organisationId.toString())
                .`when`().get("${API_PATH}auth/identityProviders")

        // Then
        response.then()
            .statusCode(200)
            .body("[0].name", equalTo("Example provider"))
        verify(exactly = 1) { securityHelpers.listIdentityProviders() }
    }

    @Test
    @TestSecurity(user = "test")
    fun `the current profile is returned`() {
        // Given
        every { authenticationProfileService.profile(emptyList()) } returns
            Uni.createFrom().item(profile())

        // When
        val response =
            given()
                .`when`().get("${API_PATH}auth/profile")

        // Then
        response.then()
            .statusCode(200)
            .body("name", equalTo("Jane Doe"))
    }

    @Test
    @TestSecurity(user = "test")
    fun `the current profile organisations are returned`() {
        // Given
        val profile =
            profile().apply {
                organisations =
                    listOf(
                        AuthenticationProfileOrganisation(
                            id = organisationId,
                            name = "Example Organisation"
                        )
                    )
            }
        every {
            authenticationProfileService.profile(
                listOf(AuthenticationProfileServiceHydration.ORGANISATIONS)
            )
        } returns Uni.createFrom().item(profile)

        // When
        val response =
            given()
                .`when`().get("${API_PATH}auth/profile/organisations")

        // Then
        response.then()
            .statusCode(200)
            .body("[0].id", equalTo(organisationId.toString()))
    }

    @Test
    @TestSecurity(user = "test")
    fun `logout clears the local session`() {
        // Given
        every { authenticationService.logout() } returns Uni.createFrom().voidItem()

        // When
        val response =
            given()
                .`when`().get("${API_PATH}auth/logout")

        // Then
        response.then()
            .statusCode(200)
            .body(equalTo("true"))
    }

    private fun givenKnownProvider() {
        every { authenticationService.getIdentityProviderById(providerId) } returns
            Uni.createFrom().item(identityProvider())
    }

    private fun identityProvider(): OrganisationIdentityProvider =
        OrganisationIdentityProvider(
            id = providerId,
            name = "Example provider",
            url = "https://issuer.example.com",
            clientId = "client-id",
            enabled = true,
        ).apply { clientSecret = "secret-ref" }

    private fun providerHeader(): OrganisationIdentityProviderHeaderWithSource =
        OrganisationIdentityProviderHeaderWithSource(
            id = providerId,
            name = "Example provider",
            default = false
        )

    private fun profile(): AuthenticationProfile =
        AuthenticationProfile(
            id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
            familyName = "Doe",
            givenName = "Jane",
            name = "Jane Doe",
            email = "jane@example.com",
            issuer = "https://issuer.example.com",
            issuerSubject = "subject",
            enabled = true
        )
}
