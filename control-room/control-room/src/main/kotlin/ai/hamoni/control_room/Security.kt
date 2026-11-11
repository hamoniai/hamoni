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

import ai.hamoni.common.core.exception.InvalidParameterException
import ai.hamoni.common.core.util.cleanUrl
import ai.hamoni.control_room.service.AuthenticationService
import ai.hamoni.quarkus.ext.quarkus.multi_tenant.Config as MultiTenantConfig
import io.quarkus.arc.Unremovable
import io.quarkus.hibernate.orm.PersistenceUnitExtension
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver
import io.quarkus.oidc.JavaScriptRequestChecker
import io.quarkus.oidc.OidcRequestContext
import io.quarkus.oidc.OidcTenantConfig
import io.quarkus.oidc.TenantConfigResolver
import io.quarkus.vertx.http.runtime.security.ChallengeData
import io.smallrye.config.SmallRyeConfig
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.infrastructure.Infrastructure
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.ConfigProvider
import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import java.util.*


/*
 * Security related components/configuration.
 * If there are overrides for these classes, check the `ModeOverrides.kt`.
 */

// DO NOT REMOVE THE FOLLOWING COMMENT SECTION
// This is the real implementation for the TenantResolver when Quarkus fix the multi-tenant setup in Quarkus Panache
// Reactive. Until then, leave this here as a reference.
///**
// * This the organisation to tenant mapping which creates data isolation.
// * Warning: this also gets called on non-HTTP requests, like messaging, timers etc. We will need to extend this
// * to support those situations where the RoutingContext is null.
// *
// * The tenant must be the hyphen separated form, else the schema check will fail To ensure this, we parse, then convert back to a string.
// */
//@ApplicationScoped
//@Priority(Interceptor.Priority.PLATFORM_BEFORE)
//class OrganisationTenantResolver : TenantResolver {
//    companion object {
//        // ContainerRequestContext contains a relative path from the configured root, so need to strip the /api.
//        val AUTH_PATH_WITHOUT_PREFIX = AUTH_PATH.substring(API_PATH.length - 1)
//
//    }
//    override fun name() = "header-custom"
//
//    override fun resolve(context: TenantResolutionContext): TenantResolution {
//        return context.get(ContainerRequestContext::class.java)
//            .map { req ->
//                // Check the URL. return TenantResolution.notApplicable if not found.
//                if (req.uriInfo.path.orEmpty().cleanUrl().startsWith(AUTH_PATH_WITHOUT_PREFIX)) {
//                    TenantResolution.notApplicable()
//                } else {
//                    req.getHeaderString(REQUEST_ORGANISATION_ID).orEmpty().trim().let {
//                        try {
//                            TenantResolution.resolved(Uuid.parse(it).toString())
//                        } catch (_: IllegalArgumentException) {
//                            TenantResolution.rejected("missing or invalid header")
//                        }
//                    }
//                }
//            }.orElseGet {
//                TODO("missing context, need to see what cases.")
//            }
//    }
//}

@PersistenceUnitExtension
@Unremovable
@RequestScoped
class OrganisationTenantResolver : TenantResolver {
    override fun getDefaultTenantId(): String {
        return "x"
    }

    override fun resolveTenantId(): String {
        return "y"
    }
}

/**
 * A Hibernate naming strategy to add the configured tenant placeholder into unqualified table names
 * and other objects. The placeholder is emitted as a quoted identifier so the connection layer can
 * replace it without needing to know the database's quote character.
 */
class PhysicalNamingStrategyTenantQualifyingSnakeCaseImpl : PhysicalNamingStrategySnakeCaseImpl() {
    private val tenantPlaceholder: String =
        ConfigProvider.getConfig()
            .unwrap(SmallRyeConfig::class.java)
            .getConfigMapping(MultiTenantConfig::class.java)
            .placeholder()

    override fun toPhysicalSchemaName(
        logicalName: Identifier?,
        jdbcEnvironment: JdbcEnvironment
    ): Identifier? =
        if (null == logicalName) {
            Identifier(tenantPlaceholder, true, true)
        } else {
            super.toPhysicalSchemaName(logicalName, jdbcEnvironment)
        }
}

/**
 * Resolve the OIDC tenant (configuration) for a request. This is not really the tenant, it is the OIDC
 * configuration that is used for the request.
 *
 * An explicit provider wins so that a person can choose another provider (or
 * switch provider) while already signed in. Once the authorization code flow is
 * under way, the tenant identifier recorded in Quarkus' state or session cookie
 * keeps the flow on the same provider. A business call carrying an organisation
 * may later select that organisation's provider. A null configuration means the
 * default tenant, which is the Keycloak Dev Services tenant in development.
 */
@ApplicationScoped
class OidcTenantConfigResolver @Inject constructor(
    private val authenticationService: AuthenticationService
) : TenantConfigResolver {

    companion object {
        /**
         * The query parameter used by the frontend login page to select a provider.
         */
        const val PROVIDER_QUERY_PARAMETER = "provider"
    }

    /**
     * This is called on every request, authenticated and unauthenticated. If authenticated, getting the
     * `routingContext.get()` call will return a value identifying the context. This function is controlled by
     * the 'quarkus.oidc.tenant-enabled' property. Returning null from this function does:
     * check for cache config (by 'routingContext.get("tenant-id")') -> Use default -> fail if auth/allow if non-auth.
     *
     * The 'quarkus.oidc.tenant-enabled=false' will skip the default. In dev mode, the default will always use
     * Keycloak. In other environments this must not be set.
     *
     * There are return types/actions depending on the request and expected output.
     *
     * ** Login page case **
     * Must pull the provider id from the request parameter. If not found, then abort with an exception.
     *
     * ** Login callback **
     * Should always return null and use the cached result.
     *
     * ** API request **
     * Should already be authenticated, so return null and use the cached, if not then auth will fail.
     *
     * ** Anything else **
     * Return null, if not protected, it will pass, if protected it will fail.
     *
     * Collapsing these rules, only the login page has special handling, all other cases return null
     */
    override fun resolve(
        routingContext: RoutingContext,
        requestContext: OidcRequestContext<OidcTenantConfig>
    ): Uni<OidcTenantConfig> {
        val path = routingContext.request().path().orEmpty().cleanUrl()

        return if (path == LOGIN_PATH) {
            Uni.createFrom()
                .item {
                    val provider = routingContext.request().getParam(PROVIDER_QUERY_PARAMETER)
                    if (null == provider || provider.isBlank()) {
                        throw InvalidParameterException(PROVIDER_QUERY_PARAMETER, "not specified")
                    } else {
                        try {
                            UUID.fromString(provider)
                        } catch (e: IllegalArgumentException) {
                            throw InvalidParameterException(PROVIDER_QUERY_PARAMETER, "invalid format", e)
                        }
                    }
                }
                .onItem().transformToUni {
                    // Need to map the provider id to an organisation, then get the full config for that organisation,
                    // will need to switch context/establish a context here, then use a utility library to look up the
                    // URL, get the well known configuration and then convert it over.
                    authenticationService.getIdentityProviderById(it)
                }
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .onItem().transform { provider ->
                    // This is the conversion to a OidcTenantConfig, which includes all the endpoint resolution.
                    // This will invoke a web request to identify the endpoint.
                    // TODO: create an object cache for OidcTenantConfig, it will be used frequently and not change often.
                    // In the event of a cache hit, bypass the whole lookup/transformation route.
                    OidcTenantConfig.builder()
                        .tenantId(provider.id.toString())
                        .authServerUrl(provider.url)
                        .clientId(provider.clientId)
                        .credentials(provider.clientSecretReference) // FIXME: should reference the credential vault.
                        .applicationType(io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.HYBRID)
                        .discoveryEnabled(true)
                        .build()
                }
                .onItem().transform { v ->
                    v
                }
                .emitOn(Infrastructure.getDefaultWorkerPool())
        } else {
            Uni.createFrom().nullItem()
        }
    }
}

/**
 * Turn unauthenticated REST challenges into a 401 response.
 *
 * Quarkus' OIDC code flow normally redirects an unauthenticated browser request to
 * the provider. That is correct for the login entry point but wrong for a REST
 * call, which must report 401 so the client can distinguish "not signed in" from
 * a redirect. Quarkus exposes [JavaScriptRequestChecker] as the extension point for
 * choosing the challenge response; it is used here to classify API calls rather
 * than to detect JavaScript.
 */
@ApplicationScoped
class RestAuthenticationChallenge : JavaScriptRequestChecker {
    /**
     * Classify business and screen-flow calls as requiring a 401 challenge. The
     * login entry point is excluded because it must start the authorization flow.
     * We are only protecting under the API route to ensure that other resources don't get locked up
     * incuding the frontend download. In production, everything from the backend should really be a service.
     * We will optimise the check to assume the call is to the API.
     */
    override fun isJavaScriptRequest(context: RoutingContext): Boolean =
        context.request().path().orEmpty().cleanUrl().let {
            it.startsWith(API_PATH) && it != LOGIN_PATH
        }

    /**
     * Return an unauthorized challenge without a redirect.
     * FIXME: to be technically correct, if the token is missing there should be no error code. If the token exists,
     * but is invalid, it should have 'error="invalid_token"'. This is further complicated by the fact that we are
     * using the security token, not the bearer to authenticate.
     */
    override fun getChallenge(context: RoutingContext): ChallengeData =
        ChallengeData(401, "WWW-Authenticate", "Bearer")
}
