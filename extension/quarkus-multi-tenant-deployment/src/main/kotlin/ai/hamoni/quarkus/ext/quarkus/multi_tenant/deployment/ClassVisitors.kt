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

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

/*
 * Class visitors for patching specific classes.
 */

/**
 * Transform the QuarkusSqlClientPool class.
 *
 * Actions:
 * 1. Create an override for [org.hibernate.reactive.pool.impl.SqlClientPool.getTenantPool]
 */
class QuarkusSqlClientPoolClassTransformer(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM9, cv) {
}

/**
 * Transform the RecordedConfig class. This is very tricky because the class below is annotated with
 * '@RecordableConstructor', so patching it has to be done really early, i.e. before anything else uses the constructor.
 *
 * We might be able to override the getter??
 *
 * Actions:
 * 1. Patch the constructor in [io.quarkus.hibernate.orm.runtime.recording.RecordedConfig] to use schema multi-tenancy
 */
class RecordedConfigClassTransformer(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM9, cv) {
}