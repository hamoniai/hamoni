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

package ai.hamoni.control_room.initialisers

import ai.hamoni.common.platform.PlatformConfig
import ai.hamoni.control_room.ResourceLogger
import io.quarkus.runtime.StartupEvent
import io.vertx.mutiny.core.Vertx
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor

/*
 * Default initialisers for the organisation
 */

/**
 * Perform the organisation initialisation last. This checks to see if the organisation has been created and
 * initialised. If there is a config file supplied, it will initialise, if not, it will throw an error
 */
@ApplicationScoped
class OrganisationInitialiser @Inject constructor (
    private val platformConfig: PlatformConfig,
    private val vertx: Vertx
) {

    /**
     * Bind to the Quarkus startup.
     */
    @Suppress("unused")
    fun onStart(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) @Suppress("UNUSED_PARAMETER") event: StartupEvent) {
        val defaultOrganisationId = platformConfig.defaultOrganisation().id()
        ResourceLogger.LOGGER.showDefaultOrganisationId(defaultOrganisationId)

        // Check and see if the organisation has been configured. We do this by checking the initialised flag.
        // TODO: if we have been told to initialise and the organisation has already been initialised, skip and warn.
        // If a config file specified, but cannot be found, it's a fatal error.
        // Need to verify the schema after loading the JSON, if it doesn't validate, then it is a fatal error.
        //vertx.fileSystem().readFile("")
    }
}