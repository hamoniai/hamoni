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
package ai.hamoni.common.core.shared

/**
 * A typed settings key.
 *
 * [code] is the stable key used by persistence and the interface. [default] is
 * the typed value used when no setting is stored.
 */
interface SettingKey<T : Any> {
    /** The stable key code. */
    val code: String

    /** The value used when no setting is stored. */
    val default: T

    /** Encode [value] to its stored text representation. */
    fun encode(value: T): String

    /** Decode [value] from its stored text representation. */
    fun decode(value: String): T
}
