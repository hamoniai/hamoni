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

package ai.hamoni.control_room

import ai.hamoni.common.core.PLATFORM_SCHEMA
import ai.hamoni.common.core.REQUEST_ORGANISATION_ID
import ai.hamoni.common.core.exception.EntityNotFoundException
import ai.hamoni.common.core.exception.InvalidParameterException
import ai.hamoni.control_room.data.OrganisationIdentityProvider
import ai.hamoni.control_room.service.AuthenticationService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver
import io.quarkus.oidc.OidcRequestContext
import io.quarkus.oidc.OidcTenantConfig
import io.smallrye.mutiny.Uni
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.RoutingContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

/**
 * Behaviour of the OIDC tenant configuration resolver.
 */
class OidcTenantConfigResolverTest {

    private val authenticationService = mockk<AuthenticationService>()
    private val resolver = OidcTenantConfigResolver(authenticationService)

    private val providerId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    @BeforeEach
    fun resetAuthenticationService() {
        clearMocks(authenticationService)
    }

    @Test
    fun `a login without a provider is rejected`() {
        // Given
        val context = routingContextFor(LOGIN_PATH, null)

        // When / Then
        assertThrows<InvalidParameterException> {
            resolver.resolve(context, requestContext()).await().indefinitely()
        }
    }

    @Test
    fun `a login with an invalid provider identifier is rejected`() {
        // Given
        val context = routingContextFor(LOGIN_PATH, "not-a-uuid")

        // When / Then
        assertThrows<InvalidParameterException> {
            resolver.resolve(context, requestContext()).await().indefinitely()
        }
    }

    @Test
    fun `a login with an unknown provider is rejected`() {
        // Given
        every { authenticationService.getIdentityProviderById(providerId) } returns
            Uni.createFrom().failure(
                EntityNotFoundException(
                    AuthenticationService::getIdentityProviderById,
                    "identity provider not found"
                )
            )
        val context = routingContextFor(LOGIN_PATH, providerId.toString())

        // When / Then
        assertThrows<EntityNotFoundException> {
            resolver.resolve(context, requestContext()).await().indefinitely()
        }
    }

    @Test
    fun `a login with a known provider resolves its configuration`() {
        // Given
        every { authenticationService.getIdentityProviderById(providerId) } returns
            Uni.createFrom().item(provider())
        val context = routingContextFor(LOGIN_PATH, providerId.toString())

        // When
        val actual = checkNotNull(resolver.resolve(context, requestContext()).await().indefinitely())

        // Then
        assertThat(actual.tenantId().get()).isEqualTo(providerId.toString())
        assertThat(actual.authServerUrl().get()).isEqualTo("https://issuer.example.com")
        assertThat(actual.clientId().get()).isEqualTo("client-id")
        assertThat(actual.credentials().secret().get()).isEqualTo("secret-ref")
        assertThat(actual.applicationType().get().toString()).isEqualTo("HYBRID")
        assertThat(actual.discoveryEnabled().get()).isTrue()
    }

    @Test
    fun `a non-login path does not resolve an OIDC tenant`() {
        // Given
        val context = routingContextFor("${API_PATH}organisation", null)

        // When
        val actual = resolver.resolve(context, requestContext()).await().indefinitely()

        // Then
        assertThat(actual).isNull()
    }

    private fun provider(): OrganisationIdentityProvider =
        OrganisationIdentityProvider(
            id = providerId,
            name = "Test provider",
            url = "https://issuer.example.com",
            clientId = "client-id",
            clientSecretReference = "secret-ref",
            enabled = true
        )

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
 * Behaviour of the organisation tenant resolver fallback.
 */
class OrganisationTenantResolverProducerTest {

    private val organisationId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Test
    fun `an authentication path resolves the platform schema`() {
        // Given
        val producer = OrganisationTenantResolverProducer(routingContextFor("${AUTH_PATH}login"))

        // When
        val resolver = checkNotNull(producer.get())

        // Then
        assertThat(resolver.resolveTenantId()).isEqualTo(PLATFORM_SCHEMA)
        assertThat(resolver.defaultTenantId).isEqualTo(PLATFORM_SCHEMA)
        assertThat(resolver.isRoot(PLATFORM_SCHEMA)).isTrue()
    }

    @Test
    fun `an organisation header resolves the organisation tenant`() {
        // Given
        val producer = OrganisationTenantResolverProducer(
            routingContextFor(
                "${API_PATH}organisation",
                organisationId.toString()
            )
        )

        // When
        val resolver = checkNotNull(producer.get())

        // Then
        assertThat(resolver.resolveTenantId()).isEqualTo(organisationId.toString())
        assertThat(resolver.defaultTenantId).isEqualTo(organisationId.toString())
        assertThat(resolver.isRoot(organisationId.toString())).isFalse()
    }

    @Test
    fun `a missing organisation header is rejected`() {
        // Given
        val producer = OrganisationTenantResolverProducer(routingContextFor("${API_PATH}organisation"))

        // When / Then
        assertThrows<InvalidParameterException> { producer.get() }
    }

    @Test
    fun `an invalid organisation header is rejected`() {
        // Given
        val producer = OrganisationTenantResolverProducer(
            routingContextFor("${API_PATH}organisation", "not-a-uuid")
        )

        // When / Then
        assertThrows<InvalidParameterException> { producer.get() }
    }

    @Test
    fun `the fallback is used when there is no routing context`() {
        // Given
        val producer = OrganisationTenantResolverProducer(noRoutingContext())
        producer.tenantFallback = organisationId.toString()

        // When
        val resolver = checkNotNull(producer.get())

        // Then
        assertThat(resolver.resolveTenantId()).isEqualTo(organisationId.toString())
    }

    @Test
    fun `the routing context wins over the fallback`() {
        // Given
        val fallbackId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val producer = OrganisationTenantResolverProducer(
            routingContextFor("${API_PATH}organisation", organisationId.toString())
        )
        producer.tenantFallback = fallbackId.toString()

        // When
        val resolver = checkNotNull(producer.get())

        // Then
        assertThat(resolver.resolveTenantId()).isEqualTo(organisationId.toString())
    }

    @Test
    fun `no routing context and no fallback resolves no tenant`() {
        // Given
        val producer = OrganisationTenantResolverProducer(noRoutingContext())

        // When / Then
        assertThat(producer.get()).isNull()
    }

    private fun routingContextFor(path: String, organisationHeader: String? = null): RoutingContext {
        val request = mockk<HttpServerRequest>()
        every { request.path() } returns path
        every { request.getHeader(REQUEST_ORGANISATION_ID) } returns organisationHeader

        val context = mockk<RoutingContext>()
        every { context.request() } returns request
        return context
    }

    private fun noRoutingContext(): RoutingContext {
        val context = mockk<RoutingContext>()
        every { context.request() } throws IllegalStateException("no request")
        return context
    }
}

/**
 * Behaviour of the platform authorisation gateway.
 */
class PlatformAuthoriserTest {

    private val authoriser =
        PlatformAuthoriser(mockk<TenantResolver>(relaxed = true))

    @Test
    fun `organisation access currently succeeds`() {
        // When
        val result = authoriser.checkOrganisationAccess().await().indefinitely()

        // Then
        assertThat(result).isEqualTo(Unit)
    }

    @Test
    fun `platform admin or system access currently succeeds`() {
        // When
        val result = authoriser.checkPlatformAdminOrSystem().await().indefinitely()

        // Then
        assertThat(result).isEqualTo(Unit)
    }
}

/**
 * Behaviour of the REST authentication challenge.
 */
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
