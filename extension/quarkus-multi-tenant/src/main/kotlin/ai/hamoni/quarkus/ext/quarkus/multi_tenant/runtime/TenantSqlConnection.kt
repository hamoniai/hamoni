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

package ai.hamoni.quarkus.ext.quarkus.multi_tenant.runtime

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.sqlclient.PrepareOptions
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.PreparedStatement
import io.vertx.sqlclient.Query
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection

/**
 * A [SqlConnection] that binds unqualified tenant tables to the tenant's schema.
 *
 * Hibernate emits the quoted [TENANT_SCHEMA_PLACEHOLDER] schema for every unqualified
 * organisation table. This wrapper replaces that placeholder with the tenant identifier
 * before the SQL reaches the connection, so the connection's session schema is never
 * changed.
 *
 * This is a deliberate deviation from setting a session schema on the connection: a
 * session schema cannot be unset for housekeeping and other non-tenant work, and a
 * prepared statement is bound to the database that was current when it was prepared.
 * Rewriting the SQL keeps non-tenant connections in the database's normal default schema
 * and makes prepared statements safe across tenants.
 *
 * The rewrite is fail-closed. A placeholder that is not a quoted identifier, or that is
 * present without a tenant, fails the statement rather than executing against the wrong
 * schema.
 *
 * @param delegate the physical connection the SQL is delegated to
 * @param tenantId the tenant whose schema is bound to statements, or null for non-tenant work
 */
class TenantSqlConnection(
    private val delegate: SqlConnection,
    private val tenantId: String?,
) : SqlConnection by delegate {

    companion object {
        /**
         * The default quoted schema placeholder Hibernate must emit for unqualified tenant tables.
         */
        const val DEFAULT_TENANT_SCHEMA_PLACEHOLDER: String = "<tenant>"

        /**
         * The quoted schema placeholder Hibernate must emit for unqualified tenant tables.
         *
         * It is set from build-time configuration by the extension recorder and must stay in
         * step with the Hibernate naming configuration.
         */
        var TENANT_SCHEMA_PLACEHOLDER: String = DEFAULT_TENANT_SCHEMA_PLACEHOLDER

        /**
         * Replace the quoted placeholder with the tenant identifier.
         *
         * A placeholder in a string literal, in an unquoted identifier, or in a statement
         * without a tenant is rejected rather than executed against the wrong schema. The
         * quote character is the one Hibernate used for the first placeholder and must be
         * consistent for every occurrence.
         *
         * @param sql the SQL Hibernate generated
         * @param tenantId the tenant whose schema is bound to the statement, or null
         * @return the SQL with the placeholder replaced
         */
        internal fun rewrite(sql: String, tenantId: String?): String {
            val first = sql.indexOf(TENANT_SCHEMA_PLACEHOLDER)
            if (first < 0) {
                return sql
            }
            check(tenantId != null) { "A tenant-scoped statement was executed with no tenant identifier" }
            check(first > 0) { "First placeholder is unquoted" }

            val quoteChar = sql[first - 1]
            val closingQuote = when (quoteChar) {
                '"' -> '"' // H2, Postgres
                '`' -> '`' // MariaDB, MySQL
                '[' -> ']' // SQL Server
                else -> throw IllegalStateException("Invalid quote character")
            }
            // Over-allocate: the buffer is cheaper than a resize, and a query has few tables.
            val replacedSql = StringBuilder(sql.length + 3 * tenantId.length)

            var pos = 0
            var index = first
            while (index >= 0) {
                val end = index + TENANT_SCHEMA_PLACEHOLDER.length
                check(
                    index > 0 && end < sql.length &&
                        sql[index - 1] == quoteChar && sql[end] == closingQuote
                ) { "Found incorrectly quoted tenant identifier" }

                replacedSql.append(sql, pos, index).append(tenantId)
                pos = end
                index = sql.indexOf(TENANT_SCHEMA_PLACEHOLDER, pos)
            }
            replacedSql.append(sql, pos, sql.length)
            return replacedSql.toString()
        }
    }

    /**
     * Keep fluent chaining on the wrapper rather than the delegate.
     */
    override fun closeHandler(handler: Handler<Void>?): SqlConnection {
        delegate.closeHandler(handler)
        return this
    }

    /**
     * Keep fluent chaining on the wrapper rather than the delegate.
     */
    override fun exceptionHandler(handler: Handler<Throwable>?): SqlConnection {
        delegate.exceptionHandler(handler)
        return this
    }

    /**
     * Prepare a statement with the tenant schema applied.
     */
    override fun prepare(sql: String): Future<PreparedStatement> =
        delegate.prepare(rewrite(sql, tenantId))

    /**
     * Prepare a statement with the tenant schema applied.
     */
    override fun prepare(sql: String, options: PrepareOptions): Future<PreparedStatement> =
        delegate.prepare(rewrite(sql, tenantId), options)

    /**
     * Prepare a statement with the tenant schema applied, returning this wrapper for chaining.
     */
    override fun prepare(
        sql: String,
        handler: Handler<AsyncResult<PreparedStatement>>?
    ): SqlConnection {
        delegate.prepare(rewrite(sql, tenantId), handler)
        return this
    }

    /**
     * Prepare a statement with the tenant schema applied, returning this wrapper for chaining.
     */
    override fun prepare(
        sql: String,
        options: PrepareOptions,
        handler: Handler<AsyncResult<PreparedStatement>>?
    ): SqlConnection {
        delegate.prepare(rewrite(sql, tenantId), options, handler)
        return this
    }

    /**
     * Prepare a simple query with the tenant schema applied.
     */
    override fun preparedQuery(sql: String): PreparedQuery<RowSet<Row>> =
        delegate.preparedQuery(rewrite(sql, tenantId))

    /**
     * Prepare a simple query with the tenant schema applied.
     */
    override fun preparedQuery(sql: String, options: PrepareOptions): PreparedQuery<RowSet<Row>> =
        delegate.preparedQuery(rewrite(sql, tenantId), options)

    /**
     * Run a simple query with the tenant schema applied.
     */
    override fun query(sql: String): Query<RowSet<Row>> =
        delegate.query(rewrite(sql, tenantId))
}
