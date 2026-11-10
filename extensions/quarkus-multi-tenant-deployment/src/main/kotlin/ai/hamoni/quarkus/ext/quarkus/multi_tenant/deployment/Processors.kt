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

@file:Suppress("unused")
package ai.hamoni.quarkus.ext.quarkus.multi_tenant.deployment

import io.quarkus.deployment.annotations.BuildProducer
import io.quarkus.deployment.annotations.BuildStep
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem
import io.quarkus.deployment.builditem.FeatureBuildItem

/**
 * The main processor for the extension
 */
internal class QuarkusMultiTenant {
    companion object {
        private const val FEATURE = "quarkus-multi-tenant"

        private val QUARKUS_CLASSES = mapOf(
            "io.quarkus.hibernate.reactive.runtime.customized.QuarkusSqlClientPool" to ::QuarkusSqlClientPoolClassTransformer,
            "io.quarkus.hibernate.orm.runtime.recording.RecordedConfig" to ::RecordedConfigClassTransformer,
        )
    }

    @BuildStep
    fun feature(): FeatureBuildItem {
        return FeatureBuildItem(FEATURE)
    }

    /**
     * Build step to transform the QuarkusSqlClientPool.
     */
    @BuildStep
    fun instrumentQuarkusClasses(
        bytecodeTransformers: BuildProducer<BytecodeTransformerBuildItem>
    ) {
        QUARKUS_CLASSES
            .map {
                BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(it.component1())
                    .setVisitorFunction { _, visitor -> it.component2()(visitor) }
                    .build()
            }
            .forEach {
                bytecodeTransformers.produce(it)
            }
    }
}