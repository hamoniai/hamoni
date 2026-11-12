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

package ai.hamoni.buildlogic.multitenant;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.constant.ClassDesc;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * Java agent that enables Hibernate Reactive schema multi-tenancy before Quarkus
 * builds the persistence unit.
 *
 * <p>The Hibernate Reactive deployment processor hardcodes
 * {@code MultiTenancyStrategy.NONE} in {@code producePersistenceUnitFromConfig}.
 * This agent replaces that field access with {@code SCHEMA}. It is a temporary
 * workaround for the upstream Quarkus behaviour and is only effective when the
 * agent is attached to the JVM performing the build.</p>
 *
 * <p>The target class and method names are Quarkus 3.x specific. Quarkus 4
 * changes the deployment method names, so this agent must be updated or removed
 * before upgrading to Quarkus 4. The transform logs a message and leaves the
 * class unchanged when the target instruction is not found.</p>
 *
 * <p>This is Java rather than Kotlin because the agent is loaded by the system
 * classloader and must be a self-contained jar; Kotlin would require bundling
 * the Kotlin runtime. It uses only the JDK class-file API.</p>
 */
public final class QuarkusMultiTenantAgent {

    private static final String TARGET_CLASS =
            "io/quarkus/hibernate/reactive/deployment/HibernateReactiveProcessor";
    // Quarkus 3.x method name. Quarkus 4 changes the deployment method names;
    // update or remove this agent before upgrading. See the class Javadoc.
    private static final String TARGET_METHOD = "producePersistenceUnitFromConfig";
    private static final String STRATEGY_OWNER =
            "io/quarkus/hibernate/orm/runtime/migration/MultiTenancyStrategy";
    private static final ClassDesc STRATEGY_TYPE =
            ClassDesc.of("io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy");
    private static final String NONE = "NONE";
    private static final String SCHEMA = "SCHEMA";

    private QuarkusMultiTenantAgent() {
    }

    /**
     * Register the Hibernate Reactive transformer.
     *
     * @param agentArguments unused
     * @param instrumentation the JVM instrumentation instance
     */
    public static void premain(String agentArguments, Instrumentation instrumentation) {
        instrumentation.addTransformer(new MultiTenantTransformer());
    }

    /**
     * Replace {@code MultiTenancyStrategy.NONE} with {@code SCHEMA} in the
     * Hibernate Reactive persistence-unit build method.
     *
     * <p>The transform is stateless and deterministic, so it can safely be
     * applied to every classloader copy of the target class.</p>
     *
     * @param classfileBuffer the original class bytes
     * @return the transformed class bytes
     */
    static byte[] transform(byte[] classfileBuffer) {
        ClassModel model = ClassFile.of().parse(classfileBuffer);
        boolean[] replaced = {false};
        byte[] transformed = ClassFile.of().transformClass(
                model,
                ClassTransform.transformingMethodBodies(
                        method -> TARGET_METHOD.equals(method.methodName().stringValue()),
                        (builder, element) -> {
                            if (element instanceof FieldInstruction field
                                    && field.opcode() == Opcode.GETSTATIC
                                    && STRATEGY_OWNER.equals(field.owner().asInternalName())
                                    && NONE.equals(field.name().stringValue())) {
                                builder.getstatic(STRATEGY_TYPE, SCHEMA, STRATEGY_TYPE);
                                replaced[0] = true;
                            } else {
                                builder.with(element);
                            }
                        }));

        if (!replaced[0]) {
            System.err.println("quarkus-multi-tenant-agent: no MultiTenancyStrategy.NONE "
                    + "instruction found in " + TARGET_CLASS + "#" + TARGET_METHOD);
        }
        return transformed;
    }

    /**
     * Transform the Hibernate Reactive deployment processor as it is loaded.
     */
    private static final class MultiTenantTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(Module module, ClassLoader loader, String className,
                Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws IllegalClassFormatException {
            if (!TARGET_CLASS.equals(className) || classfileBuffer == null) {
                return null;
            }

            try {
                return QuarkusMultiTenantAgent.transform(classfileBuffer);
            } catch (RuntimeException exception) {
                IllegalClassFormatException failure = new IllegalClassFormatException(
                        "quarkus-multi-tenant-agent: failed to transform " + TARGET_CLASS);
                failure.initCause(exception);
                throw failure;
            }
        }
    }
}
