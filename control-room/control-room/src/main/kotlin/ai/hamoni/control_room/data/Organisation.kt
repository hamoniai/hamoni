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
package ai.hamoni.control_room.data

import ai.hamoni.common.core.shared.TypedSetting
import ai.hamoni.common.core.shared.TypedSettingType
import ai.hamoni.common.platform.serialization.UUIDSerializer
import ai.hamoni.control_room.service.OrganisationService
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.NamedNativeQueries
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import jakarta.persistence.OneToMany
import jakarta.persistence.QueryHint
import jakarta.persistence.Table
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.Objects
import java.util.UUID

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
        )
    ]
)
@Serializable
@Table(name = "organisation", comment = "Organisations")
class Organisation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Serializable(with = UUIDSerializer::class)
    @Schema(readOnly = true)
    var id: UUID? = null,
    @Column(name = "name", nullable = false, updatable = true, comment = "Organisation name")
    var name: String,
    @Column(name = "enabled", nullable = false, updatable = true, unique = false, comment = "Enabled status")
    var enabled: Boolean,
    @kotlinx.serialization.Transient
    @Schema(readOnly = true)
    @Column(name = "created", nullable = false, updatable = false, comment = "Date the account was created (UTC)")
    @CreationTimestamp
    var created: Instant? = null,
    @kotlinx.serialization.Transient
    @Schema(readOnly = true)
    @Column(name = "updated", nullable = false, updatable = false, comment = "Date the account was last updated (UTC)")
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
@Serializable
@Table(name = "organisation_identity_provider", comment = "Organisation identify providers (OIDC)")
class OrganisationIdentityProvider(
    /**
     * This is not generated as the actual generated value is in the platform organisation identity provider index.
     */
    @Id
    @Serializable(with = UUIDSerializer::class)
    @Schema(readOnly = true)
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
    @Column(
        name = "client_secret_ref",
        nullable = true,
        updatable = true,
        unique = true,
        comment = "The client secret reference, the actual value is in the credential vault. Null means public client."
    )
    var clientSecretReference: String?,
    @Column(name = "enabled", nullable = false, updatable = true, unique = false, comment = "Enabled status")
    var enabled: Boolean,
    @kotlinx.serialization.Transient
    @Schema(readOnly = true)
    @Column(name = "created", nullable = false, updatable = false, comment = "Date the organisation was created (UTC)")
    @CreationTimestamp
    var created: Instant? = null,
    @kotlinx.serialization.Transient
    @Schema(readOnly = true)
    @Column(
        name = "updated",
        nullable = false,
        updatable = false,
        comment = "Date the organisation was last updated (UTC)"
    )
    var updated: Instant? = null,
) {
    override fun hashCode(): Int = Objects.hash(id)

    override fun equals(other: Any?): Boolean = other is OrganisationIdentityProvider && id == other.id
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
    ) : this(organisationIdentityProvider.id!!, organisationIdentityProvider.name, false)
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
    @Enumerated(EnumType.STRING)
    var key: OrganisationSettingKey,
    /**
     * The key for the key/value pair.
     */
    @Column(
        name = "value",
        nullable = true,
        updatable = true,
        unique = false,
        comment = "Settings value",
        columnDefinition = "TEXT"
    )
    var value: String,
    @kotlinx.serialization.Transient
    @Schema(readOnly = true)
    @Column(name = "created", nullable = false, updatable = false, comment = "Date the key/value created (UTC)")
    @CreationTimestamp
    var created: Instant? = null,
    @kotlinx.serialization.Transient
    @Schema(readOnly = true)
    @Column(name = "updated", nullable = false, updatable = false, comment = "Date the key/value last updated (UTC)")
    var updated: Instant? = null,
)

/**
 * Keys that can be stored in the [OrganisationSetting] table.
 *
 * Each key declares its value type and the value used when the organisation has
 * not stored a setting.
 */
@Schema(enumeration = ["country", "language", "timezone", "includeDefaultAuthenticationMethods"])
@Serializable
enum class OrganisationSettingKey(
    override val type: TypedSettingType<*>,
    override val default: Any,
) : TypedSetting {
    /**
     * Defines the calendar.
     */
    @SerialName("country")
    COUNTRY(TypedSettingType.StringType, "GB"),

    /**
     * Defines the default application language.
     */
    @SerialName("language")
    LANGUAGE(TypedSettingType.StringType, "en"),

    /**
     * Defines the time zone as a BCP 47 `-tz-` key, for example `utc` for UTC
     * or `gbldn` for London. The key is stored as text; translating it to a
     * time zone is a separate concern.
     */
    @SerialName("timezone")
    TIMEZONE(TypedSettingType.StringType, "utc"),

    /**
     * Defines whether an organisation also uses the default organisation auth
     * methods in addition to its own.
     */
    @SerialName("includeDefaultAuthenticationMethods")
    INCLUDE_DEFAULT_AUTHENTICATION_METHODS(TypedSettingType.BooleanType, true),
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
