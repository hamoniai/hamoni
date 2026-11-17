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

import ai.hamoni.common.core.exception.ServiceException
import ai.hamoni.control_room.OrganisationTenantResolverProducer
import ai.hamoni.control_room.PlatformAuthoriser
import ai.hamoni.control_room.data.IdentityProviderIndex
import ai.hamoni.control_room.data.Organisation
import ai.hamoni.control_room.data.OrganisationIdentityProvider
import ai.hamoni.control_room.data.OrganisationState
import ai.hamoni.control_room.repository.IdentityProviderIndexRepository
import ai.hamoni.control_room.repository.OrganisationIdentityProviderRepository
import ai.hamoni.control_room.repository.OrganisationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheQuery
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.vertx.RunOnVertxContext
import io.quarkus.test.vertx.UniAsserter
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

/**
 * Behaviour of the organisation service.
 */
@QuarkusTest
class OrganisationServiceTest {

    @Inject
    lateinit var service: OrganisationService

    @Inject
    lateinit var tenantResolverProducer: OrganisationTenantResolverProducer

    @Inject
    lateinit var dataSource: DataSource

    @InjectMock
    lateinit var organisationRepository: OrganisationRepository

    @InjectMock
    lateinit var identityProviderIndexRepository: IdentityProviderIndexRepository

    @InjectMock
    lateinit var organisationIdentityProviderRepository: OrganisationIdentityProviderRepository

    @InjectMock
    lateinit var platformAuthoriser: PlatformAuthoriser

    private val organisationId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val providerId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @BeforeEach
    fun createTenantSchema() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("""CREATE SCHEMA IF NOT EXISTS "$organisationId"""")
                statement.execute("""DROP TABLE IF EXISTS "$organisationId"."organisation"""")
                statement.execute("""CREATE TABLE "$organisationId"."organisation" ("enabled" BOOLEAN NOT NULL)""")
            }
        }
    }

    @Test
    @DisplayName("addIdentityProvider/authorised: create success")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun addIdentityProviderAuthorisedCreateSuccess(asserter: UniAsserter) {
        // Given
        val organisationIdentityProvider = provider()
        val indexSlot = slot<IdentityProviderIndex>()
        val providerSlot = slot<OrganisationIdentityProvider>()

        every { platformAuthoriser.checkOrganisationAdminOrPlatformAdminOrSystem() } returns
            Uni.createFrom().item(Unit)
        every { identityProviderIndexRepository.persistAndFlush(capture(indexSlot)) } answers {
            indexSlot.captured.identityProviderId = providerId
            Uni.createFrom().item(indexSlot.captured)
        }
        every { organisationIdentityProviderRepository.persistAndFlush(capture(providerSlot)) } answers {
            Uni.createFrom().item(providerSlot.captured)
        }

        // When
        val provider = { addIdentityProvider(organisationIdentityProvider) }

        // Then
        asserter.assertThat(provider) { result ->
            assertThat(indexSlot.captured.organisationId).isEqualTo(organisationId)
            assertThat(providerSlot.captured.id).isEqualTo(providerId)
            assertThat(result.id).isEqualTo(providerId)
        }
    }

    @Test
    @DisplayName("addIdentityProvider/unauthorised: rejected")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun addIdentityProviderUnauthorisedRejected(asserter: UniAsserter) {
        // Given
        every { platformAuthoriser.checkOrganisationAdminOrPlatformAdminOrSystem() } returns
            Uni.createFrom().failure(
                ServiceException(OrganisationService::addIdentityProvider, "denied"),
            )

        // When
        val provider = { addIdentityProvider(provider()) }

        // Then
        asserter.assertFailedWith(provider) { failure ->
            assertThat(failure).isInstanceOf(ServiceException::class.java)
            verify(exactly = 0) { identityProviderIndexRepository.persistAndFlush(any()) }
            verify(exactly = 0) { organisationIdentityProviderRepository.persistAndFlush(any()) }
        }
    }

    @Test
    @DisplayName("create/authorised: organisation id is taken from the tenant")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun createAuthorisedTenantIdentifier(asserter: UniAsserter) {
        // Given
        val organisation = Organisation(name = "Example Organisation", enabled = true)
        val organisationSlot = slot<Organisation>()

        every { platformAuthoriser.checkPlatformAdminOrSystem() } returns Uni.createFrom().item(Unit)
        every { organisationRepository.persistAndFlush(capture(organisationSlot)) } answers {
            Uni.createFrom().item(organisationSlot.captured)
        }

        // When
        val created = { create(organisation) }

        // Then
        asserter.assertThat(created) { result ->
            assertThat(result.id).isEqualTo(organisationId)
            assertThat(organisationSlot.captured.id).isEqualTo(organisationId)
        }
    }

    @Test
    @DisplayName("create/authorised: providers are added before the organisation")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun createAuthorisedProviders(asserter: UniAsserter) {
        // Given
        val provider = provider()
        val organisation =
            Organisation(name = "Example Organisation", enabled = true).apply {
                identityProviders = mutableListOf(provider)
            }
        val indexSlot = slot<IdentityProviderIndex>()
        val providerSlot = slot<OrganisationIdentityProvider>()
        val organisationSlot = slot<Organisation>()

        every { platformAuthoriser.checkPlatformAdminOrSystem() } returns Uni.createFrom().item(Unit)
        every { platformAuthoriser.checkOrganisationAdminOrPlatformAdminOrSystem() } returns
            Uni.createFrom().item(Unit)
        every { identityProviderIndexRepository.persistAndFlush(capture(indexSlot)) } answers {
            indexSlot.captured.identityProviderId = providerId
            Uni.createFrom().item(indexSlot.captured)
        }
        every { organisationIdentityProviderRepository.persistAndFlush(capture(providerSlot)) } answers {
            Uni.createFrom().item(providerSlot.captured)
        }
        every { organisationRepository.persistAndFlush(capture(organisationSlot)) } answers {
            Uni.createFrom().item(organisationSlot.captured)
        }

        // When
        val created = { create(organisation) }

        // Then
        asserter.assertThat(created) { result ->
            assertThat(providerSlot.captured.id).isEqualTo(providerId)
            assertThat(organisationSlot.captured.identityProviders.single().id).isEqualTo(providerId)
            assertThat(result.id).isEqualTo(organisationId)
            verifyOrder {
                identityProviderIndexRepository.persistAndFlush(any())
                organisationIdentityProviderRepository.persistAndFlush(any())
                organisationRepository.persistAndFlush(any())
            }
        }
    }

    @Test
    @DisplayName("create/unauthorised: organisation is not persisted")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun createUnauthorised(asserter: UniAsserter) {
        // Given
        every { platformAuthoriser.checkPlatformAdminOrSystem() } returns
            Uni.createFrom().failure(
                ServiceException(OrganisationService::create, "denied"),
            )
        val organisation = Organisation(name = "Example Organisation", enabled = true)

        // When
        val created = { create(organisation) }

        // Then
        asserter.assertFailedWith(created) { failure ->
            assertThat(failure).isInstanceOf(ServiceException::class.java)
            verify(exactly = 0) { organisationRepository.persistAndFlush(any<Organisation>()) }
        }
    }

    @Test
    @DisplayName("state/unauthorised: rejected")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun stateUnauthorisedRejected(asserter: UniAsserter) {
        // Given
        every { platformAuthoriser.checkPlatformAdminOrSystem() } returns
            Uni.createFrom().failure(
                ServiceException(OrganisationService::state, "denied"),
            )

        // When
        val state = { state() }

        // Then
        asserter.assertFailedWith(state) { failure ->
            assertThat(failure).isInstanceOf(ServiceException::class.java)
        }
    }

    @Test
    @DisplayName("state/authorised: not created")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun stateAuthorisedNotCreated(asserter: UniAsserter) {
        // Given
        dropOrganisationTable()
        every { platformAuthoriser.checkPlatformAdminOrSystem() } returns Uni.createFrom().item(Unit)

        // When
        val state = { state() }

        // Then
        asserter.assertThat(state) { result ->
            assertThat(result).isEqualTo(OrganisationState.NOT_CREATED)
        }
    }

    @Test
    @DisplayName("state/authorised: not initialised")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun stateAuthorisedNotInitialised(asserter: UniAsserter) {
        assertState(asserter, OrganisationState.NOT_INITIALISED)
    }

    @Test
    @DisplayName("state/authorised: active")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun stateAuthorisedActive(asserter: UniAsserter) {
        assertState(asserter, OrganisationState.ACTIVE)
    }

    @Test
    @DisplayName("state/authorised: disabled")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun stateAuthorisedDisabled(asserter: UniAsserter) {
        assertState(asserter, OrganisationState.DISABLED)
    }

    @Test
    @DisplayName("state/authorised: error")
    @TestSecurity(user = "test")
    @RunOnVertxContext
    fun stateAuthorisedError(asserter: UniAsserter) {
        assertState(asserter, OrganisationState.ERROR)
    }

    private fun assertState(asserter: UniAsserter, expected: OrganisationState) {
        // Given
        val organisationQuery = mockk<PanacheQuery<Organisation>>(relaxed = true)
        val stateQuery = mockk<PanacheQuery<OrganisationState>>(relaxed = true)
        every { platformAuthoriser.checkPlatformAdminOrSystem() } returns Uni.createFrom().item(Unit)
        every { organisationRepository.find("#Organisation.isOrganisationInitialised") } returns organisationQuery
        every { organisationQuery.project(OrganisationState::class.java) } returns stateQuery
        every { stateQuery.singleResult() } returns Uni.createFrom().item(expected)

        // When
        val state = { state() }

        // Then
        asserter.assertThat(state) { result ->
            assertThat(result).isEqualTo(expected)
        }
    }

    private fun dropOrganisationTable() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("""DROP TABLE IF EXISTS "$organisationId"."organisation"""")
            }
        }
    }

    private fun addIdentityProvider(provider: OrganisationIdentityProvider): Uni<OrganisationIdentityProvider> =
        Uni.createFrom().deferred {
            tenantResolverProducer.tenantFallback = organisationId.toString()
            service.addIdentityProvider(provider)
        }

    private fun create(organisation: Organisation): Uni<Organisation> =
        Uni.createFrom().deferred {
            tenantResolverProducer.tenantFallback = organisationId.toString()
            service.create(organisation)
        }

    private fun state(): Uni<OrganisationState> =
        Uni.createFrom().deferred {
            tenantResolverProducer.tenantFallback = organisationId.toString()
            service.state()
        }

    private fun provider(): OrganisationIdentityProvider =
        OrganisationIdentityProvider(
            name = "Example provider",
            url = "https://issuer.example.com",
            clientId = "client-id",
            clientSecretReference = null,
            enabled = true,
        )
}
