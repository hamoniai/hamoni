/*
 * Hamoni: Cloud workflow platform
 * Copyright (C) 2026 Hamoni Limited
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
package ai.hamoni.common.platform

import ai.hamoni.common.core.PRODUCT_ID
import io.quarkus.runtime.annotations.StaticInitSafe
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithParentName
import jakarta.validation.constraints.Positive
import java.util.*

/**
 * Platform configuration properties. These are runtime adjustable.
 */
@ConfigMapping(prefix = "${PRODUCT_ID}.platform")
interface PlatformConfig {
    /**
     * The default organisation configuration. This must be defined for the control room, it is not relevant for the
     * process runner as all processes are run in the context of an organisation. There is a startup check to make
     * sure that the organisation is configured. If it is not, then the init script must be provided to initialise
     * the organisation.
     */
    fun defaultOrganisation(): DefaultOrganisationConfig

    /**
     * The default organisation (tenant) configuration
     */
    interface DefaultOrganisationConfig {
        /**
         * The default tenant. This must be defined for the control room, it is not relevant for the process runner as
         * all processes are run in the context of an organisation.
         */
        fun id(): UUID

        /**
         * Initial config. Used if the organisation is not configured.
         */
        fun config(): Optional<String>
    }

//    /**
//     * The platform security configuration
//     */
//    fun security(): SecurityConfig
//
//    /**
//     * The security configuration
//     */
//    interface SecurityConfig {
//        /**
//         * The security key for the platform. All nodes in the same cluster must have the same platform key. The
//         * default values are dependent on the source.
//         */
//        fun key(): String
//
//        /**
//         * The source of the platform key material
//         */
//        @WithDefault("platform")
//        @WithConverter(SecurityConfigKeySourceConverter::class)
//        fun keySource(): SecurityConfigKeySource
//
//        /**
//         * Where to obtain the key from
//         */
//        enum class SecurityConfigKeySource {
//            /**
//             * A static configuration. They key value should be manually set on startup. If it is not set, a random
//             * value will be assigned.
//             */
//            STATIC,
//            /**
//             * The platform. The exact source depends on the platform configured.
//             */
//            PLATFORM,
//        }
//
//        /**
//         * A converter for the case
//         */
//        class SecurityConfigKeySourceConverter : Converter<SecurityConfigKeySource> {
//            override fun convert(value: String): SecurityConfigKeySource {
//                return SecurityConfigKeySource.valueOf(value.uppercase().trim())
//            }
//        }
//    }
}

/**
 * Multi organisation mode configuration. Designed to be build time fixed.
 */
@ConfigMapping(prefix = "${PRODUCT_ID}.platform.multi-organisation")
@StaticInitSafe
interface MultiOrganisationConfig {
    /**
     * Enable multi-organisation mode. This has implications on security and operations that should be
     * recognised before enabling.
     *
     * Disabled by default as organisations will generally want one organisation.
     */
    @WithDefault("false")
    @WithParentName
    fun enabled(): Boolean

    /**
     * Allow a user to provision their own organisations. Further down the line we might want to create constraints
     * around this, i.e. a user can create one organisation, but be a participant of many.
     */
    fun selfProvisioning(): MultiOrganisationConfigSelfProvisioning

    /**
     * The config for self provisioning
     */
    interface MultiOrganisationConfigSelfProvisioning {
        /**
         * Self provisioning disabled by default. Organisations will not want staff creating their own organisations
         * independently,
         */
        @WithDefault("false")
        @WithParentName
        fun enabled(): Boolean

        /**
         * The maximum number of self-provisioned organisations. Ignored if disabled. The default is large enough to
         * effectively be unlimited, without having a bad actor spam the platform.
         */
        @Positive(message = "The maximum number of self-provisioned organisations must be greater than 0")
        @WithDefault("1000")
        fun max(): Int
    }
}
