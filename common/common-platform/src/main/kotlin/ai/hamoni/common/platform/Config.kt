package ai.hamoni.common.platform

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithConverter
import io.smallrye.config.WithDefault
import org.eclipse.microprofile.config.spi.Converter
import java.util.*

/**
 * Platform configuration properties. These use a custom config source to provide defualts.
 */
@ConfigMapping(prefix = "hamoni.platform")
interface HamoniPlatformConfig {
    /**
     * The default tenant. If not specified, it will be created as a random value. On subsequent
     * boots, the default tenant will be determined from the database. If a default tenant has already been created
     * and this value is specified, and the value is not equal to the already created default, the default tenant
     * will be re-initialised. The previous default will be unset.
     */
    fun defaultTenant(): TenantConfig

    /**
     * The tenant configuration
     */
    interface TenantConfig {
        /**
         * The tenant identifier
         */
        val id: UUID
    }

    /**
     * The platform security configuration
     */
    fun security(): SecurityConfig

    /**
     * The security configuration
     */
    interface SecurityConfig {
        /**
         * The security key for the platform. All nodes in the same cluster must have the same platform key. The
         * default values are dependent on the source.
         */
        fun key(): String

        /**
         * The source of the platform key material
         */
        @WithDefault("platform")
        @WithConverter(SecurityConfigKeySourceConverter::class)
        fun keySource(): SecurityConfigKeySource

        /**
         * Where to obtain the key from
         */
        enum class SecurityConfigKeySource {
            /**
             * A static configuration. They key value should be manually set on startup. If it is not set, a random
             * value will be assigned.
             */
            STATIC,
            /**
             * The platform. The exact source depends on the platform configured.
             */
            PLATFORM,
        }

        /**
         * A converter for the case
         */
        class SecurityConfigKeySourceConverter : Converter<SecurityConfigKeySource> {
            override fun convert(value: String): SecurityConfigKeySource {
                return SecurityConfigKeySource.valueOf(value.uppercase().trim())
            }
        }
    }
}

