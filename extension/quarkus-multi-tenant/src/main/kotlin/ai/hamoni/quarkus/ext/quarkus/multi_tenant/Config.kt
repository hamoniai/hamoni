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

package ai.hamoni.quarkus.ext.quarkus.multi_tenant

import ai.hamoni.quarkus.ext.quarkus.multi_tenant.runtime.TenantSqlConnection
import io.quarkus.runtime.annotations.ConfigPhase
import io.quarkus.runtime.annotations.ConfigRoot
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault

/**
 * Configuration for the multi-tenant extension.
 */
@ConfigMapping(prefix = "hamoni.ext.quarkus.multi-tenant")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
interface Config {
    /**
     * The quoted schema placeholder Hibernate emits for unqualified organisation tables.
     */
    @WithDefault(TenantSqlConnection.DEFAULT_TENANT_SCHEMA_PLACEHOLDER)
    fun placeholder(): String
}