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

package ai.hamoni.quarkus.ext.quarkus.multi_tenant.test

import ai.hamoni.quarkus.ext.quarkus.multi_tenant.ConstantsSettingRecorder
import ai.hamoni.quarkus.ext.quarkus.multi_tenant.runtime.TenantSqlConnection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Behavioural tests for [ConstantsSettingRecorder].
 */
class RecordersTest {

    @Test
    fun `the recorder sets the tenant placeholder`() {
        // Given
        val previous = TenantSqlConnection.TENANT_SCHEMA_PLACEHOLDER
        val recorder = ConstantsSettingRecorder()

        try {
            // When
            recorder.setTenantPlaceholder("custom")

            // Then
            assertThat(TenantSqlConnection.TENANT_SCHEMA_PLACEHOLDER).isEqualTo("custom")
        } finally {
            TenantSqlConnection.TENANT_SCHEMA_PLACEHOLDER = previous
        }
    }
}
