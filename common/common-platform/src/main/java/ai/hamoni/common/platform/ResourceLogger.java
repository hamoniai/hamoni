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

package ai.hamoni.common.platform;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import java.lang.invoke.MethodHandles;

/**
 * Must do this in Java as there is a bug in the annotation processor for JBoss Logging
 * that detects NotNull annotations and thinks they are not arguments.
 */
@MessageLogger(projectCode = "HA-PLAT")
@SuppressWarnings("doclint:missing")
public interface ResourceLogger {
    ResourceLogger LOGGER = Logger.getMessageLogger(
            MethodHandles.lookup(), ResourceLogger.class, ResourceLogger.class.getPackageName()
    );

    /*
     * REST errors
     */
    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 100, format = Message.Format.MESSAGE_FORMAT, value = "REST error (''{0}'')")
    void restError(String reference, @Cause Throwable cause);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 101, format = Message.Format.MESSAGE_FORMAT, value = "Validation error: (''{0}'') ''{1}'': {2}")
    void restValidationError(String reference, String service, String message);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 102, format = Message.Format.MESSAGE_FORMAT, value = "Entity not found error: (''{0}'') ''{1}''")
    void restNotFoundError(String reference, String service);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 103, format = Message.Format.MESSAGE_FORMAT, value = "Security error: (''{0}'') ''{1}'': {2}")
    void restSecurityError(String reference, String service, String message);
}
