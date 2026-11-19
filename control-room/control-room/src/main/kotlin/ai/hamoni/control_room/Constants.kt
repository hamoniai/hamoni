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
 * Identifier for the system credential
 */
const val IDENTITY_SYSTEM = "system"

/**
 * Obtained by being a member of an organisation. This provides general access to an organisation. It is obtained
 * by having an active "user profile" in the organisation. This is a precursor role, so if the identity does not
 * have it role, it cannot have any higher roles in the organisation.
 *
 * This role is evaluated for each call, so the effect will be immediate and does not require logout.
 *
 * Allows configuration of the organisation (as a member of that organisation).
 */
const val ROLE_ORGANISATION = "organisation"

/**
 * Obtained by being the organisation owner, by being a member of a team nominated to be the organisation admins.
 * The role is organisation specific, so being an organisation admin of one organisation does not enable the
 * permission on other organisations. It does not dictate which organisation, but as there can only be one
 * organisation, that is implied.
 *
 * This role is evaluated for each call, so the effect will be immediate and does not require logout.
 *
 * Allows configuration of the organisation (as a member of that organisation).
 */
const val ROLE_ORGANISATION_OWNER = "organisation_owner"

/**
 * Obtained by manual configuration of the platform startup. This is usually a service account credential,
 * though there is nothing stopping this being assigned to any user. However, be exceptionally careful about
 * assigning this in a multi-organisation mode, it allows the configuration of the platform underlying all
 * organisations. It will not allow data access to the organisations, but can easily corrupt the runtime environment.
 *
 * Allows configuration of the environments, runners etc. It does not allow across organisation access.
 */
const val ROLE_PLATFORM_ADMIN = "platform_admin"

/**
 * This is a dynamic role obtained by being the owner of one or more processes. It does not dictate process access,
 * only that this person owns one or more processes.
 *
 * This role is evaluated for each call, so the effect will be immediate and does not require logout.
 *
 * Allows configuration of the organisation (as a member of that organisation).
 */
const val ROLE_PROCESS_OWNER = "process_owner"

/**
 * This is a dynamic role obtained by being the owner of one or more projects. It does not dictate project
 * access, only that this person owns one or more projects.
 *
 * This role is evaluated for each call, so the effect will be immediate and does not require logout.
 *
 * Allows configuration of the organisation (as a member of that organisation).
 */
const val ROLE_PROJECT_OWNER = "project_owner"

/**
 * Obtained internally by the system for identifying system generated activities.
 *
 * Allows access to cross organisations for specific calls such as logins. See [SecurityHelpers].
 */
const val ROLE_SYSTEM = "system"

// TODO: shall we just define an effective access-to-object?
