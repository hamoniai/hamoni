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
import ai.hamoni.common.core.exception.InvalidParameterException
import ai.hamoni.common.core.exception.ServiceException
import ai.hamoni.common.platform.PlatformConfig
import ai.hamoni.control_room.data.IdentityProviderIndex
import ai.hamoni.control_room.data.OrganisationIdentityProvider
import ai.hamoni.control_room.data.OrganisationIdentityProviderHeaderWithSource
import ai.hamoni.control_room.data.OrganisationSettingKey
import ai.hamoni.control_room.data.OrganisationState
import ai.hamoni.control_room.service.ConfigurationService
import ai.hamoni.control_room.service.IdentityProviderIndexService
import ai.hamoni.control_room.service.OrganisationService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver
import io.quarkus.oidc.OidcRequestContext
import io.quarkus.oidc.OidcTenantConfig
import io.quarkus.security.identity.CurrentIdentityAssociation
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
import io.smallrye.mutiny.Uni
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.RoutingContext
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Behaviour of the OIDC tenant configuration resolver.
 */
@QuarkusTest
class OidcTenantConfigResolverTest {

    private val securityHelpers = mockk<SecurityHelpers>()
    private val resolver = OidcTenantConfigResolver(securityHelpers)

    private val providerId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    @BeforeEach
    fun resetSecurityHelpers() {
        clearMocks(securityHelpers)
    }

    @Test
    @TestSecurity(user = "test")
    @RunOnVertxContext
    @DisplayName("resolve/unauthorised: login without provider is rejected")
    fun resolveUnauthorisedLoginWithoutProvider(asserter: UniAsserter) {
        // Given
        val context = routingContextFor(LOGIN_PATH, null)

        // When
        val result = { resolver.resolve(context, requestContext()) }

        // Then
        asserter.assertFailedWith(result, InvalidParameterException::class.java)
    }

    @Test
    @TestSecurity(user = "test")
    @RunOnVertxContext
    @DisplayName("resolve/unauthorised: invalid provider identifier is rejected")
    fun resolveUnauthorisedInvalidProvider(asserter: UniAsserter) {
        // Given
        val context = routingContextFor(LOGIN_PATH, "not-a-uuid")

        // When
        val result = { resolver.resolve(context, requestContext()) }

        // Then
        asserter.assertFailedWith(result, InvalidParameterException::class.java)
    }

    @Test
    @TestSecurity(user = "test")
    @RunOnVertxContext
    @DisplayName("resolve/unauthorised: unknown provider is rejected")
    fun resolveUnauthorisedUnknownProvider(asserter: UniAsserter) {
        // Given
        every { securityHelpers.getIdentityProviderById(providerId) } returns
            Uni.createFrom().failure(
                ServiceException(SecurityHelpers::getIdentityProviderById, "identity provider not found"),
            )
        val context = routingContextFor(LOGIN_PATH, providerId.toString())

        // When
        val result = { resolver.resolve(context, requestContext()) }

        // Then
        asserter.assertFailedWith(result, ServiceException::class.java)
    }

    @Test
    @TestSecurity(user = "test")
    @RunOnVertxContext
    @DisplayName("resolve/unauthorised: known provider resolves configuration")
    fun resolveUnauthorisedKnownProvider(asserter: UniAsserter) {
        // Given
        every { securityHelpers.getIdentityProviderById(providerId) } returns
            Uni.createFrom().item(provider())
        val context = routingContextFor(LOGIN_PATH, providerId.toString())

        // When
        val result = { resolver.resolve(context, requestContext()) }

        // Then
        asserter.assertThat(result) { actual ->
            val configuration = checkNotNull(actual)
            assertThat(configuration.tenantId().get()).isEqualTo(providerId.toString())
            assertThat(configuration.authServerUrl().get()).isEqualTo("https://www.example.com")
            assertThat(configuration.clientId().get()).isEqualTo("client-id")
            assertThat(configuration.credentials().secret().get()).isEqualTo("secret-ref")
            assertThat(configuration.applicationType().get().toString()).isEqualTo("HYBRID")
            assertThat(configuration.discoveryEnabled().get()).isTrue()
        }
    }

    @Test
    @TestSecurity(user = "test")
    @RunOnVertxContext
    @DisplayName("resolve/unauthorised: non-login path resolves no tenant")
    fun resolveUnauthorisedNonLoginPath(asserter: UniAsserter) {
        // Given
        val context = routingContextFor("${API_PATH}organisation", null)

        // When
        val result = { resolver.resolve(context, requestContext()) }

        // Then
        asserter.assertNull(result)
    }

    private fun provider(): OrganisationIdentityProvider =
        OrganisationIdentityProvider(
            id = providerId,
            name = "Test provider",
            url = "https://www.example.com",
            clientId = "client-id",
            enabled = true,
        ).apply { clientSecret = "secret-ref" }

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
    private val platformConfig = mockk<PlatformConfig>(relaxed = true)

    @Test
    fun `an authentication path resolves the platform schema`() {
        // Given
        val producer = OrganisationTenantResolverProducer(platformConfig, routingContextFor("${AUTH_PATH}login"))

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
        val producer =
            OrganisationTenantResolverProducer(
                platformConfig,
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
        val producer = OrganisationTenantResolverProducer(platformConfig, routingContextFor("${API_PATH}organisation"))

        // When / Then
        assertThrows<InvalidParameterException> { producer.get() }
    }

    @Test
    fun `an invalid organisation header is rejected`() {
        // Given
        val producer =
            OrganisationTenantResolverProducer(
                platformConfig,
                routingContextFor("${API_PATH}organisation", "not-a-uuid")
            )

        // When / Then
        assertThrows<InvalidParameterException> { producer.get() }
    }

    @Test
    fun `the fallback is used when there is no routing context`() {
        // Given
        val producer = OrganisationTenantResolverProducer(platformConfig, noRoutingContext())
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
        val producer =
            OrganisationTenantResolverProducer(
                platformConfig,
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
        val producer = OrganisationTenantResolverProducer(platformConfig, noRoutingContext())

        // When / Then
        assertThat(producer.get()).isNull()
    }

    private fun routingContextFor(
        path: String,
        organisationHeader: String? = null,
    ): RoutingContext {
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
@QuarkusTest
class PlatformAuthoriserTest {
    private val authoriser =
        PlatformAuthoriser(mockk<TenantResolver>(relaxed = true))

    @Test
    @TestSecurity(user = "test")
    @RunOnVertxContext
    @DisplayName("checkOrganisationAccess/authorised: succeeds")
    fun checkOrganisationAccessAuthorised(asserter: UniAsserter) {
        // When / Then
        asserter.assertThat({ authoriser.checkOrganisationAccess() }) { result ->
            assertThat(result).isEqualTo(Unit)
        }
    }

    @Test
    @TestSecurity(user = "test")
    @RunOnVertxContext
    @DisplayName("checkPlatformAdminOrSystem/authorised: succeeds")
    fun checkPlatformAdminOrSystemAuthorised(asserter: UniAsserter) {
        // When / Then
        asserter.assertThat({ authoriser.checkPlatformAdminOrSystem() }) { result ->
            assertThat(result).isEqualTo(Unit)
        }
    }

    @Test
    @TestSecurity(user = "test")
    @RunOnVertxContext
    @DisplayName("checkOrganisationAdminOrPlatformAdminOrSystem/authorised: succeeds")
    fun checkOrganisationAdminOrPlatformAdminOrSystemAuthorised(asserter: UniAsserter) {
        // When / Then
        asserter.assertThat({ authoriser.checkOrganisationAdminOrPlatformAdminOrSystem() }) { result ->
            assertThat(result).isEqualTo(Unit)
        }
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

/**
 * Behaviour of [SecurityHelpers] when merging the default organisation's
 * identity providers with the current organisation's providers.
 */
@QuarkusTest
class SecurityHelpersTest {

    private val defaultOrganisationId =
        UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val otherOrganisationId =
        UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val defaultProvider = provider(
        UUID.fromString("33333333-3333-3333-3333-333333333333"),
        "Default provider",
    )
    private val otherProvider = provider(
        UUID.fromString("44444444-4444-4444-4444-444444444444"),
        "Other provider",
    )

    private lateinit var configurationService: ConfigurationService
    private lateinit var currentIdentityAssociation: CurrentIdentityAssociation
    private lateinit var identityProviderIndexService: IdentityProviderIndexService
    private lateinit var organisationService: OrganisationService
    private lateinit var organisationTenantResolverProducer: OrganisationTenantResolverProducer
    private lateinit var tenantResolver: TenantResolver
    private lateinit var helpers: SecurityHelpers

    private val providerCalls = mutableListOf<String?>()
    private val stateCalls = mutableListOf<String?>()
    private val settingCalls = mutableListOf<String?>()
    private val identityProviders = mutableMapOf<String, List<OrganisationIdentityProvider>>()

    @BeforeEach
    fun setUp() {
        configurationService = mockk()
        every { configurationService.defaultOrganisationId } returns defaultOrganisationId

        currentIdentityAssociation = mockk(relaxed = true)
        identityProviderIndexService = mockk()
        organisationService = mockk()
        tenantResolver = mockk()
        organisationTenantResolverProducer =
            OrganisationTenantResolverProducer(
                mockk<PlatformConfig>(relaxed = true),
                mockk<RoutingContext>(relaxed = true),
            )

        every { tenantResolver.resolveTenantId() } returns otherOrganisationId.toString()

        helpers =
            SecurityHelpers(
                configurationService,
                currentIdentityAssociation,
                identityProviderIndexService,
                organisationService,
                organisationTenantResolverProducer,
                tenantResolver,
            )

        every { organisationService.getIdentityProviders(true) } answers {
            val tenant = organisationTenantResolverProducer.tenantFallback
            providerCalls.add(tenant)
            Uni.createFrom().item(
                checkNotNull(identityProviders[tenant]) {
                    "No identity providers configured for tenant $tenant"
                },
            )
        }

        providerCalls.clear()
        stateCalls.clear()
        settingCalls.clear()
        identityProviders.clear()
    }

    @Test
    @DisplayName("listIdentityProviders/current-is-default: providers are returned once")
    @RunOnVertxContext
    fun currentIsDefaultWithProviders(asserter: UniAsserter) {
        // Given
        every { tenantResolver.resolveTenantId() } returns defaultOrganisationId.toString()
        givenIdentityProviders(defaultOrganisationId, listOf(defaultProvider))

        // When
        val result = { helpers.listIdentityProviders() }

        // Then
        asserter.assertThat(result) { providers ->
            assertThat(providers).containsExactly(
                OrganisationIdentityProviderHeaderWithSource(defaultProvider, true),
            )
            assertThat(providerCalls).containsExactly(defaultOrganisationId.toString())
            assertThat(stateCalls).isEmpty()
            assertThat(settingCalls).isEmpty()
            verify(exactly = 1) { organisationService.getIdentityProviders(true) }
        }
    }

    @Test
    @DisplayName("listIdentityProviders/current-is-default: no providers fails")
    @RunOnVertxContext
    fun currentIsDefaultWithoutProviders(asserter: UniAsserter) {
        // Given
        every { tenantResolver.resolveTenantId() } returns defaultOrganisationId.toString()
        givenIdentityProviders(defaultOrganisationId, emptyList())

        // When
        val result = { helpers.listIdentityProviders() }

        // Then
        asserter.assertFailedWith(result) { failure ->
            assertThat(failure).isInstanceOf(ServiceException::class.java)
            assertThat(providerCalls).containsExactly(defaultOrganisationId.toString())
            assertThat(stateCalls).isEmpty()
            assertThat(settingCalls).isEmpty()
            verify(exactly = 1) { organisationService.getIdentityProviders(true) }
        }
    }

    @Test
    @DisplayName("listIdentityProviders/default-included: current and default providers are returned")
    @RunOnVertxContext
    fun otherOrganisationIncludesDefault(asserter: UniAsserter) {
        // Given
        givenIdentityProviders(defaultOrganisationId, listOf(defaultProvider))
        givenIdentityProviders(otherOrganisationId, listOf(otherProvider))
        givenCurrentStateAndSetting(OrganisationState.ACTIVE, true)

        // When
        val result = { helpers.listIdentityProviders() }

        // Then
        asserter.assertThat(result) { providers ->
            assertThat(providers).containsExactly(
                OrganisationIdentityProviderHeaderWithSource(otherProvider, false),
                OrganisationIdentityProviderHeaderWithSource(defaultProvider, true),
            )
            assertThat(providerCalls).containsExactly(
                otherOrganisationId.toString(),
                defaultOrganisationId.toString(),
            )
            assertThat(stateCalls).containsExactly(otherOrganisationId.toString())
            assertThat(settingCalls).containsExactly(otherOrganisationId.toString())
        }
    }

    @Test
    @DisplayName("listIdentityProviders/default-excluded: only current providers are returned")
    @RunOnVertxContext
    fun otherOrganisationExcludesDefault(asserter: UniAsserter) {
        // Given
        givenIdentityProviders(defaultOrganisationId, listOf(defaultProvider))
        givenIdentityProviders(otherOrganisationId, listOf(otherProvider))
        givenCurrentStateAndSetting(OrganisationState.ACTIVE, false)

        // When
        val result = { helpers.listIdentityProviders() }

        // Then
        asserter.assertThat(result) { providers ->
            assertThat(providers).containsExactly(
                OrganisationIdentityProviderHeaderWithSource(otherProvider, false),
            )
            assertThat(providerCalls).containsExactly(otherOrganisationId.toString())
            assertThat(stateCalls).containsExactly(otherOrganisationId.toString())
            assertThat(settingCalls).containsExactly(otherOrganisationId.toString())
        }
    }

    private fun givenIdentityProviders(
        organisationId: UUID,
        providers: List<OrganisationIdentityProvider>,
    ) {
        identityProviders[organisationId.toString()] = providers
    }

    private fun givenCurrentStateAndSetting(state: OrganisationState, includeDefault: Boolean) {
        every { organisationService.state() } answers {
            stateCalls.add(organisationTenantResolverProducer.tenantFallback)
            Uni.createFrom().item(state)
        }
        every {
            organisationService.getSetting(OrganisationSettingKey.IncludeDefaultAuthenticationMethods)
        } answers {
            settingCalls.add(organisationTenantResolverProducer.tenantFallback)
            Uni.createFrom().item(includeDefault)
        }
    }

    private fun provider(id: UUID, name: String): OrganisationIdentityProvider =
        OrganisationIdentityProvider(
            id = id,
            name = name,
            url = "https://$name.example.com",
            clientId = "client-$id",
            enabled = true,
        )
}

/**
 * Behaviour of [SecurityHelpers.getIdentityProviderById].
 */
@QuarkusTest
class SecurityHelpersIdentityProviderTest {

    private val defaultOrganisationId =
        UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val otherOrganisationId =
        UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val providerId =
        UUID.fromString("33333333-3333-3333-3333-333333333333")

    private lateinit var configurationService: ConfigurationService
    private lateinit var currentIdentityAssociation: CurrentIdentityAssociation
    private lateinit var identityProviderIndexService: IdentityProviderIndexService
    private lateinit var organisationService: OrganisationService
    private lateinit var organisationTenantResolverProducer: OrganisationTenantResolverProducer
    private lateinit var tenantResolver: TenantResolver
    private lateinit var helpers: SecurityHelpers

    private val providerCalls = mutableListOf<String?>()
    private val indexScopes = mutableListOf<Set<UUID>>()

    @BeforeEach
    fun setUp() {
        configurationService = mockk()
        every { configurationService.defaultOrganisationId } returns defaultOrganisationId

        currentIdentityAssociation = mockk(relaxed = true)
        identityProviderIndexService = mockk()
        organisationService = mockk()
        tenantResolver = mockk()
        organisationTenantResolverProducer =
            OrganisationTenantResolverProducer(
                mockk<PlatformConfig>(relaxed = true),
                mockk<RoutingContext>(relaxed = true),
            )

        every { tenantResolver.resolveTenantId() } returns otherOrganisationId.toString()

        helpers =
            SecurityHelpers(
                configurationService,
                currentIdentityAssociation,
                identityProviderIndexService,
                organisationService,
                organisationTenantResolverProducer,
                tenantResolver,
            )

        providerCalls.clear()
        indexScopes.clear()
    }

    @Test
    @DisplayName("getIdentityProviderById/current-organisation: provider is resolved in the current scope")
    @RunOnVertxContext
    fun providerInCurrentOrganisation(asserter: UniAsserter) {
        // Given
        val provider = provider("Current provider")
        givenIndexEntry(otherOrganisationId)
        every { organisationService.getIdentityProvider(providerId, true) } answers {
            providerCalls.add(organisationTenantResolverProducer.tenantFallback)
            Uni.createFrom().item(provider)
        }

        // When
        val result = { helpers.getIdentityProviderById(providerId) }

        // Then
        asserter.assertThat(result) { actual ->
            assertThat(actual).isEqualTo(provider)
            assertThat(indexScopes).containsExactly(
                setOf(defaultOrganisationId, otherOrganisationId),
            )
            assertThat(providerCalls).containsExactly(otherOrganisationId.toString())
        }
    }

    @Test
    @DisplayName("getIdentityProviderById/default-organisation: provider is resolved in the default scope")
    @RunOnVertxContext
    fun providerInDefaultOrganisation(asserter: UniAsserter) {
        // Given
        val provider = provider("Default provider")
        givenIndexEntry(defaultOrganisationId)
        every { organisationService.getIdentityProvider(providerId, true) } answers {
            providerCalls.add(organisationTenantResolverProducer.tenantFallback)
            Uni.createFrom().item(provider)
        }

        // When
        val result = { helpers.getIdentityProviderById(providerId) }

        // Then
        asserter.assertThat(result) { actual ->
            assertThat(actual).isEqualTo(provider)
            assertThat(indexScopes).containsExactly(
                setOf(defaultOrganisationId, otherOrganisationId),
            )
            assertThat(providerCalls).containsExactly(defaultOrganisationId.toString())
        }
    }

    @Test
    @DisplayName("getIdentityProviderById/not-found: provider is rejected")
    @RunOnVertxContext
    fun providerNotFound(asserter: UniAsserter) {
        // Given
        every { identityProviderIndexService.get(providerId, any()) } answers {
            indexScopes.add(secondArg())
            Uni.createFrom().nullItem()
        }

        // When
        val result = { helpers.getIdentityProviderById(providerId) }

        // Then
        asserter.assertFailedWith(result) { failure ->
            assertThat(failure).isInstanceOf(ServiceException::class.java)
            assertThat(indexScopes).containsExactly(
                setOf(defaultOrganisationId, otherOrganisationId),
            )
            verify(exactly = 0) { organisationService.getIdentityProvider(providerId, true) }
        }
    }

    @Test
    @DisplayName("getIdentityProviderById/disabled: provider is rejected")
    @RunOnVertxContext
    fun providerDisabled(asserter: UniAsserter) {
        // Given
        givenIndexEntry(otherOrganisationId)
        every { organisationService.getIdentityProvider(providerId, true) } answers {
            providerCalls.add(organisationTenantResolverProducer.tenantFallback)
            Uni.createFrom().failure(
                ServiceException(OrganisationService::getIdentityProvider, "identity provider not found"),
            )
        }

        // When
        val result = { helpers.getIdentityProviderById(providerId) }

        // Then
        asserter.assertFailedWith(result) { failure ->
            assertThat(failure).isInstanceOf(ServiceException::class.java)
            assertThat(providerCalls).containsExactly(otherOrganisationId.toString())
        }
    }

    private fun givenIndexEntry(organisationId: UUID) {
        every { identityProviderIndexService.get(providerId, any()) } answers {
            indexScopes.add(secondArg())
            Uni.createFrom().item(
                IdentityProviderIndex(
                    identityProviderId = providerId,
                    organisationId = organisationId,
                ),
            )
        }
    }

    private fun provider(name: String): OrganisationIdentityProvider =
        OrganisationIdentityProvider(
            id = providerId,
            name = name,
            url = "https://$name.example.com",
            clientId = "client-$providerId",
            enabled = true,
        )
}
