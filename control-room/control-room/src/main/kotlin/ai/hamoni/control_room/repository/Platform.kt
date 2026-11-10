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

package ai.hamoni.control_room.repository

import ai.hamoni.control_room.data.AuthenticationProfile
import ai.hamoni.control_room.data.IdentityProviderIndex
import ai.hamoni.control_room.data.OrganisationIdentityProvider
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheQuery
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/*
 * Control room specific repositories in the platform schema.
 */

/**
 * Repository for authentication profiles.
 */
@ApplicationScoped
class AuthenticationProfileRepository : PanacheRepositoryBase<AuthenticationProfile, UUID>

/**
 * Repository for organisation identity provider index.
 */
@ApplicationScoped
class IdentityProviderIndexRepository : PanacheRepositoryBase<IdentityProviderIndex, UUID>