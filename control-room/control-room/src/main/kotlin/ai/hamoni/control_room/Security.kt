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

package ai.hamoni.control_room

import ai.hamoni.common.platform.REQUEST_ORGANISATION_ID
import io.quarkus.oidc.OidcRequestContext
import io.quarkus.oidc.OidcTenantConfig
import io.quarkus.oidc.TenantConfigResolver
import io.smallrye.mutiny.Uni
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped

/*
 * Security related components/configuration.
 * If there are overrides for these classes, check the `ModeOverrides.kt`.
 */

/**
 * Resolve the OIDC tenant. This is used
 */
@ApplicationScoped
class DynamicOidcResolver : TenantConfigResolver {

//    @Override
//    public Uni<OidcTenantConfig> resolve(RoutingContext context) {
//        String tenantId = context.request().getHeader("X-Tenant-Id");
//
//        if (tenantId == null) {
//            return Uni.createFrom().nullItem(); // Fallback to default configuration
//        }
//
//        // Fetch OIDC credentials dynamically (e.g., from a DB or cache)
//        return Uni.createFrom().item(() -> {
//        OidcTenantConfig config = new OidcTenantConfig();
//        config.setTenantId(tenantId);
//        config.setAuthServerUrl("https://identity-provider.com" + tenantId);
//        config.setClientId("dynamic-client-id");
//
//        OidcTenantConfig.Credentials credentials = new OidcTenantConfig.Credentials();
//        credentials.setSecret("dynamic-client-secret");
//        config.setCredentials(credentials);
//
//        return config;
//    });
//    }

    override fun resolve(
        routingContext: RoutingContext,
        requestContext: OidcRequestContext<OidcTenantConfig>
    ): Uni<OidcTenantConfig> {
        val tenantId = routingContext.request().getHeader(REQUEST_ORGANISATION_ID)
        TODO("Not yet implemented")
    }
}
