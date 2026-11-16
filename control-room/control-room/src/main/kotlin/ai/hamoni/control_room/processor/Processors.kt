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

package ai.hamoni.control_room.processor

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing


/*
 * Processors for reactive messages. Used for communication between the control room to other components.
 */

/**
 * Generator for process runner interaction events.
 */
@ApplicationScoped
class ProcessRunnerGenerator {
    @Incoming("process-runner-status-response")
    @Outgoing("process-runner-status-request")
    fun status(pingPayload: String?): String {
        if ("PING".equals(pingPayload, ignoreCase = true)) {
            return "PONG"
        }
        return "UNKNOWN"
    }

}
