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
package ai.hamoni.control_room.command

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Behaviour of the organisation import schema generator.
 */
class JsonSchemaExportServiceTest {

    @Test
    fun `the root allows a default organisation and additional organisations`(
        @TempDir tempDir: Path,
    ) {
        // Given
        val input = tempDir.resolve("openapi.json")
        input.toFile().writeText(
            """
            {
              "components": {
                "schemas": {
                  "Organisation": {
                    "type": "object",
                    "properties": {
                      "name": {
                        "type": "string"
                      }
                    },
                    "required": ["name"]
                  }
                }
              }
            }
            """.trimIndent(),
        )
        val output = tempDir.resolve("organisation-import.schema.json")
        val service = JsonSchemaExportService(
            input.toString(),
            output.toString(),
            "test-version",
        )

        // When
        service.generate()

        // Then
        val schema = Json.parseToJsonElement(output.toFile().readText()).jsonObject
        val required = schema["required"]?.jsonArray?.map { it.jsonPrimitive.content }
        val properties = schema["properties"]?.jsonObject
        val definitions = schema["\$defs"]?.jsonObject

        assertThat(required).containsExactly("schemaVersion", "defaultOrganisation")
        assertThat(properties).containsKeys("schemaVersion", "defaultOrganisation", "organisations")
        assertThat(properties?.get("organisations")?.jsonObject?.get("type")?.jsonPrimitive?.content)
            .isEqualTo("array")

        val additional = definitions?.get("OrganisationWithIdentifier")?.jsonObject
        val additionalRequired = additional?.get("required")?.jsonArray?.map { it.jsonPrimitive.content }
        val additionalProperties = additional?.get("properties")?.jsonObject

        assertThat(additionalRequired).contains("id")
        assertThat(additionalProperties?.get("id")?.jsonObject?.get("format")?.jsonPrimitive?.content)
            .isEqualTo("uuid")
    }
}
