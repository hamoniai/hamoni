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

package ai.hamoni.control_room

import ai.hamoni.common.core.exception.InvalidParameterException
import ai.hamoni.control_room.service.OidcProviderConfigurationService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.quarkus.oidc.OidcRequestContext
import io.quarkus.oidc.OidcTenantConfig
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
import io.smallrye.mutiny.Uni
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.RoutingContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Behaviour of the OIDC tenant configuration resolver.
 */
@QuarkusTest
class OidcTenantConfigResolverTest {

    private val providerConfigurationService = mockk<OidcProviderConfigurationService>()
    private val resolver = OidcTenantConfigResolver(providerConfigurationService)

    private val providerId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    @BeforeEach
    fun resetProviderConfigurationService() {
        clearMocks(providerConfigurationService)
    }

    @Test
    @RunOnVertxContext
    fun `a login without a provider is rejected`(asserter: UniAsserter) {
        // Given
        val context = routingContextFor(LOGIN_PATH, null)

        // When
        val result = resolver.resolve(context, requestContext())

        // Then
        asserter.assertFailedWith({ result }, InvalidParameterException::class.java)
    }

    @Test
    @RunOnVertxContext
    fun `a login with an invalid provider identifier is rejected`(asserter: UniAsserter) {
        // Given
        val context = routingContextFor(LOGIN_PATH, "not-a-uuid")

        // When
        val result = resolver.resolve(context, requestContext())

        // Then
        asserter.assertFailedWith({ result }, InvalidParameterException::class.java)
    }

    @Test
    @RunOnVertxContext
    fun `a login with an unknown provider is rejected`(asserter: UniAsserter) {
        // Given
        every { providerConfigurationService.getById(providerId) } returns
            Uni.createFrom().failure(
                InvalidParameterException(
                    OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER,
                    "unknown provider"
                )
            )
        val context = routingContextFor(LOGIN_PATH, providerId.toString())

        // When
        val result = resolver.resolve(context, requestContext())

        // Then
        asserter.assertFailedWith({ result }, InvalidParameterException::class.java)
    }

    @Test
    @RunOnVertxContext
    fun `a login with a known provider resolves its configuration`(asserter: UniAsserter) {
        // Given
        val configuration = mockk<OidcTenantConfig>()
        every { providerConfigurationService.getById(providerId) } returns
            Uni.createFrom().item(configuration)
        val context = routingContextFor(LOGIN_PATH, providerId.toString())

        // When
        val result = resolver.resolve(context, requestContext())

        // Then
        asserter.assertThat({ result }) { actual ->
            assertThat(actual).isSameAs(configuration)
        }
    }

    private fun requestContext(): OidcRequestContext<OidcTenantConfig> =
        mockk<OidcRequestContext<OidcTenantConfig>>()

    private fun routingContextFor(path: String, provider: String?): RoutingContext {
        val request = mockk<HttpServerRequest>()
        every { request.path() } returns path
        every { request.getParam(OidcTenantConfigResolver.PROVIDER_QUERY_PARAMETER) } returns provider

        val context = mockk<RoutingContext>()
        every { context.request() } returns request
        return context
    }
}

/**
 * Behaviour of the REST authentication challenge.
 */
@QuarkusTest
class RestAuthenticationChallengeTest {

    private val challenge = RestAuthenticationChallenge()

    @Test
    fun `an unauthenticated REST call is challenged with 401`() {
        // Given
        val context = routingContextFor("${API_PATH}tasks/123")

        // When
        val isJavaScriptRequest = challenge.isJavaScriptRequest(context)
        val challengeData = challenge.getChallenge(context)

        // Then
        assertThat(isJavaScriptRequest).isTrue()
        assertThat(challengeData.status).isEqualTo(401)
    }

    @Test
    fun `the login entry point is allowed to start the authorization flow`() {
        // Given
        val loginContext = routingContextFor(LOGIN_PATH)
        val loginWithTrailingSlashContext = routingContextFor("$LOGIN_PATH/")

        // When
        val loginIsJavaScriptRequest = challenge.isJavaScriptRequest(loginContext)
        val trailingSlashIsJavaScriptRequest =
            challenge.isJavaScriptRequest(loginWithTrailingSlashContext)

        // Then
        assertThat(loginIsJavaScriptRequest).isFalse()
        assertThat(trailingSlashIsJavaScriptRequest).isFalse()
    }

    @Test
    fun `a non-API path is not treated as a REST challenge`() {
        // Given
        val context = routingContextFor("/auth/organisations")

        // When
        val isJavaScriptRequest = challenge.isJavaScriptRequest(context)

        // Then
        assertThat(isJavaScriptRequest).isFalse()
    }

    private fun routingContextFor(path: String): RoutingContext {
        val request = mockk<HttpServerRequest>()
        every { request.path() } returns path

        val context = mockk<RoutingContext>()
        every { context.request() } returns request
        return context
    }
}
