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

package ai.hamoni.common.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Objects and entities that are global. This also contains associated enums if they are used in the API.
 */

/**
 * Key/value table types.
 */
@Serializable
enum class KeyValueTableKeyType {
    @SerialName("any")
    ANY,

    @SerialName("string")
    STRING,

}
