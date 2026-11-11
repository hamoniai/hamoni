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

import ai.hamoni.quarkus.ext.quarkus.multi_tenant.runtime.MultiTenantSchemaPool
import ai.hamoni.quarkus.ext.quarkus.multi_tenant.runtime.TenantSqlConnection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Query
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * Behavioural tests for [MultiTenantSchemaPool].
 */
class MultiTenantSchemaPoolTest {

    @Test
    fun `getConnection returns a connection bound to the tenant`() {
        // Given
        val delegate = mockk<Pool>(relaxed = true)
        val connection = mockk<SqlConnection>(relaxed = true)
        every { delegate.getConnection() } returns Future.succeededFuture(connection)
        val pool = MultiTenantSchemaPool(delegate, TENANT_ID)

        // When
        val acquired = pool.getConnection().result()

        // Then
        assertThat(acquired).isInstanceOf(TenantSqlConnection::class.java)
    }

    @Test
    fun `the handler form returns a connection bound to the tenant`() {
        // Given
        val delegate = mockk<Pool>(relaxed = true)
        val connection = mockk<SqlConnection>(relaxed = true)
        every { delegate.getConnection() } returns Future.succeededFuture(connection)
        val pool = MultiTenantSchemaPool(delegate, TENANT_ID)
        var result: AsyncResult<SqlConnection>? = null

        // When
        pool.getConnection { acquired -> result = acquired }

        // Then
        assertThat(result?.result()).isInstanceOf(TenantSqlConnection::class.java)
    }

    @Test
    fun `query rewrites the tenant placeholder`() {
        // Given
        val delegate = mockk<Pool>(relaxed = true)
        val query = mockk<Query<RowSet<Row>>>(relaxed = true)
        every { delegate.query(any()) } returns query
        val pool = MultiTenantSchemaPool(delegate, TENANT_ID)

        // When
        pool.query("SELECT * FROM \"$PLACEHOLDER\".\"orders\"")

        // Then
        verify { delegate.query("SELECT * FROM \"$TENANT_ID\".\"orders\"") }
    }

    @Test
    fun `preparedQuery rewrites the tenant placeholder`() {
        // Given
        val delegate = mockk<Pool>(relaxed = true)
        val preparedQuery = mockk<PreparedQuery<RowSet<Row>>>(relaxed = true)
        every { delegate.preparedQuery(any()) } returns preparedQuery
        val pool = MultiTenantSchemaPool(delegate, TENANT_ID)

        // When
        pool.preparedQuery("SELECT * FROM \"$PLACEHOLDER\".\"orders\"")

        // Then
        verify { delegate.preparedQuery("SELECT * FROM \"$TENANT_ID\".\"orders\"") }
    }

    @Test
    fun `a non-tenant pool rejects a tenant scoped statement`() {
        // Given
        val delegate = mockk<Pool>(relaxed = true)
        val pool = MultiTenantSchemaPool(delegate, null)

        // When / Then
        assertThatThrownBy {
            pool.query("SELECT * FROM \"$PLACEHOLDER\".\"orders\"")
        }.isInstanceOf(IllegalStateException::class.java)
        verify(exactly = 0) { delegate.query(any()) }
    }

    private companion object {
        private val PLACEHOLDER = TenantSqlConnection.TENANT_SCHEMA_PLACEHOLDER
        private const val TENANT_ID = "550e8400-e29b-41d4-a716-446655440000"
    }
}
