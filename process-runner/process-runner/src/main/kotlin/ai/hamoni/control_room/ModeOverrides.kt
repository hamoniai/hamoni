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

package ai.hamoni.control_room

import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.h2.value.DataType
import org.h2.value.TypeInfo
import java.sql.Types

/*
 * Mode specific overrides
 */

/**
 * Fixups for the H2 drive JSON incompatibility.
 */
@IfBuildProfile("dev")
@ApplicationScoped
class AppInitializer {
    fun onStart(@Observes event: StartupEvent) {
        // Need to configure the H2 driver. See the root test support package for reasoning. This does not
        // exist in production, which uses MySQL.
        val dataType = DataType.getDataType(TypeInfo.TYPE_JSON.valueType)
        dataType.sqlType = Types.VARBINARY
    }
}
