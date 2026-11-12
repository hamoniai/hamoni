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

import ai.hamoni.quarkus.ext.quarkus.multi_tenant.deployment.QuarkusSqlClientPoolClassTransformer
import java.lang.reflect.Proxy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

/**
 * Behavioural tests for the class visitors in
 * [ai.hamoni.quarkus.ext.quarkus.multi_tenant.deployment.ClassVisitors].
 */
class QuarkusSqlClientPoolClassTransformerTest {

    @Test
    fun `getTenantPool wraps the current pool in a multi tenant schema pool`() {
        // Given
        val pool = newPool()
        val transformedClass = transform(fixtureClass())
        val instance = transformedClass.getDeclaredConstructor(POOL_CLASS).newInstance(pool)
        val getTenantPool = transformedClass.getDeclaredMethod(GET_TENANT_POOL, String::class.java)
            .apply { isAccessible = true }

        // When
        val tenantPool = getTenantPool.invoke(instance, TENANT_ID)

        // Then
        assertThat(tenantPool.javaClass.name).isEqualTo(MULTI_TENANT_SCHEMA_POOL)
        assertThat(tenantPool.javaClass.getMethod(POOL_SIZE).invoke(tenantPool)).isEqualTo(POOL_SIZE_VALUE)
    }

    /**
     * Transform the supplied class bytes with the transformer under test.
     */
    private fun transform(classBytes: ByteArray): Class<*> {
        val writer = ClassWriter(0)
        ClassReader(classBytes).accept(QuarkusSqlClientPoolClassTransformer(writer), 0)
        return define(writer.toByteArray())
    }

    /**
     * Build a class shaped like QuarkusSqlClientPool: it holds a pool and exposes
     * the protected [GET_POOL] method the transformer calls.
     */
    private fun fixtureClass(): ByteArray {
        val writer = ClassWriter(0)
        writer.visit(
            Opcodes.V17,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER,
            FIXTURE_CLASS_NAME,
            null,
            "java/lang/Object",
            null
        )
        writer.visitField(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL,
            POOL_FIELD,
            POOL_DESCRIPTOR,
            null,
            null
        ).visitEnd()
        writer.visitMethod(Opcodes.ACC_PUBLIC, CONSTRUCTOR, POOL_CONSTRUCTOR_DESCRIPTOR, null, null).apply {
            visitCode()
            visitVarInsn(Opcodes.ALOAD, 0)
            visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", CONSTRUCTOR, "()V", false)
            visitVarInsn(Opcodes.ALOAD, 0)
            visitVarInsn(Opcodes.ALOAD, 1)
            visitFieldInsn(Opcodes.PUTFIELD, FIXTURE_CLASS_NAME, POOL_FIELD, POOL_DESCRIPTOR)
            visitInsn(Opcodes.RETURN)
            visitMaxs(2, 2)
            visitEnd()
        }
        writer.visitMethod(Opcodes.ACC_PROTECTED, GET_POOL, GET_POOL_DESCRIPTOR, null, null).apply {
            visitCode()
            visitVarInsn(Opcodes.ALOAD, 0)
            visitFieldInsn(Opcodes.GETFIELD, FIXTURE_CLASS_NAME, POOL_FIELD, POOL_DESCRIPTOR)
            visitInsn(Opcodes.ARETURN)
            visitMaxs(1, 1)
            visitEnd()
        }
        writer.visitEnd()
        return writer.toByteArray()
    }

    /**
     * Create a non-null [POOL_CLASS] instance whose [POOL_SIZE] returns a known value.
     */
    private fun newPool(): Any =
        Proxy.newProxyInstance(POOL_CLASS.classLoader, arrayOf(POOL_CLASS)) { _, method, _ ->
            when (method.name) {
                POOL_SIZE -> POOL_SIZE_VALUE
                else -> when (method.returnType) {
                    Boolean::class.javaPrimitiveType -> false
                    Int::class.javaPrimitiveType -> 0
                    Long::class.javaPrimitiveType -> 0L
                    else -> null
                }
            }
        }

    /**
     * Define the transformed fixture in an isolated loader so it can be loaded.
     */
    private fun define(classBytes: ByteArray): Class<*> =
        object : ClassLoader(javaClass.classLoader) {
            fun define(): Class<*> =
                defineClass(FIXTURE_BINARY_NAME, classBytes, 0, classBytes.size)
        }.define()

    private companion object {
        private const val CONSTRUCTOR = "<init>"
        private const val FIXTURE_BINARY_NAME =
            "ai.hamoni.quarkus.ext.quarkus.multi_tenant.test.QuarkusSqlClientPoolFixture"
        private const val FIXTURE_CLASS_NAME =
            "ai/hamoni/quarkus/ext/quarkus/multi_tenant/test/QuarkusSqlClientPoolFixture"
        private const val GET_POOL = "getPool"
        private const val GET_POOL_DESCRIPTOR = "()Lio/vertx/sqlclient/Pool;"
        private const val GET_TENANT_POOL = "getTenantPool"
        private const val MULTI_TENANT_SCHEMA_POOL =
            "ai.hamoni.quarkus.ext.quarkus.multi_tenant.runtime.MultiTenantSchemaPool"
        private const val POOL_CONSTRUCTOR_DESCRIPTOR = "(Lio/vertx/sqlclient/Pool;)V"
        private const val POOL_DESCRIPTOR = "Lio/vertx/sqlclient/Pool;"
        private const val POOL_FIELD = "pool"
        private const val POOL_SIZE = "size"
        private const val POOL_SIZE_VALUE = 42
        private const val TENANT_ID = "550e8400-e29b-41d4-a716-446655440000"
        private val POOL_CLASS: Class<*> = Class.forName("io.vertx.sqlclient.Pool")
    }
}


