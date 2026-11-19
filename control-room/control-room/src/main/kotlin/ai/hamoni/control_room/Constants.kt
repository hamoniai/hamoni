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

/*
 * Control room specific constants
 */
const val API_PATH = "/api/"
const val AUTH_PATH = "${API_PATH}auth/"
const val LOGIN_PATH = "${AUTH_PATH}login"
const val IDENTITY_PROVIDERS_PATH = "${AUTH_PATH}identityProviders"
const val LANDING_PATH = "/"


/*
 * Standard role definitions. Most of the roles or permissions are dynamically evaluated based on object holding.
 * However, there are a few standard roles that override those access permissions
 */
/**
 * System role definition. This is for anything acting in the context of the system, and not a user.
 */
const val ROLE_SYSTEM = "system"

/**
 * allows configuration of the environments, runners etc.
 */
const val ROLE_PLATFORM_ADMIN = "platform_admin"

/**
 * allows configuration of the organisation (as a member of that organisation). It does not allow across organisation
 * access.
 */
const val ROLE_ORGANISATION_ADMIN = "platform_admin"

/**
 * The name of the system identity
 */
const val IDENTITY_SYSTEM = "system"

