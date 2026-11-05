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

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.mapping.Table
import org.junit.jupiter.api.Test

/**
 * Behaviour of [PlatformSchemaFilter].
 */
class PlatformSchemaFilterTest {

    private val filter = PlatformSchemaFilter()

    @Test
    fun `a platform table is included`() {
        // Given
        val table = Table("authnprofile").apply { schema = "platform" }

        // When
        val included = filter.includeTable(table)

        // Then
        assertThat(included).isTrue()
    }

    @Test
    fun `a table without a schema is excluded`() {
        // Given
        val table = Table("organisation")

        // When
        val included = filter.includeTable(table)

        // Then
        assertThat(included).isFalse()
    }

    @Test
    fun `a table in another schema is excluded`() {
        // Given
        val table = Table("audit").apply { schema = "audit" }

        // When
        val included = filter.includeTable(table)

        // Then
        assertThat(included).isFalse()
    }
}

/**
 * Behaviour of [OrganisationSchemaFilter].
 */
class OrganisationSchemaFilterTest {

    private val filter = OrganisationSchemaFilter()

    @Test
    fun `a platform table is excluded`() {
        // Given
        val table = Table("authnprofile").apply { schema = "platform" }

        // When
        val included = filter.includeTable(table)

        // Then
        assertThat(included).isFalse()
    }

    @Test
    fun `a table without a schema is included`() {
        // Given
        val table = Table("organisation")

        // When
        val included = filter.includeTable(table)

        // Then
        assertThat(included).isTrue()
    }

    @Test
    fun `a table in another schema is included`() {
        // Given
        val table = Table("audit").apply { schema = "audit" }

        // When
        val included = filter.includeTable(table)

        // Then
        assertThat(included).isTrue()
    }
}

/**
 * Behaviour of [PlatformSchemaFilterProvider].
 */
class PlatformSchemaFilterProviderTest {

    private val provider = PlatformSchemaFilterProvider()

    @Test
    fun `the provider supplies the platform filter for every action`() {
        // Given / When / Then
        assertThat(provider.createFilter).isInstanceOf(PlatformSchemaFilter::class.java)
        assertThat(provider.dropFilter).isInstanceOf(PlatformSchemaFilter::class.java)
        assertThat(provider.truncatorFilter).isInstanceOf(PlatformSchemaFilter::class.java)
        assertThat(provider.migrateFilter).isInstanceOf(PlatformSchemaFilter::class.java)
        assertThat(provider.validateFilter).isInstanceOf(PlatformSchemaFilter::class.java)
    }
}

/**
 * Behaviour of [OrganisationSchemaFilterProvider].
 */
class OrganisationSchemaFilterProviderTest {

    private val provider = OrganisationSchemaFilterProvider()

    @Test
    fun `the provider supplies the organisation filter for every action`() {
        // Given / When / Then
        assertThat(provider.createFilter).isInstanceOf(OrganisationSchemaFilter::class.java)
        assertThat(provider.dropFilter).isInstanceOf(OrganisationSchemaFilter::class.java)
        assertThat(provider.truncatorFilter).isInstanceOf(OrganisationSchemaFilter::class.java)
        assertThat(provider.migrateFilter).isInstanceOf(OrganisationSchemaFilter::class.java)
        assertThat(provider.validateFilter).isInstanceOf(OrganisationSchemaFilter::class.java)
    }
}
