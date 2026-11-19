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
package ai.hamoni.control_room.initialisers

import ai.hamoni.control_room.data.OrganisationSettingKey
import ai.hamoni.control_room.exception.OrganisationInitialisationException
import io.vertx.core.json.DecodeException
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Behaviour of the organisation JSON loader.
 */
class OrganisationLoaderTest {

    private val json = Json
    private val loader = OrganisationLoader.createLoader(json, schema())

    @Test
    fun `a valid document is deserialised into an organisation`() {
        // When
        val organisation = loader.loadOrganisation(validDocument())

        // Then
        assertThat(organisation.name).isEqualTo("Example Organisation")
        assertThat(organisation.enabled).isTrue()
        assertThat(organisation.settings).hasSize(1)
        assertThat(organisation.settings[0].key).isEqualTo(OrganisationSettingKey.Country)
        assertThat(organisation.settings[0].value).isEqualTo("GB")
        assertThat(organisation.identityProviders).isEmpty()
    }

    @Test
    fun `an unsupported schema version is rejected`() {
        // Given
        val document = validDocument().replace("test-version", "other-version")

        // When / Then
        assertThrows<OrganisationInitialisationException> {
            loader.loadOrganisation(document)
        }
    }

    @Test
    fun `a document without an organisation is rejected`() {
        // Given
        val document = """{"schemaVersion":"test-version"}"""

        // When / Then
        assertThrows<OrganisationInitialisationException> {
            loader.loadOrganisation(document)
        }
    }

    @Test
    fun `malformed JSON is rejected`() {
        // When / Then
        assertThrows<DecodeException> {
            loader.loadOrganisation("not-json")
        }
    }

    private fun validDocument(): String =
        """
        {
          "schemaVersion": "test-version",
          "defaultOrganisation": {
            "name": "Example Organisation",
            "enabled": true,
            "settings": [
              {
                "key": "country",
                "value": "GB"
              }
            ],
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
