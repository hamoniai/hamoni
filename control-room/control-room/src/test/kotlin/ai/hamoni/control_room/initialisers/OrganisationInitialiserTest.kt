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

package ai.hamoni.control_room.initialisers

import ai.hamoni.common.platform.PlatformConfig
import ai.hamoni.control_room.OrganisationTenantResolverProducer
import ai.hamoni.control_room.data.Organisation
import ai.hamoni.control_room.data.OrganisationState
import ai.hamoni.control_room.service.OrganisationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.quarkus.arc.Arc
import io.quarkus.runtime.StartupEvent
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import io.vertx.mutiny.core.Vertx
import jakarta.inject.Inject
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Behaviour of the default-organisation initialiser.
 */
@QuarkusTest
class OrganisationInitialiserTest {

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var json: Json

    @TempDir
    lateinit var tempDir: Path

    private val organisationId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    private lateinit var schemaPath: Path
    private lateinit var defaultsPath: Path

    @BeforeEach
    fun writeInputFiles() {
        schemaPath = tempDir.resolve("organisation-import.schema.json")
        defaultsPath = tempDir.resolve("organisation-init.json")
        Files.writeString(schemaPath, schema())
        Files.writeString(defaultsPath, defaults())
    }

    @Test
    fun `a not-initialised organisation is created inside the request scope`() {
        // Given
        val service = mockk<OrganisationService>()
        val producer = mockk<OrganisationTenantResolverProducer>(relaxed = true)
        val created = AtomicReference<Organisation>()
        var requestScopeWasActive = false

        every { service.state() } returns Uni.createFrom().item(OrganisationState.NOT_INITIALISED)
        every { service.create(any()) } answers {
            requestScopeWasActive = Arc.container().requestContext().isActive
            created.set(firstArg())
            Uni.createFrom().item(firstArg())
        }

        // When
        initialiser(service, producer).onStart(mockk<StartupEvent>())

        // Then
        assertThat(requestScopeWasActive).isTrue()
        val organisation = checkNotNull(created.get())
        assertThat(organisation.name).isEqualTo("Example Organisation")
        verify { producer.tenantFallback = organisationId.toString() }
    }

    @Test
    fun `an active organisation is not initialised again`() {
        // Given
        val service = mockk<OrganisationService>()
        val producer = mockk<OrganisationTenantResolverProducer>(relaxed = true)
        every { service.state() } returns Uni.createFrom().item(OrganisationState.ACTIVE)

        // When
        initialiser(service, producer).onStart(mockk<StartupEvent>())

        // Then
        verify(exactly = 0) { service.create(any()) }
    }

    @Test
    fun `a disabled organisation is not initialised`() {
        // Given
        val service = mockk<OrganisationService>()
        val producer = mockk<OrganisationTenantResolverProducer>(relaxed = true)
        every { service.state() } returns Uni.createFrom().item(OrganisationState.DISABLED)

        // When
        initialiser(service, producer).onStart(mockk<StartupEvent>())

        // Then
        verify(exactly = 0) { service.create(any()) }
    }

    @Test
    fun `a missing schema prevents the organisation being created`() {
        // Given
        schemaPath = tempDir.resolve("missing-schema.json")
        val service = mockk<OrganisationService>()
        val producer = mockk<OrganisationTenantResolverProducer>(relaxed = true)
        every { service.state() } returns Uni.createFrom().item(OrganisationState.NOT_INITIALISED)

        // When
        initialiser(service, producer).onStart(mockk<StartupEvent>())

        // Then
        verify(exactly = 0) { service.create(any()) }
    }

    private fun initialiser(
        service: OrganisationService,
        producer: OrganisationTenantResolverProducer,
    ): OrganisationInitialiser =
        OrganisationInitialiser(
            service,
            producer,
            platformConfig(),
            vertx,
            schemaPath.toString(),
            json,
        )

    private fun platformConfig(): PlatformConfig {
        val organisationConfig = mockk<PlatformConfig.DefaultOrganisationConfig>()
        every { organisationConfig.id() } returns organisationId
        every { organisationConfig.config() } returns Optional.of(defaultsPath.toString())

        return mockk {
            every { defaultOrganisation() } returns organisationConfig
        }
    }

    private fun defaults(): String =
        """
        {
          "schemaVersion": "test-version",
          "defaultOrganisation": {
            "name": "Example Organisation",
            "enabled": true,
            "settings": [],
            "identityProviders": []
          }
        }
        """.trimIndent()

    private fun schema(): String =
        """
        {
          "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
          "${'$'}id": "https://hamoni.ai/schema/organisation-init/test",
          "type": "object",
          "additionalProperties": false,
          "required": ["schemaVersion", "defaultOrganisation"],
          "properties": {
            "schemaVersion": {
              "const": "test-version"
            },
            "defaultOrganisation": {
              "type": "object"
            }
          }
        }
        """.trimIndent()
}
