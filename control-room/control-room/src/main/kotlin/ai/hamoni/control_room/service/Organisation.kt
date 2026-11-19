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
package ai.hamoni.control_room.service

import ai.hamoni.common.core.exception.ServiceException
import ai.hamoni.control_room.PlatformAuthoriser
import ai.hamoni.control_room.data.IdentityProviderIndex
import ai.hamoni.control_room.data.Organisation
import ai.hamoni.control_room.data.OrganisationHydration
import ai.hamoni.control_room.data.OrganisationIdentityProvider
import ai.hamoni.control_room.data.OrganisationSetting
import ai.hamoni.control_room.data.OrganisationSettingKey
import ai.hamoni.control_room.data.OrganisationState
import ai.hamoni.control_room.repository.IdentityProviderIndexRepository
import ai.hamoni.control_room.repository.OrganisationIdentityProviderRepository
import ai.hamoni.control_room.repository.OrganisationRepository
import io.quarkus.hibernate.orm.PersistenceUnitExtension
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver
import io.quarkus.hibernate.reactive.panache.kotlin.Panache
import io.quarkus.runtime.annotations.RegisterForReflection
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant
import java.util.UUID

/*
 * All frontend organisation specific services go in here. Services that are common to the frontend and backend
 * should go into one of the common projects.
 *
 * In all services, the authorisation should be checked before making data calls/accessing cache objects. The
 * authorisation itself uses a cache because it is accessed very frequently.
 *
 * Every method in this file must check the authorisation before proceeding.
 */

/**
 * Service for organisation data.
 *
 * This service purposely does not have a list()/search() function as it would potentially be a large security hole.
 * The external provisioning system (for the provision API) knows the state of organisations. This also does not make
 * sense because there will only ever be one organisation record in the organisation table. The organisation
 * routing is done by Hibernate's tenant separation.
 *
 */
@ApplicationScoped
@RegisterForReflection
class OrganisationService
@Inject
constructor(
    private val organisationRepository: OrganisationRepository,
    private val organisationIdentityProviderRepository: OrganisationIdentityProviderRepository,
    private val identityProviderIndexRepository: IdentityProviderIndexRepository,
    private val platformAuthoriser: PlatformAuthoriser,
    @param:PersistenceUnitExtension private val tenantResolver: TenantResolver,
) {
    /**
     * Add Identity provider.
     *
     * This requires the organisation admin permission (the organisation owner or a
     * person delegated that access), a platform admin, or a system security context.
     *
     * @return the organisation authentication configuration
     */
    fun addIdentityProvider(identityProvider: OrganisationIdentityProvider): Uni<OrganisationIdentityProvider> =
        Panache.withTransaction {
            platformAuthoriser.checkOrganisationAdminOrPlatformAdminOrSystem()
                .onItem().transformToUni {
                    identityProviderIndexRepository
                        .persistAndFlush(
                            IdentityProviderIndex(
                                organisationId = UUID.fromString(tenantResolver.resolveTenantId()),
                            ),
                        )
                }
                .onItem().transformToUni { index ->
                    identityProvider.id =
                        index.identityProviderId
                            ?: return@transformToUni Uni.createFrom().failure(
                                ServiceException(
                                    ::addIdentityProvider,
                                    "identity provider index id was not generated",
                                ),
                            )

                    organisationIdentityProviderRepository.persistAndFlush(identityProvider)
                }
        }

    /**
     * Create an organisation. This requires the organisation schema and tables to already exist. A provisioning
     * service must perform that. This will only populate the schema. It will not check the state of the schema,
     * that is the caller's responsibility
     *
     * DO NOT pass an organisation identifier to this function
     *
     * This requires a platform admin role, or a system security context.
     *
     * @param organisation The organisation representation
     * @return A Uni with the updated organisation, i.e. identifiers populated.
     */
    fun create(organisation: Organisation): Uni<Organisation> =
        Panache.withTransaction {
            platformAuthoriser.checkPlatformAdminOrSystem()
                .onItem().transformToUni {
                    organisation.id = UUID.fromString(tenantResolver.resolveTenantId())

                    // Hibernate's timestamp generation does not run for an
                    // @Embeddable inside an @ElementCollection, so set the
                    // organisation and setting timestamps explicitly.
                    val now = Instant.now()
                    organisation.created = now
                    organisation.updated = now
                    organisation.settings.forEach { setting ->
                        setting.created = now
                        setting.updated = now
                    }

                    if (organisation.identityProviders.isNotEmpty()) {
                        Multi.createFrom().iterable(organisation.identityProviders)
                            .onItem().transformToUniAndConcatenate { idp -> addIdentityProvider(idp) }
                            .collect().asList().replaceWith(organisation)
                    } else {
                        Uni.createFrom().item(organisation)
                    }
                }
                .onItem().transformToUni {
                    organisationRepository.persistAndFlush(organisation)
                }
        }

    /**
     * Get an organisation and attach the related data the caller asked for. The organisation id comes from the header
     * there should be no reason to cross over organisations.
     *
     * @param hydrate the related data to attach to the organisation
     * @return the organisation with the requested data attached
     */
    fun get(hydrate: List<OrganisationHydration> = emptyList()): Uni<Organisation> {
        // checkAuthorisation()
        // organisationRepository.findById(id)
        TODO()
    }

    /**
     * Get an organisation's identity providers.
     *
     * @return the organisation's identity providers
     */
    fun getIdentityProviders(enabled: Boolean? = null): Uni<List<OrganisationIdentityProvider>> =
        Panache.withSession {
            platformAuthoriser.checkOrganisationAdminOrSystem()
                .onItem().transformToUni {
                    if (null != enabled) {
                        organisationIdentityProviderRepository.find("enabled", enabled)
                    } else {
                        organisationIdentityProviderRepository.findAll()
                    }
                        .list()
                }
        }

    /**
     * Get single identity provider.
     *
     * @param id The identity provider ID
     * @param enabled filter for the enabled state. If null, no filter.
     * @return a specific identity provider
     */
    fun getIdentityProvider(id: UUID, enabled: Boolean? = null): Uni<OrganisationIdentityProvider> =
        Panache.withSession {
            platformAuthoriser.checkOrganisationAdminOrSystem()
                .onItem().transformToUni {
                    if (null != enabled) {
                        organisationIdentityProviderRepository.find(
                            "id = ?1 AND enabled = ?2",
                            enabled,
                            id)
                    } else {
                        organisationIdentityProviderRepository.find(
                            "id ?1",
                            id
                        )
                    }
                        .firstResult()
                }
                .onItem().transformToUni {
                    if (null != it) {
                        Uni.createFrom().item(it)
                    } else {
                        Uni.createFrom().failure(
                            ServiceException(::getIdentityProvider, "identity provider not found")
                        )
                    }
                }
        }


    /**
     * Get a specific setting for an organisation.
     *
     * @return the setting value, or the key's default if it was not specified.
     */
    fun <T : Any> getSetting(key: OrganisationSettingKey<T>): Uni<T> =
        Panache.withSession {
            platformAuthoriser.checkOrganisationAccessOrSystem()
                .onItem().transformToUni {
                    organisationRepository
                        .find("#Organisation.getSetting", mapOf("key" to key))
                        .project(String::class.java)
                        .firstResult()
                }
                .onItem().transform {
                    it?.let(key::decode) ?: key.default
                }
        }

    /**
     * Get an organisation's settings. The values are returned in their stored
     * text form; the caller decodes them using the key.
     *
     * @return the organisation settings
     */
    fun getSettings(): Uni<List<OrganisationSetting>> =
        Panache.withSession {
            platformAuthoriser.checkOrganisationAccessOrSystem()
                .onItem().transformToUni {
                    organisationRepository
                        .find("#Organisation.getSettings")
                        .project(OrganisationSetting::class.java)
                        .list()
                }
        }

    /**
     * Get the organisation state.
     *
     * TODO: cache, this is called frequently, especially from the identity providers login page sequence, which is
     * unauthenticated, meaning, this service could be susceptible to a DoS by frequent calling, or organisation
     * enumeration.
     *
     * Should also allow organisation admin?
     *
     * @return whether the organisation table exists
     */
    fun state(): Uni<OrganisationState> =
        // To do this properly, and safely, we need to chain two queries. The first checks the organisation schema
        // exists, and the second checks the data state.
        Panache.withSession {
            platformAuthoriser.checkPlatformAdminOrSystem()
                .onItem().transformToUni { Panache.getSession() }
                .onItem().transformToUni { session ->
                    session.createNamedQuery<Boolean>("Organisation.checkOrganisationExists")
                        .setParameter("schema", tenantResolver.resolveTenantId())
                        .singleResult
                }
                .onItem().transformToUni { result ->
                    if (result) {
                        organisationRepository.find("#Organisation.isOrganisationInitialised")
                            .project(OrganisationState::class.java)
                            .singleResult()
                    } else {
                        Uni.createFrom().item(OrganisationState.NOT_CREATED)
                    }
                }
                .onFailure().transform { throwable ->
                    ServiceException(::state, "Unable to determine organisation state", throwable)
                }
        }
}
