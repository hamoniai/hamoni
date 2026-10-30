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
import ai.hamoni.common.core.REQUEST_ORGANISATION_ID
import ai.hamoni.control_room.service.ConfigurationService
import ai.hamoni.control_room.service.OidcProviderConfigurationService
import io.quarkus.hibernate.orm.PersistenceUnitExtension
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver
import io.quarkus.oidc.JavaScriptRequestChecker
import io.quarkus.oidc.OidcRequestContext
import io.quarkus.oidc.OidcTenantConfig
import io.quarkus.oidc.TenantConfigResolver
import io.quarkus.vertx.http.runtime.security.ChallengeData
import io.smallrye.mutiny.Uni
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import java.util.UUID


/*
 * Security related components/configuration.
 * If there are overrides for these classes, check the `ModeOverrides.kt`.
 */

/**
 * This the organisation to tenant mapping which creates data isolation.
 * Warning: this also gets called on non-HTTP requests, like messaging, timers etc. We will need to extend this
 * to support those situations where the RoutingContext is null.
 */
@PersistenceUnitExtension
@RequestScoped
class DataTenantResolver @Inject constructor(
    private val context: RoutingContext?,
    private val configurationService: ConfigurationService
): TenantResolver {

    /**
     * This is a nasty bridge, but it should be okay. The `getDefaultOrganisationId` should really resolve from
     * properties and so should never block, and always be available.
     */
    override fun getDefaultTenantId(): String =
        configurationService.getDefaultOrganisationId().await().indefinitely().toString()

    /**
     * See above notes about when called without a routingContext
     */
    override fun resolveTenantId(): String =
        if (null != context) {
            context.request()?.getHeader(REQUEST_ORGANISATION_ID) ?: defaultTenantId
        } else {
            TODO("Unhandled situation in hibernate configuration")
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
    private val providerConfigurationService: OidcProviderConfigurationService
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
                .onItem().transformToUni { providerConfigurationService.getById(it) }
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
