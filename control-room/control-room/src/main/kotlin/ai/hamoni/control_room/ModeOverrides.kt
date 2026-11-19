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
package ai.hamoni.control_room

import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.oidc.OidcRequestContext
import io.quarkus.oidc.OidcTenantConfig
import io.quarkus.oidc.TenantConfigResolver
import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.Uni
import io.vertx.ext.web.RoutingContext
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Alternative
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.h2.value.DataType
import org.h2.value.TypeInfo
import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import java.sql.Types

/*
 * Mode specific overrides
 */

/**
 * Fixups for the H2 drive JSON incompatibility.
 */
@IfBuildProfile(anyOf = ["dev", "test"])
@ApplicationScoped
class AppInitializer {
    fun onStart(
        @Observes event: StartupEvent,
    ) {
        // Need to configure the H2 driver. See the root test support package for reasoning. This does not
        // exist in production, which uses MySQL.
        val dataType = DataType.getDataType(TypeInfo.TYPE_JSON.valueType)
        dataType.sqlType = Types.VARBINARY
    }
}

/**
 * Use Keycloak for all requests in dev mode. This approach exercises the parent class, and performs all the
 * validation, but then diverts all traffic to the local Keycloak.
 */
@Alternative
@Singleton
@IfBuildProfile(anyOf = ["dev", "test"])
@Priority(Int.MAX_VALUE)
class DevOidcTenantConfigResolver @Inject constructor(
    securityHelpers: SecurityHelpers
) : OidcTenantConfigResolver(securityHelpers) {

    /**
     * Call parent then divert, unless failure.
     */
    override fun resolve(
        routingContext: RoutingContext,
        requestContext: OidcRequestContext<OidcTenantConfig>
    ): Uni<OidcTenantConfig?> =
        super.resolve(routingContext, requestContext)
            .onItem().transform { null }
}

/**
 * A Hibernate naming strategy remove any schema names when exporting. This class cannot be excluded, but is not
 * used except in schema generation.
 */
@IfBuildProfile("cmd")
class PhysicalNamingSchemaErasingSnakeCaseImpl : PhysicalNamingStrategySnakeCaseImpl() {
    override fun toPhysicalCatalogName(
        logicalName: Identifier?,
        jdbcEnvironment: JdbcEnvironment?,
    ): Identifier? = null

    override fun toPhysicalSchemaName(
        logicalName: Identifier?,
        jdbcEnvironment: JdbcEnvironment,
    ): Identifier? = null
}
