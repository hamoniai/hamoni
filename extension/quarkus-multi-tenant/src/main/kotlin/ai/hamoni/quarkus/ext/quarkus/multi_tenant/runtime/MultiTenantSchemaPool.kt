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

@file:Suppress("unused")

package ai.hamoni.quarkus.ext.quarkus.multi_tenant.runtime

import io.vertx.core.AsyncResult
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PrepareOptions
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Query
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection
import java.util.function.Function

/**
 * This is a simple delegate handler for the standard pool that binds every
 * unqualified tenant table to the tenant's schema.
 *
 * The pool does not change the session schema. It hands out [TenantSqlConnection]
 * instances and rewrites SQL that carries [TenantSqlConnection.TENANT_SCHEMA_PLACEHOLDER],
 * so a null tenant means genuinely non-tenant work and a pooled connection never
 * carries a previous tenant's schema.
 *
 * @param delegate the Vert.x pool the operations are delegated to
 * @param tenantId the tenant whose schema this pool is bound to, or null for non-tenant work
 */
open class MultiTenantSchemaPool(
    private val delegate: Pool,
    private val tenantId: String?
): Pool {
    override fun close(): Future<Void?>? =
        delegate.close()

    override fun close(handler: Handler<AsyncResult<Void?>?>?) =
        delegate.close(handler)

    @Deprecated("Deprecated in Java")
    override fun connectHandler(handler: Handler<SqlConnection?>?): Pool? =
        throw UnsupportedOperationException("This operation is not supported")

    @Deprecated("Deprecated in Java")
    override fun connectionProvider(provider: Function<Context?, Future<SqlConnection?>?>?): Pool? =
        throw UnsupportedOperationException("This operation is not supported")

    /**
     * Acquire a connection whose SQL is bound to this pool's tenant.
     */
    override fun getConnection(handler: Handler<AsyncResult<SqlConnection>>?) {
        if (null != handler) {
            getConnection().onComplete(handler)
        }
    }

    /**
     * Acquire a connection whose SQL is bound to this pool's tenant.
     */
    override fun getConnection(): Future<SqlConnection> =
        delegate.connection.map { connection -> TenantSqlConnection(connection, tenantId) }

    /**
     * Prepare a simple query with this pool's tenant schema applied.
     */
    override fun preparedQuery(sql: String): PreparedQuery<RowSet<Row>> =
        delegate.preparedQuery(TenantSqlConnection.rewrite(sql, tenantId))

    /**
     * Prepare a simple query with this pool's tenant schema applied.
     */
    override fun preparedQuery(sql: String, options: PrepareOptions): PreparedQuery<RowSet<Row>> =
        delegate.preparedQuery(TenantSqlConnection.rewrite(sql, tenantId), options)

    /**
     * Run a simple query with this pool's tenant schema applied.
     */
    override fun query(sql: String): Query<RowSet<Row>> =
        delegate.query(TenantSqlConnection.rewrite(sql, tenantId))

    override fun size(): Int =
        delegate.size()
}