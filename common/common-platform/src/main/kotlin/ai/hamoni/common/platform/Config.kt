package ai.hamoni.common.platform

import ai.hamoni.common.core.PRODUCT_ID
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithConverter
import io.smallrye.config.WithDefault
import org.eclipse.microprofile.config.spi.Converter
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
    fun defaultOrganisation(): OrganisationConfig

    /**
     * The tenant configuration
     */
    interface OrganisationConfig {
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

