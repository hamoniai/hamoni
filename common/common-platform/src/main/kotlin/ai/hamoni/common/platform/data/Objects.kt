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

package ai.hamoni.common.platform.data

/**
 * An object that maps to an error being sent to the frontend. Maps RFC9457
 */
sealed class Problem {
    /**
     * Reference RFC9457 type
     */
    abstract val type: String
    /**
     * Reference RFC9457 title
     */
    abstract val title: String
    /**
     * Reference RFC9457 status
     */
    abstract val status: Int
    /**
     * Reference RFC9457 detail
     */
    abstract val detail: String
    /**
     * Reference RFC9457 instance. Should contain:
     * urn:hamoni:error?requestId=<requestId>&code=<code>
     */
    abstract val instance: String

    /**
     * A subclass of problem specifically for general errors
     */
    data class GenericProblem(
        override val type: String,
        override val title: String,
        override val status: Int,
        override val detail: String,
        override val instance: String) : Problem()

    /**
     * A subclass of problem specifically for validation errors.
     */
    data class ValidationProblem(
        override val type: String,
        override val title: String,
        override val status: Int,
        override val detail: String,
        override val instance: String,
        /**
         * Additional field - validation errors
         */
        val errors: List<ValidationError>) : Problem()
}

/**
 * A class representing a validation problem
 */
data class ValidationError (
    /**
     * The exact error message of the error
     */
    val detail: String,
    /**
     * The source of the error. Usually a JSON message pointer
     */
    val source: String
)