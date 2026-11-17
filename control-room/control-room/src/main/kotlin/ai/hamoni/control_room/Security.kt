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

import ai.hamoni.common.core.PLATFORM_SCHEMA
import ai.hamoni.common.core.REQUEST_ORGANISATION_ID
import ai.hamoni.common.core.exception.InvalidParameterException
import ai.hamoni.common.core.util.cleanUrl
import ai.hamoni.control_room.service.AuthenticationService
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
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import org.eclipse.microprofile.config.ConfigProvider
import org.hibernate.boot.model.naming.Identifier
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment
import java.util.UUID
import ai.hamoni.quarkus.ext.quarkus.multi_tenant.Config as MultiTenantConfig

/*
 * Security related components/configuration.
 * If there are overrides for these classes, check the `ModeOverrides.kt`.
 */

/**
 * A bean to centralise all authorisation checks All checks are reactive since there might be database or cache
 * access.
 *
 * This can be application scoped because all the methods are runtime evaluated in the erquest scope.
 *
 * A session is always required to be active for this.
 */
@ApplicationScoped
class PlatformAuthoriser
@Inject
constructor(
    @param:PersistenceUnitExtension private val tenantResolver: TenantResolver,
) {
    /**
     * Check the caller has a specified role.
     */
    fun checkRoles(vararg roles: String): Uni<Unit> {
        // this is for the future and will replace all the methods below, although we might have some shortcuts.
        TODO()
    }

    /**
     * Check the caller has a specified role, or is a system account.
     */
    fun checkSystemOrRoles(vararg roles: String): Uni<Unit> {
        // this is for the future and will replace all the methods below, although we might have some shortcuts.
        TODO()
    }

    /**
     * Check an authenticated identity has access to an organisation. This is the standard platform wide check
     * that must be invoked before accessing an organisation.
     *
     * @return An empty Uni on success, or a failed Uni on error
     */
    fun checkOrganisationAccess(): Uni<Unit> = Uni.createFrom().item(Unit)

    /**
     * Check if the caller has the platform admin role, or if it is a system security context.
     *
     * @return An empty Uni on success, or a failed Uni on error
     */
    fun checkPlatformAdminOrSystem(): Uni<Unit> = Uni.createFrom().item(Unit)

    /**
     * Check the caller may administer an organisation.
     *
     * The organisation admin permission includes the organisation owner and any
     * person delegated that access. A platform admin or system security context is
     * also allowed.
     *
     * Delegation is not modelled yet; this check is currently a pass-through like
     * the other authorisation checks.
     */
    fun checkOrganisationAdminOrPlatformAdminOrSystem(): Uni<Unit> = Uni.createFrom().item(Unit)
}

/**
 * In the usual case, we want to have tenant resolver that directs queries based on the organisation header.
 *
 * However, in certain circumstances, the routingContext will not be available, e.g. startup jobs. Instead of allowing
 * an override, we are allowing a fallback. The value from the RoutingContext should always be taken in preference,
 * and if it is available, the producer method ensures that it is fixed for the request scope so that anything
 * unaccounted for cannot change the tenant. This also allows the TenantResolver to be injected without performance
 * impact.
 *
 * If the organisation header is not present or not correct, an exception will be thrown.
 */
@RequestScoped
@Unremovable
class OrganisationTenantResolverProducer
@Inject
constructor(
    private val routingContext: RoutingContext,
) {
    /**
     * Use this to produce a fallback in the request scope. It will only work before the TenantResolver has been
     * produced, and if the RoutingContext does not exist. Once the tenant override has been produced, it will not be
     * overridable since the bean already exists.
     */
    var tenantFallback: String? = null

    @PersistenceUnitExtension
    @Produces
    @RequestScoped
    @Unremovable
    fun get(): TenantResolver? =
        // Cannot use Instance.isResolvable because it only checks the bean's existence. Therefore, just have to
        // try and access the request data.
        runCatching { routingContext.request() }.fold(
            { request ->
                if (request.path().orEmpty().cleanUrl().startsWith(AUTH_PATH)) {
                    // Organisation agnostic, but we can not set the tenant to null, the
                    // HibernateCurrentTenantIdentifierResolver will throw an error. Instead, since all of these requests will
                    // route to the platform schema anyway, just set the platform schema
                    PLATFORM_SCHEMA
                } else {
                    // Everything else requires an organisation. If we cannot resolve one, and it is valid then throw an error.
                    // Do not let unvalidated organisation identifiers pass here.
                    try {
                        UUID.fromString(
                            request.getHeader(REQUEST_ORGANISATION_ID)
                                .orEmpty()
                                .trim()
                        ).toString()
                    } catch (_: IllegalArgumentException) {
                        throw InvalidParameterException(REQUEST_ORGANISATION_ID, "Invalid organisation identifier")
                    }
                }
            },
            { tenantFallback } // If there is no fallback, this will end up being unresolved.
        )?.let {
            object : TenantResolver {
                override fun getDefaultTenantId(): String = it

                override fun resolveTenantId(): String = it

                override fun isRoot(tenantId: String?): Boolean = PLATFORM_SCHEMA == tenantId
            }
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
        jdbcEnvironment: JdbcEnvironment,
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
class OidcTenantConfigResolver
@Inject
constructor(
    private val authenticationService: AuthenticationService,
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
        requestContext: OidcRequestContext<OidcTenantConfig>,
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
                    // TODO: create an object cache for OidcTenantConfig, it is used frequently and not change often
                    // In the event of a cache hit, bypass the whole lookup/transformation route.
                    OidcTenantConfig.builder()
                        .tenantId(provider.id.toString())
                        .authServerUrl(provider.url)
                        .clientId(provider.clientId)
                        .credentials(provider.clientSecret) // FIXME: cred-vault ref.
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
    override fun getChallenge(context: RoutingContext): ChallengeData = ChallengeData(401, "WWW-Authenticate", "Bearer")
}
