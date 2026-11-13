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

package ai.hamoni.common.core.shared

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Tests the stored-text conversion of the supported setting value types.
 */
class TypedSettingTypeTest {

    @Test
    fun `boolean values round-trip`() {
        // Given
        val type = TypedSettingType.BooleanType

        // When
        val stored = type.encode(true)

        // Then
        assertThat(type.decode(stored)).isEqualTo(true)
    }

    @Test
    fun `string values round-trip`() {
        // Given
        val type = TypedSettingType.StringType

        // When
        val stored = type.encode("GB")

        // Then
        assertThat(type.decode(stored)).isEqualTo("GB")
    }

    @Test
    fun `UUID values round-trip`() {
        // Given
        val type = TypedSettingType.UUIDType
        val value = UUID.fromString("00000000-0000-0000-0000-000000000001")

        // When
        val stored = type.encode(value)

        // Then
        assertThat(type.decode(stored)).isEqualTo(value)
    }
}
