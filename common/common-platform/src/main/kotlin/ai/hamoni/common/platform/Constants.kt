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

package ai.hamoni.common.platform

import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE
import java.nio.charset.StandardCharsets

/*
 * Global database definitions
 */
const val GLOBAL_SCHEMA = "platform"

/*
 * media types with character set.
 */
const val APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON + "; charset=utf-8"
val APPLICATION_JSON_UTF8_TYPE = MediaType(APPLICATION_JSON_TYPE.type, APPLICATION_JSON_TYPE.subtype,
    StandardCharsets.UTF_8.name())

const val APPLICATION_PROBLEM_JSON_UTF8 = "application/problem+json; charset=utf-8"
val APPLICATION_PROBLEM_JSON_UTF8_TYPE = MediaType(APPLICATION_JSON_TYPE.type, "problem+json",
    StandardCharsets.UTF_8.name())

/*
 * Request/response properties.
 */
const val REQUEST_PROPERTY_ID = "Request-Id"
