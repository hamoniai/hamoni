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

import ai.hamoni.quarkus.ext.quarkus.multi_tenant.Config
import ai.hamoni.quarkus.ext.quarkus.multi_tenant.ConstantsSettingRecorder
import io.quarkus.deployment.annotations.BuildProducer
import io.quarkus.deployment.annotations.BuildStep
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem
import io.quarkus.deployment.builditem.FeatureBuildItem
import org.objectweb.asm.ClassVisitor

/**
 * The main processor for the extension
 */
internal class QuarkusMultiTenantProcessor {
    companion object {
        private const val FEATURE = "quarkus-multi-tenant"
        private const val QUARKUS_SQL_CLIENT_POOL =
            "io.quarkus.hibernate.reactive.runtime.customized.QuarkusSqlClientPool"
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
        val transformers: Map<String, (ClassVisitor) -> ClassVisitor> = mapOf(
            QUARKUS_SQL_CLIENT_POOL to ::QuarkusSqlClientPoolClassTransformer,
        )

        transformers
            .map { (className, transformer) ->
                BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(className)
                    .setVisitorFunction { _, visitor -> transformer(visitor) }
                    .build()
            }
            .forEach {
                bytecodeTransformers.produce(it)
            }
    }

    /**
     * Set the placeholder value at build time.
     * Related issue: https://github.com/quarkusio/quarkus/issues/58349
     *
     * Effectively disabled for now because the recorder has the same classloader issue.
     */
    //@BuildStep
    //@Record(ExecutionTime.STATIC_INIT)
    fun setupConstants(
        constantsSettingRecorder: ConstantsSettingRecorder,
        config: Config
    ) {
        constantsSettingRecorder.setTenantPlaceholder(config.placeholder())
    }
}