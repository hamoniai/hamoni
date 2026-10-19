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

package ai.hamoni.control_room.service

import ai.hamoni.control_room.data.AuthenticationProfile
import io.quarkus.runtime.annotations.RegisterForReflection
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * All frontend specific services go in here. Services that common to the frontend and backend should go into one of
 * the common projects.
 *
 * One service will cover all hydrate options. The caller can slice the pbject as required.
 */


/**
 * Service for authentication profile logic
 */
@ApplicationScoped
@RegisterForReflection
class AuthenticationProfileService @Inject constructor() {

    /**
     * Get the current logged-in authentication profile.
     */
    fun profile(hydrate: List<AuthenticationProfileServiceHydration>): Uni<AuthenticationProfile> {
        // Check the current principle id, and get the profile associated.
        TODO()
    }

    /**
     * Logout the current authentication profile.
     */
    fun logout(): Uni<Void> {
        // Destroy the principle and all authorisation cache objects for this authentication profile.
        TODO()
    }
}

@Serializable
enum class AuthenticationProfileServiceHydration {
    @SerialName("organisations")
    ORGANISATIONS
}

