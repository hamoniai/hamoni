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

import ai.hamoni.common.core.exception.InvalidParameterException
import ai.hamoni.control_room.API_PATH
import ai.hamoni.control_room.LANDING_PATH
import ai.hamoni.control_room.LOGIN_PATH
import ai.hamoni.control_room.OidcTenantConfigResolver
import ai.hamoni.control_room.service.OidcProviderConfigurationService
import io.mockk.every
import io.mockk.mockk
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.oidc.OidcTenantConfig
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.smallrye.mutiny.Uni
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Behaviour of the authentication entry points.
 */
@QuarkusTest
class AuthResourceTest {

    @InjectMock
    lateinit var providerConfigurationService: OidcProviderConfigurationService

    private val providerId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    @Test
    fun `a login without a provider is rejected`() {
        // Given

        // When
        val response = given()
            .redirects().follow(false)
            .`when`().get(LOGIN_PATH)

        // Then
        response.then().statusCode(400)
    }

    @Test
    fun `a login with an unknown provider is rejected`() {
        // Given
        every { providerConfigurationService.getById(providerId) } returns
            Uni.createFrom().failure(
                InvalidParameterException(
                    OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER,
                    "unknown provider"
                )
            )

        // When
        val response = given()
            .redirects().follow(false)
            .queryParam(OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER, providerId)
            .`when`().get(LOGIN_PATH)

        // Then
        response.then().statusCode(400)
    }

    @Test
    fun `a login with a known provider redirects to the landing path`() {
        // Given
        givenKnownProvider()

        // When
        val response = given()
            .redirects().follow(false)
            .queryParam(OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER, providerId)
            .`when`().get(LOGIN_PATH)

        // Then
        response.then()
            .statusCode(303)
            .header("Location", equalTo(LANDING_PATH))
    }

    @Test
    fun `a local return url redirects to that path`() {
        // Given
        givenKnownProvider()
        val returnUrl = "/inbox"

        // When
        val response = given()
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
    fun `a non-local return url is rejected`() {
        // Given
        givenKnownProvider()
        val returnUrl = "https://example.com"

        // When
        val response = given()
            .redirects().follow(false)
            .queryParam(OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER, providerId)
            .queryParam("returnUrl", returnUrl)
            .`when`().get(LOGIN_PATH)

        // Then
        response.then().statusCode(400)
    }

    @Test
    fun `a protocol-relative return url is rejected`() {
        // Given
        givenKnownProvider()
        val returnUrl = "//example.com"

        // When
        val response = given()
            .redirects().follow(false)
            .queryParam(OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER, providerId)
            .queryParam("returnUrl", returnUrl)
            .`when`().get(LOGIN_PATH)

        // Then
        response.then().statusCode(400)
    }

    @Test
    fun `a protected authentication call without a session returns 401`() {
        // Given

        // When
        val response = given()
            .redirects().follow(false)
            .`when`().get("${API_PATH}auth/profile")

        // Then
        response.then().statusCode(401)
    }

    /**
     * Stub the provider lookup for a known provider.
     */
    private fun givenKnownProvider() {
        every { providerConfigurationService.getById(providerId) } returns
            Uni.createFrom().item(mockk<OidcTenantConfig>(relaxed = true))
    }
}
