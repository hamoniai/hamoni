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

package ai.hamoni.control_room;

import org.flywaydb.core.api.FlywayException;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Must do this in Java as there is a bug in the annotation processor for JBoss Logging
 * that detects NotNull annotations and thinks they are not arguments.
 */
@MessageLogger(projectCode = "HA-CR")
@SuppressWarnings("doclint:missing")
public interface ResourceLogger {
    ResourceLogger LOGGER = Logger.getMessageLogger(
            MethodHandles.lookup(), ResourceLogger.class, ResourceLogger.class.getPackageName()
    );

    /*
     * Startup related errors
     */
    @LogMessage(level = Logger.Level.FATAL)
    @Message(id = 100, format = Message.Format.MESSAGE_FORMAT, value = "Failed to list schemas")
    void schemaQueryError(@Cause SQLException cause);

    @LogMessage(level = Logger.Level.FATAL)
    @Message(id = 101, format = Message.Format.MESSAGE_FORMAT, value = "Failed to migrate schema ''{0}''")
    void databaseMigrationError(String schema, @Cause FlywayException cause);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 102, format = Message.Format.MESSAGE_FORMAT, value = "Setting default organisation id to ''{0}''")
    void showDefaultOrganisationId(UUID defaultOrganisationId);
}
