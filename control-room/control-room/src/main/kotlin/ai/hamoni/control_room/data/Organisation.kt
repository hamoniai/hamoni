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
@file:OptIn(ExperimentalSerializationApi::class)

package ai.hamoni.control_room.data

import ai.hamoni.common.core.shared.SettingKey
import ai.hamoni.common.platform.serialization.UUIDSerializer
import ai.hamoni.control_room.service.OrganisationService
import jakarta.persistence.AttributeConverter
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Converter
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.ForeignKey
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.NamedNativeQueries
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import jakarta.persistence.OneToMany
import jakarta.persistence.QueryHint
import jakarta.persistence.Table
import jakarta.persistence.Transient
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.*

/*
 * All frontend specific entities and data types (including any projections) for the organisation specific schema go in
 * here. Entities and data types that are common to the frontend and backend should go into one of the common projects.
 */

/**
 * An organisation.
 *
 * The configuration is held as element collections and is populated by the
 * organisation service's hydration options.
 *
 * TODO: holidays, SCIM
 *
 * scope: schema
 *
 */
@Entity
@NamedNativeQueries(
    value = [
        NamedNativeQuery(
            name = "Organisation.checkOrganisationExists",
            hints = [QueryHint(name = "org.hibernate.readOnly", value = "true")],
            query = """
            SELECT
                COUNT(*) > 0
            FROM
                INFORMATION_SCHEMA.TABLES
            WHERE
                (LOWER(TABLE_CATALOG) = :schema OR LOWER(TABLE_SCHEMA) = :schema) AND LOWER(TABLE_NAME) = 'organisation'
        """,
            resultClass = Boolean::class
        )
    ]
)
@NamedQueries(
    value = [
        NamedQuery(
            name = "Organisation.isOrganisationInitialised",
            hints = [QueryHint(name = "org.hibernate.readOnly", value = "true")],
            query = """
            SELECT
                CASE 
                    WHEN COUNT(*) = 0 THEN ai.hamoni.control_room.data.OrganisationState.NOT_INITIALISED
                    WHEN COUNT(*) = 1 AND o.enabled = TRUE THEN ai.hamoni.control_room.data.OrganisationState.ACTIVE
                    WHEN COUNT(*) = 1 AND o.enabled = FALSE THEN ai.hamoni.control_room.data.OrganisationState.DISABLED
                    ELSE ai.hamoni.control_room.data.OrganisationState.ERROR
                END as organisationState
            FROM
                Organisation o
        """,
            resultClass = OrganisationState::class
        ),
        NamedQuery(
            name = "Organisation.getSetting",
            hints = [QueryHint(name = "org.hibernate.readOnly", value = "true")],
            query = """
            SELECT
                s.value
            FROM
                Organisation o
                JOIN o.settings s ON s.key = :key
        """
        ),
        NamedQuery(
            name = "Organisation.getSettings",
            hints = [QueryHint(name = "org.hibernate.readOnly", value = "true")],
            query = """
            SELECT
                s
            FROM
                Organisation o
                JOIN o.settings s
        """
        )
    ]
)
@Serializable
@Table(name = "organisation", comment = "Organisations")
class Organisation(
    @Id
    @Serializable(with = UUIDSerializer::class)
    @param:Schema(readOnly = true)
    var id: UUID? = null,

    @Column(name = "name", nullable = false, updatable = true, comment = "Organisation name")
    var name: String,

    @Column(name = "enabled", nullable = false, updatable = true, unique = false, comment = "Enabled status")
    var enabled: Boolean,

    @kotlinx.serialization.Transient
    @param:Schema(readOnly = true)
    @Column(name = "created", nullable = false, updatable = false, comment = "Date the account was created (UTC)")
    @CreationTimestamp
    var created: Instant? = null,

    @kotlinx.serialization.Transient
    @param:Schema(readOnly = true)
    @Column(name = "updated", nullable = false, updatable = true, comment = "Date the account was last updated (UTC)")
    @UpdateTimestamp
    var updated: Instant? = null,
) {
    /**
     * The organisation's settings, populated when the `settings`
     * hydration option is requested.
     */
    @ElementCollection
    @CollectionTable(
        name = "organisation_setting",
        joinColumns = [JoinColumn(name = "organisation_id")],
        foreignKey = ForeignKey(name = "fk_organisation_settings")
    )
    var settings: MutableList<OrganisationSetting> = mutableListOf()

    /**
     * The organisation's identity providers, populated when the
     * `identityProvider` hydration option is requested.
     */
    @OneToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE], orphanRemoval = true)
    @JoinColumn(name = "organisation_id", foreignKey = ForeignKey(name = "fk_organisation_identity_provider"))
    var identityProviders: MutableList<OrganisationIdentityProvider> = mutableListOf()

    /**
     * Compare organisations by their technical identifier, never by the display
     * name (CODING-STANDARDS §9).
     */
    override fun equals(other: Any?): Boolean = other is Organisation && id == other.id

    /**
     * Hash the technical identifier (CODING-STANDARDS §9).
     */
    override fun hashCode(): Int = Objects.hash(id)
}

/**
 * A projection of [Organisation] for a list.
 *
 * The header is the identity of an organisation only; it grows into a genuine
 * projection as the organisation gains fields a list does not need.
 */
@Serializable
@JvmRecord
data class OrganisationHeader(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,

    val name: String,
) {
    constructor(organisation: Organisation) : this(organisation.id!!, organisation.name)
}

/**
 * The related data [OrganisationService] can attach to an organisation.
 */
@Schema(enumeration = ["settings", "identityProviders"])
@Serializable
enum class OrganisationHydration {
    @SerialName("settings")
    CONFIGURATION,

    @SerialName("identityProviders")
    IDENTITY_PROVIDERS,
}

/**
 * The identity providers an organisation has enabled for sign-in.
 *
 * This is an empty placeholder until the authentication configuration is
 * modelled.
 *
 * scope: schema
 *
 */
@Entity
@KeepGeneratedSerializer
@Serializable(with = OrganisationIdentityProviderSerializer::class)
@Table(name = "organisation_identity_provider", comment = "Organisation identify providers (OIDC)")
class OrganisationIdentityProvider(
    /**
     * This is not generated as the actual generated value is in the platform organisation identity provider index.
     */
    @Id
    @Serializable(with = UUIDSerializer::class)
    @param:Schema(readOnly = true)
    var id: UUID? = null,

    /**
     * Security note: the name appears on the login page, do not choose names that are identifying. It will be shown as
     * "continue with <name>".
     */
    @Column(
        name = "name",
        nullable = false,
        updatable = true,
        unique = true,
        comment = "The display name for the OIDC provider"
    )
    var name: String,

    @Column(name = "url", nullable = false, updatable = true, unique = true, comment = "URL for the OIDC provider")
    var url: String,

    @Column(name = "client_id", nullable = false, updatable = true, unique = true, comment = "The client id")
    var clientId: String,

    /**
     * On write, this value is stored or updated in the credential store. It is not available by read.
     */
    @param:Schema(writeOnly = true)
    @Transient
    var clientSecret: String? = null,

    /**
     * True if this identity provider is enabled.
     */
    @Column(name = "enabled", nullable = false, updatable = true, unique = false, comment = "Enabled status")
    var enabled: Boolean,

    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false, comment = "Date the account was created (UTC)")
    @param:Schema(readOnly = true)
    @kotlinx.serialization.Transient
    var created: Instant? = null,

    @Column(name = "updated", nullable = false, updatable = true, comment = "Date the account was last updated (UTC)")
    @param:Schema(readOnly = true)
    @kotlinx.serialization.Transient
    @UpdateTimestamp
    var updated: Instant? = null,
) {
    override fun hashCode(): Int = Objects.hash(id)

    override fun equals(other: Any?): Boolean = other is OrganisationIdentityProvider && id == other.id
}

/**
 * Custom serializer to filter out write-only fields.
 */
object OrganisationIdentityProviderSerializer :
    JsonTransformingSerializer<OrganisationIdentityProvider>(OrganisationIdentityProvider.generatedSerializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        JsonObject(element.jsonObject.filterKeys { it != "clientSecret" })
}

/**
 * A projection of [OrganisationIdentityProvider] for a list.
 *
 * The header is the identity of an organisation only; it grows into a genuine
 * projection as the organisation gains fields a list does not need.
 *
 * WARNING: this object is available unauthenticated.
 */
@Serializable
@JvmRecord
data class OrganisationIdentityProviderHeader(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
) {
    constructor(
        organisationIdentityProvider: OrganisationIdentityProvider,
    ) : this(organisationIdentityProvider.id!!, organisationIdentityProvider.name)
}

/**
 * A projection of [OrganisationIdentityProvider] for public use. This is a special case
 *
 * WARNING: this object is available unauthenticated.
 */
@Serializable
@JvmRecord
data class OrganisationIdentityProviderHeaderWithSource(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    val default: Boolean,
) {
    constructor(
        organisationIdentityProvider: OrganisationIdentityProvider,
        default: Boolean = false,
    ) : this(organisationIdentityProvider.id!!, organisationIdentityProvider.name, default)
}

/**
 * The settings that configure how an organisation works.
 *
 * This is an empty placeholder until the settings are modelled.
 *
 * scope: schema
 *
 */
@Serializable
@Embeddable
class OrganisationSetting(
    /**
     * The key for the key/value pair.
     */
    @Column(name = "key", nullable = false, updatable = false, unique = true, comment = "Settings key")
    @Convert(converter = OrganisationSettingKeyConverter::class)
    @field:Schema(
        implementation = String::class,
        type = SchemaType.STRING,
        enumeration = ["country", "language", "timezone", "includeDefaultAuthenticationMethods"]
    )
    var key: OrganisationSettingKey<*>,

    /**
     * The key for the key/value pair.
     */
    @Column(name = "value", nullable = true, updatable = true, comment = "Settings value", columnDefinition = "TEXT")
    var value: String,

    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false, comment = "Date the account was created (UTC)")
    @param:Schema(readOnly = true)
    @kotlinx.serialization.Transient
    var created: Instant? = null,

    @Column(name = "updated", nullable = false, updatable = true, comment = "Date the account was last updated (UTC)")
    @param:Schema(readOnly = true)
    @kotlinx.serialization.Transient
    @UpdateTimestamp
    var updated: Instant? = null,
)

/**
 * A key in the [OrganisationSetting] table, together with its value type,
 * default and stored-text conversion.
 *
 * The key is invariant because settings are both read and written: [decode]
 * returns [T] and [encode] accepts [T].
 */
@Serializable(with = OrganisationSettingKeySerializer::class)
sealed class OrganisationSettingKey<T : Any>(override val code: String, override val default: T) : SettingKey<T> {
    /**
     * Decode the stored text representation.
     */
    abstract override fun decode(value: String): T

    /**
     * Encode the value to its stored text representation.
     */
    abstract override fun encode(value: T): String

    /**
     * Defines the calendar.
     */
    data object Country : OrganisationSettingKey<String>("country", "HK") {
        override fun decode(value: String): String = value

        override fun encode(value: String): String = value
    }

    /**
     * Defines the default application language.
     */
    data object Language : OrganisationSettingKey<String>("language", "en") {
        override fun decode(value: String): String = value

        override fun encode(value: String): String = value
    }

    /**
     * Defines the time zone as a BCP 47 `-tz-` key, for example `utc` for UTC
     * or `gbldn` for London. The key is stored as text; translating it to a
     * time zone is a separate concern.
     */
    data object Timezone : OrganisationSettingKey<String>("timezone", "utc") {
        override fun decode(value: String): String = value

        override fun encode(value: String): String = value
    }

    /**
     * Defines whether an organisation also uses the default organisation auth
     * methods in addition to its own.
     */
    data object IncludeDefaultAuthenticationMethods :
        OrganisationSettingKey<Boolean>("includeDefaultAuthenticationMethods", true) {
        override fun decode(value: String): Boolean = value.toBooleanStrict()

        override fun encode(value: Boolean): String = value.toString()
    }

    companion object {
        /**
         * Resolve a persisted key code.
         */
        fun fromCode(code: String): OrganisationSettingKey<*> = when (code) {
            Country.code -> Country
            Language.code -> Language
            Timezone.code -> Timezone
            IncludeDefaultAuthenticationMethods.code -> IncludeDefaultAuthenticationMethods
            else -> throw IllegalArgumentException("Unknown organisation setting: $code")
        }
    }
}

/**
 * Maps an [OrganisationSettingKey] to and from its persisted code.
 */
@Converter
class OrganisationSettingKeyConverter : AttributeConverter<OrganisationSettingKey<*>, String> {
    override fun convertToDatabaseColumn(attribute: OrganisationSettingKey<*>?): String? = attribute?.code

    override fun convertToEntityAttribute(dbData: String?): OrganisationSettingKey<*>? =
        dbData?.let { OrganisationSettingKey.fromCode(it) }
}

/**
 * Serialises an [OrganisationSettingKey] as its stable code.
 *
 * The key is a closed hierarchy, so the code is the complete wire
 * representation. The entity's JPA converter uses the same code for storage.
 */
object OrganisationSettingKeySerializer : KSerializer<OrganisationSettingKey<*>> {
    override val descriptor =
        PrimitiveSerialDescriptor("ai.hamoni.control_room.data.OrganisationSettingKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OrganisationSettingKey<*>) {
        encoder.encodeString(value.code)
    }

    override fun deserialize(decoder: Decoder): OrganisationSettingKey<*> =
        OrganisationSettingKey.fromCode(decoder.decodeString())
}

/**
 * The organisation state
 */
@Schema(enumeration = ["not-created", "not-initialised", "active", "disabled", "error"])
@Serializable
enum class OrganisationState {
    /**
     * Organisation has not been created.
     */
    @SerialName("not-created")
    NOT_CREATED,

    /**
     * Organisation has not been initialised.
     */
    @SerialName("not-initialised")
    NOT_INITIALISED,

    /**
     * Organisation has been and appears in good order.
     */
    @SerialName("active")
    ACTIVE,

    /**
     * Organisation has been administratively disabled.
     */
    @SerialName("disabled")
    DISABLED,

    /**
     * Organisation is in error (corrupted data).
     */
    @SerialName("error")
    ERROR,
}
