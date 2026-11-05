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

import ai.hamoni.common.core.PLATFORM_SCHEMA
import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.relational.Namespace
import org.hibernate.boot.model.relational.Sequence
import org.hibernate.mapping.Table
import org.hibernate.tool.schema.spi.SchemaFilter
import org.hibernate.tool.schema.spi.SchemaFilterProvider

/**
 * The two schema scopes exported from the JPA model.
 *
 * [directoryName] is also the migration directory under `db/migration`.
 *
 * @param directoryName the source-tree directory the generated script is copied to
 * @param filterProvider the provider that limits Hibernate's schema export to this scope
 */
enum class SchemaScope(
    val directoryName: String,
    val filterProvider: Class<out SchemaFilterProvider>
) {
    PLATFORM("platform", PlatformSchemaFilterProvider::class.java),
    ORGANISATION("organisation", OrganisationSchemaFilterProvider::class.java)
}

/**
 * Include only the explicit platform schema.
 *
 * Platform entities carry `@Table(schema = PLATFORM_SCHEMA)`.
 */
class PlatformSchemaFilter : SchemaFilter {

    override fun includeNamespace(namespace: Namespace): Boolean =
        namespace.name.schema().isPlatformSchema()

    override fun includeTable(table: Table): Boolean =
        table.schema.isPlatformSchema()

    override fun includeSequence(sequence: Sequence): Boolean =
        sequence.name.schemaName.isPlatformSchema()
}

/**
 * Include everything that is not the platform schema.
 *
 * The organisation script is applied to each organisation's schema by Flyway, so
 * it contains the default schema and any future non-platform schema.
 */
class OrganisationSchemaFilter : SchemaFilter {

    override fun includeNamespace(namespace: Namespace): Boolean =
        !namespace.name.schema().isPlatformSchema()

    override fun includeTable(table: Table): Boolean =
        !table.schema.isPlatformSchema()

    override fun includeSequence(sequence: Sequence): Boolean =
        !sequence.name.schemaName.isPlatformSchema()
}

/**
 * Supply the platform filter to Hibernate's schema tool.
 *
 * Hibernate instantiates this provider by class name from
 * `hibernate.hbm2ddl.schema_filter_provider`, so it needs a public no-argument
 * constructor.
 */
class PlatformSchemaFilterProvider : SchemaFilterProvider {

    private val filter = PlatformSchemaFilter()

    override fun getCreateFilter(): SchemaFilter = filter

    override fun getDropFilter(): SchemaFilter = filter

    override fun getTruncatorFilter(): SchemaFilter = filter

    override fun getMigrateFilter(): SchemaFilter = filter

    override fun getValidateFilter(): SchemaFilter = filter
}

/**
 * Supply the organisation filter to Hibernate's schema tool.
 *
 * Hibernate instantiates this provider by class name from
 * `hibernate.hbm2ddl.schema_filter_provider`, so it needs a public no-argument
 * constructor.
 */
class OrganisationSchemaFilterProvider : SchemaFilterProvider {

    private val filter = OrganisationSchemaFilter()

    override fun getCreateFilter(): SchemaFilter = filter

    override fun getDropFilter(): SchemaFilter = filter

    override fun getTruncatorFilter(): SchemaFilter = filter

    override fun getMigrateFilter(): SchemaFilter = filter

    override fun getValidateFilter(): SchemaFilter = filter
}

/**
 * Whether the identifier names the explicit platform schema.
 */
private fun Identifier?.isPlatformSchema(): Boolean =
    this?.text.equals(PLATFORM_SCHEMA, ignoreCase = true)

/**
 * Whether the name is the explicit platform schema.
 */
private fun String?.isPlatformSchema(): Boolean =
    this.equals(PLATFORM_SCHEMA, ignoreCase = true)
