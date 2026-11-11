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

package ai.hamoni.quarkus.ext.quarkus.multi_tenant.test

import ai.hamoni.quarkus.ext.quarkus.multi_tenant.runtime.TenantSqlConnection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Query
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * Behavioural tests for [TenantSqlConnection].
 */
class TenantSqlConnectionTest {

    @Test
    fun `a double quoted placeholder is rewritten to the tenant`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val query = mockk<Query<RowSet<Row>>>(relaxed = true)
        every { delegate.query(any()) } returns query
        val connection = TenantSqlConnection(delegate, TENANT_ID)

        // When
        connection.query("SELECT * FROM \"$PLACEHOLDER\".\"orders\"")

        // Then
        verify { delegate.query("SELECT * FROM \"$TENANT_ID\".\"orders\"") }
    }

    @Test
    fun `a backtick quoted placeholder is rewritten to the tenant`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val query = mockk<Query<RowSet<Row>>>(relaxed = true)
        every { delegate.query(any()) } returns query
        val connection = TenantSqlConnection(delegate, TENANT_ID)

        // When
        connection.query("SELECT * FROM `$PLACEHOLDER`.`orders`")

        // Then
        verify { delegate.query("SELECT * FROM `$TENANT_ID`.`orders`") }
    }

    @Test
    fun `every quoted placeholder in a statement is rewritten`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val query = mockk<Query<RowSet<Row>>>(relaxed = true)
        every { delegate.query(any()) } returns query
        val connection = TenantSqlConnection(delegate, TENANT_ID)

        // When
        connection.query(
            "SELECT * FROM \"$PLACEHOLDER\".\"orders\" JOIN \"$PLACEHOLDER\".\"customers\""
        )

        // Then
        verify {
            delegate.query(
                "SELECT * FROM \"$TENANT_ID\".\"orders\" JOIN \"$TENANT_ID\".\"customers\""
            )
        }
    }

    @Test
    fun `a prepared query is rewritten to the tenant`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val preparedQuery = mockk<PreparedQuery<RowSet<Row>>>(relaxed = true)
        every { delegate.preparedQuery(any()) } returns preparedQuery
        val connection = TenantSqlConnection(delegate, TENANT_ID)

        // When
        connection.preparedQuery("SELECT * FROM \"$PLACEHOLDER\".\"orders\"")

        // Then
        verify { delegate.preparedQuery("SELECT * FROM \"$TENANT_ID\".\"orders\"") }
    }

    @Test
    fun `a statement without a placeholder is passed through unchanged`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val query = mockk<Query<RowSet<Row>>>(relaxed = true)
        every { delegate.query(any()) } returns query
        val connection = TenantSqlConnection(delegate, TENANT_ID)
        val sql = "SELECT * FROM \"orders\""

        // When
        connection.query(sql)

        // Then
        verify { delegate.query(sql) }
    }

    @Test
    fun `a non-tenant statement without a placeholder is passed through unchanged`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val query = mockk<Query<RowSet<Row>>>(relaxed = true)
        every { delegate.query(any()) } returns query
        val connection = TenantSqlConnection(delegate, null)
        val sql = "SELECT * FROM \"orders\""

        // When
        connection.query(sql)

        // Then
        verify { delegate.query(sql) }
    }

    @Test
    fun `a tenant scoped statement without a tenant is rejected`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val connection = TenantSqlConnection(delegate, null)

        // When / Then
        assertThatThrownBy {
            connection.query("SELECT * FROM \"$PLACEHOLDER\".\"orders\"")
        }.isInstanceOf(IllegalStateException::class.java)
        verify(exactly = 0) { delegate.query(any()) }
    }

    @Test
    fun `an unquoted placeholder is rejected`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val connection = TenantSqlConnection(delegate, TENANT_ID)

        // When / Then
        assertThatThrownBy {
            connection.query("SELECT * FROM $PLACEHOLDER.orders")
        }.isInstanceOf(IllegalStateException::class.java)
        verify(exactly = 0) { delegate.query(any()) }
    }

    @Test
    fun `a placeholder inside a string literal is rejected`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val connection = TenantSqlConnection(delegate, TENANT_ID)

        // When / Then
        assertThatThrownBy {
            connection.query("SELECT '$PLACEHOLDER' FROM orders")
        }.isInstanceOf(IllegalStateException::class.java)
        verify(exactly = 0) { delegate.query(any()) }
    }

    @Test
    fun `a placeholder with mismatched quotes is rejected`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val connection = TenantSqlConnection(delegate, TENANT_ID)

        // When / Then
        assertThatThrownBy {
            connection.query("SELECT * FROM \"$PLACEHOLDER`.`orders`")
        }.isInstanceOf(IllegalStateException::class.java)
        verify(exactly = 0) { delegate.query(any()) }
    }

    @Test
    fun `the tenant identifier is used as supplied`() {
        // Given
        val delegate = mockk<SqlConnection>(relaxed = true)
        val query = mockk<Query<RowSet<Row>>>(relaxed = true)
        every { delegate.query(any()) } returns query
        val connection = TenantSqlConnection(delegate, "tenant_schema")

        // When
        connection.query("SELECT * FROM \"$PLACEHOLDER\".\"orders\"")

        // Then
        verify { delegate.query("SELECT * FROM \"tenant_schema\".\"orders\"") }
    }

    private companion object {
        private val PLACEHOLDER = TenantSqlConnection.TENANT_SCHEMA_PLACEHOLDER
        private const val TENANT_ID = "550e8400-e29b-41d4-a716-446655440000"
    }
}
