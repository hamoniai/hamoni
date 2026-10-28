package ai.hamoni.control_room.data

import ai.hamoni.common.platform.GLOBAL_SCHEMA
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.*


/*
 * All frontend specific entities and data types (including any projections) go in here. Entities and data types
 * that are common to the frontend and backend should go into one of the common projects.
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
@Table(name = "authnprofile", comment = "Authentication profiles", schema = GLOBAL_SCHEMA, indexes = [
    Index(name = "idx_authnprofile_id_issuer_issuersubject", columnList = "id, issuer, issuerSubject", unique = false),
    Index(name = "idx_authnprofile_issuer_issuersubject", columnList = "issuer, issuerSubject", unique = false),
])
@Entity
@NamedQueries()
class AuthenticationProfile (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @Column(name = "enabled", nullable = false, updatable = true, unique = false, comment = "Enabled status")
    var enabled: String,

    @kotlinx.serialization.Transient
    @Column(name = "created", nullable = false, updatable = false, comment = "Date the account was created (UTC)")
    @CreationTimestamp
    var created: Instant? = null,

    @kotlinx.serialization.Transient
    @Column(name = "updated", nullable = false, updatable = false, comment = "Date the account was last updated (UTC)")
    var updated: Instant? = null,
) {

    @ElementCollection
    @CollectionTable(name = "authnprofile_organisations", schema = GLOBAL_SCHEMA, joinColumns = [JoinColumn(name = "authnprofile_id")])
    var organisations: List<AuthenticationProfileOrganisation> = emptyList()

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
@JvmRecord
data class AuthenticationProfileHeader(val id: UUID, val name: String, val email: String) {
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
@Embeddable
class AuthenticationProfileOrganisation (
    @Column(name = "id", nullable = false, updatable = false, comment = "The organisation identifier")
    var id: UUID,

    @Transient
    var name: String
) {
    override fun hashCode(): Int = Objects.hash(id)
    override fun equals(other: Any?): Boolean = other is AuthenticationProfileOrganisation && id == other.id
}

/**
 * Projection of AuthenticationProfileOrganisation (not used, but kept for consistency).
 */
@JvmRecord
data class AuthenticationProfileOrganisationHeader(val id: UUID, val name: String) {
    constructor(p: AuthenticationProfileOrganisation) : this(p.id, p.name)
}
