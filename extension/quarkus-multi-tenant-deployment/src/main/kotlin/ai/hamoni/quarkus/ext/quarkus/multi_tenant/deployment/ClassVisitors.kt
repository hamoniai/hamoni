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

package ai.hamoni.quarkus.ext.quarkus.multi_tenant.deployment

import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/*
 * Class visitors for patching specific classes.
 */

/**
 * Transform the QuarkusSqlClientPool class.
 *
 * Actions:
 * 1. Create an override for [org.hibernate.reactive.pool.impl.SqlClientPool.getTenantPool]
 *    that wraps the current Vert.x pool in a [MultiTenantSchemaPool].
 */
class QuarkusSqlClientPoolClassTransformer(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM9, cv) {

    private var className: String? = null
    private var getTenantPoolOverridden = false

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    /**
     * Track an existing [getTenantPool] override so the method is never added twice.
     */
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        if (name == GET_TENANT_POOL && descriptor == GET_TENANT_POOL_DESCRIPTOR) {
            getTenantPoolOverridden = true
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    override fun visitEnd() {
        if (!getTenantPoolOverridden) {
            className?.let(::addGetTenantPool)
        }
        super.visitEnd()
    }

    /**
     * Add
     * `protected Pool getTenantPool(String tenantId) { return new MultiTenantSchemaPool(getPool(), tenantId); }`.
     */
    private fun addGetTenantPool(owner: String) {
        super.visitMethod(
            Opcodes.ACC_PROTECTED,
            GET_TENANT_POOL,
            GET_TENANT_POOL_DESCRIPTOR,
            null,
            null
        ).apply {
            visitCode()
            visitTypeInsn(Opcodes.NEW, MULTI_TENANT_SCHEMA_POOL)
            visitInsn(Opcodes.DUP)
            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, GET_POOL, GET_POOL_DESCRIPTOR, false)
            visitVarInsn(Opcodes.ALOAD, 1)
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                MULTI_TENANT_SCHEMA_POOL,
                CONSTRUCTOR,
                MULTI_TENANT_SCHEMA_POOL_CONSTRUCTOR_DESCRIPTOR,
                false
            )
            visitInsn(Opcodes.ARETURN)
            visitMaxs(4, 2)
            visitEnd()
        }
    }

    private companion object {
        private const val CONSTRUCTOR = "<init>"
        private const val GET_POOL = "getPool"
        private const val GET_POOL_DESCRIPTOR = "()Lio/vertx/sqlclient/Pool;"
        private const val GET_TENANT_POOL = "getTenantPool"
        private const val GET_TENANT_POOL_DESCRIPTOR = "(Ljava/lang/String;)Lio/vertx/sqlclient/Pool;"
        private const val MULTI_TENANT_SCHEMA_POOL =
            "ai/hamoni/quarkus/ext/quarkus/multi_tenant/runtime/MultiTenantSchemaPool"
        private const val MULTI_TENANT_SCHEMA_POOL_CONSTRUCTOR_DESCRIPTOR =
            "(Lio/vertx/sqlclient/Pool;Ljava/lang/String;)V"
    }
}

/**
 * Transform the RecordedConfig class to apply the build-time configured multi-tenancy strategy.
 *
 * Actions:
 * 1. Replace [io.quarkus.hibernate.orm.runtime.recording.RecordedConfig.getMultiTenancyStrategy]
 *    so it returns the configured [MultiTenancyStrategy] constant.
 *
 * See `ISSUES.md` (quarkusio/quarkus#58249) for the related Quarkus issue.
 */
class RecordedConfigClassTransformer(
    cv: ClassVisitor,
    private val multiTenancyStrategy: MultiTenancyStrategy,
) : ClassVisitor(Opcodes.ASM9, cv) {

    /**
     * Remove the existing getter; the replacement is added in [visitEnd].
     */
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        if (name == GET_MULTI_TENANCY_STRATEGY && descriptor == GET_MULTI_TENANCY_STRATEGY_DESCRIPTOR) {
            return null
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    /**
     * Add
     * `public MultiTenancyStrategy getMultiTenancyStrategy() { return MultiTenancyStrategy.<configured>; }`.
     */
    override fun visitEnd() {
        super.visitMethod(
            Opcodes.ACC_PUBLIC,
            GET_MULTI_TENANCY_STRATEGY,
            GET_MULTI_TENANCY_STRATEGY_DESCRIPTOR,
            null,
            null
        ).apply {
            visitCode()
            visitFieldInsn(
                Opcodes.GETSTATIC,
                MULTI_TENANCY_STRATEGY,
                multiTenancyStrategy.name,
                MULTI_TENANCY_STRATEGY_DESCRIPTOR
            )
            visitInsn(Opcodes.ARETURN)
            visitMaxs(1, 1)
            visitEnd()
        }
        super.visitEnd()
    }

    private companion object {
        private const val GET_MULTI_TENANCY_STRATEGY = "getMultiTenancyStrategy"
        private const val GET_MULTI_TENANCY_STRATEGY_DESCRIPTOR =
            "()Lio/quarkus/hibernate/orm/runtime/migration/MultiTenancyStrategy;"
        private const val MULTI_TENANCY_STRATEGY =
            "io/quarkus/hibernate/orm/runtime/migration/MultiTenancyStrategy"
        private const val MULTI_TENANCY_STRATEGY_DESCRIPTOR =
            "Lio/quarkus/hibernate/orm/runtime/migration/MultiTenancyStrategy;"
    }
}