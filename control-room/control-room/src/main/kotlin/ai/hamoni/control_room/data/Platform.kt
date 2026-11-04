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

package ai.hamoni.control_room.data

import ai.hamoni.common.core.PLATFORM_SCHEMA
import ai.hamoni.common.platform.serialization.UUIDSerializer
import jakarta.persistence.*
import kotlinx.serialization.Serializable
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.*


/*
 * All frontend specific entities and data types (including any projections) for the platform schema go in here.
 * Entities and data types that are common to the frontend and backend should go into one of the common projects.
 */

/**
 * Entity for an authentication profile. In this table we keep an active/enabled status, however,
 * this is for globally blocking an authentication profile. This is because we always need to keep the
 * authentication profile header, for data integrity as there are links from the organisation schema tables.
 * The active flags allow us to clear the PII, but retain the database integrity.
 *
 * This is a special case because the hashCode and equals methods check for semantic equivalence rather than the
 * general object equivalence. This is because the issuer/issuerSubject act like a foreign key to and external
 * system.
 *
 * scope: global
 */
@Serializable
@Entity
@Table(name = "authnprofile", comment = "Authentication profiles", schema = PLATFORM_SCHEMA, indexes = [
    Index(name = "idx_authnprofile_id_issuer_issuersubject", columnList = "id, issuer, issuerSubject", unique = false),
    Index(name = "idx_authnprofile_issuer_issuersubject", columnList = "issuer, issuerSubject", unique = false),
])
@NamedQueries()
class AuthenticationProfile (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Serializable(with = UUIDSerializer::class)
    var id: UUID? = null,

    @Column(name = "family_name", nullable = false, updatable = true, comment = "Family name")
    var familyName: String,

    @Column(name = "given_name", nullable = false, updatable = true, comment = "Given name")
    var givenName: String,

    @Column(name = "name", nullable = false, updatable = true, comment = "Display name")
    var name: String,

    @Column(name = "email", nullable = false, updatable = true, unique = false, comment = "Email address")
    var email: String,

    @Column(name = "issuer", nullable = false, updatable = false, unique = false, comment = "OIDC issuer")
    var issuer: String,

    @Column(name = "issuerSubject", nullable = false, updatable = false, unique = false, comment = "OIDC issuer subject")
    var issuerSubject: String,

    @Column(name = "enabled", nullable = false, updatable = true, unique = false, comment = "Enabled status (for banning users)")
    var enabled: Boolean,

    @kotlinx.serialization.Transient
    @Column(name = "created", nullable = false, updatable = false, comment = "Date the account was created (UTC)")
    @CreationTimestamp
    var created: Instant? = null,

    @kotlinx.serialization.Transient
    @Column(name = "updated", nullable = false, updatable = false, comment = "Date the account was last updated (UTC)")
    var updated: Instant? = null,
) {

    @ElementCollection
    @CollectionTable(
        name = "authnprofile_organisations",
        schema = PLATFORM_SCHEMA,
        joinColumns = [JoinColumn(name = "authnprofile_id")],
        foreignKey = ForeignKey(name = "fk_authnprofile_organisations")
    )
    var organisations: List<AuthenticationProfileOrganisation>? = null

    /**
     * the initials. Not stored as it should update with the name.
     * TODO: this needs fixing, but needs some serious thought. We do not know the origin of the name, i.e. CJK,
     * Vietnamese/Thai etc. Each locale has a different way of abbreviating. Cannot use the locale settings because
     * they are company wide, and do not dictate the user's name origin. Could do something like detecting CJK
     * characters and having an assumption for those. Use standard roman as a fallback. East Asian names are still a
     * problem.
     */
    @Transient
    var initials: String = "XX"

    override fun hashCode(): Int = Objects.hash(issuer, issuerSubject)
    override fun equals(other: Any?): Boolean = other is AuthenticationProfile && issuer == other.issuer && issuerSubject == other.issuerSubject
}

/**
 * Projection of authentication profile.
 *
 */
@Serializable
@JvmRecord
data class AuthenticationProfileHeader(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val name: String,
    val email: String
) {
    constructor(c: AuthenticationProfile) : this(c.id!!, c.name, c.email)
}

/**
 * Entity for authentication profile organisations. These are the organisations that an authentication profile has
 * access to. It is updated behind the scenes by invites and authentication profile blocking. Note this is different to
 * the main organisations object which is used for managing tenants. This is an embedded collection and so prefixed
 * with the main table.
 *
 * The name is not stored. On retrieval, the organisation access is checked and if access is granted, the organisation
 * name is inserted. This is because the organisation name can change at any time.
 *
 * scope: global
 *
 */
@Serializable
@Embeddable
class AuthenticationProfileOrganisation (
    @Column(name = "id", nullable = false, updatable = false, comment = "The organisation identifier")
    @Serializable(with = UUIDSerializer::class)
    var id: UUID,

    @Transient
    var name: String
) {
    override fun hashCode(): Int = Objects.hash(id)
    override fun equals(other: Any?): Boolean = other is AuthenticationProfileOrganisation && id == other.id
}


/**
 * The organisation identity provider index. This is because we need to backtrack from an identity provider
 * identifier to the organisation identity providers for full configuration. This has a by-product of making
 * identity provider ids unique across all organisations.
 */
@Serializable
@Entity
@Table(name = "idpindex", comment = "Organisation Identity Provider index.", schema = PLATFORM_SCHEMA,
    indexes = [],
    uniqueConstraints = [
        UniqueConstraint("uk_idpindex_identity_provider_id_organisation_id", ["identity_provider_id", "organisation_id"])
    ]
)
@NamedQueries()
class IdentityProviderIndex (
    /**
     * This will become the organisation identity provider id.
     */
    @Id
    @Column(name = "identity_provider_id", updatable = false, nullable = false, unique = true, comment="Identity Provider identifier")
    @GeneratedValue(strategy = GenerationType.UUID)
    @Serializable(with = UUIDSerializer::class)
    var identityProviderId: UUID? = null,

    @Column(name = "organisation_id", nullable = false, updatable = false, unique = false, comment = "Organisation identifier")
    @Serializable(with = UUIDSerializer::class)
    var organisationId: UUID
) {
    override fun hashCode(): Int = Objects.hash(identityProviderId)
    override fun equals(other: Any?): Boolean = other is IdentityProviderIndex && identityProviderId == other.identityProviderId
}