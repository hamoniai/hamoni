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

package ai.hamoni.common.platform.exception

import kotlin.reflect.KFunction

/*
 * All exceptions should extend RuntimeException, following the general Reactive style.
 */

/**
 * Base for all exceptions
 */
abstract class ApplicationException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * An exception thrown by the service layer
 */
open class ServiceException : ApplicationException {
    // The service that generated the exception
    val serviceMethod: KFunction<Any?>

    constructor(serviceMethod: KFunction<Any?>) : super() {
        this.serviceMethod = serviceMethod
    }
    constructor(serviceMethod: KFunction<Any?>, message: String) : super(message) {
        this.serviceMethod = serviceMethod
    }
    constructor(serviceMethod: KFunction<Any?>, message: String, cause: Throwable) : super(message, cause) {
        this.serviceMethod = serviceMethod
    }
    constructor(serviceMethod: KFunction<Any?>, cause: Throwable) : super(cause) {
        this.serviceMethod = serviceMethod
    }
}

/**
 * Specific service exception for when an entity is not found, and should be expected
 */
open class EntityNotFoundException : ServiceException {
    constructor(serviceMethod: KFunction<Any?>) : super(serviceMethod)
    constructor(serviceMethod: KFunction<Any?>, message: String) : super(serviceMethod, message)
    constructor(serviceMethod: KFunction<Any?>, message: String, cause: Throwable) : super(serviceMethod, message, cause)
    constructor(serviceMethod: KFunction<Any?>, cause: Throwable) : super(serviceMethod, cause)
}

/**
 * Service exception for when there is a security concern
 */
open class SecurityException : ServiceException {
    constructor(serviceMethod: KFunction<Any?>) : super(serviceMethod)
    constructor(serviceMethod: KFunction<Any?>, message: String) : super(serviceMethod, message)
    constructor(serviceMethod: KFunction<Any?>, message: String, cause: Throwable) : super(serviceMethod, message, cause)
    constructor(serviceMethod: KFunction<Any?>, cause: Throwable) : super(serviceMethod, cause)
}

