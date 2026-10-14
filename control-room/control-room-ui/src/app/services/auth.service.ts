import { HttpClient, HttpErrorResponse, httpResource } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, of, take, tap, throwError, Observable } from 'rxjs';
import { AppError } from '@/app.exceptions';
import { LoggingService, getProblemDetailsFromError } from '@/app/services/logging.service';
import { environment } from '@/environments/environment';

/**
 * The representation of an organisation
 */
export interface Organisation {
    /**
     * The identifier of the organisation
     */
    id: string;
    /**
     * The name of the organisation
     */
    name: string;
}

/**
 * The representation of a user
 */
export interface User {
    /**
     * The identifier of the user in the application (should be a UUID)
     */
    id: string;
    /**
     * The display name of the user in the applicaiton.
     */
    name: string;
    /**
     * The email address of the user in the application.
     */
    email: string;
    /**
     * The organisations a user belongs to
     */
    organisations: Organisation[];
    /**
     * The default organisation for the user (should be a uuid)
     */
    defaultOrganisationId: string;
}

/**
 * Service for determing authenticated status
 */
@Injectable({
    providedIn: 'root'
})
export class AuthService {
    readonly #apiBase = environment.apiBase + '/auth';
    readonly #http = inject(HttpClient);
    readonly #logging = inject(LoggingService);

    constructor() {
    }

    // Http resources. For cases when the underlying resource is needed. These should be readonly.
    // Note that this will trigger under all conditions, even if accessing unauthenticated pages. That is
    // okay because this is a light service. However, it may not resolve if the user is not logged in.
    readonly #userResource = httpResource<User | undefined>(() => `${this.#apiBase}/whoami`);
    readonly userResource = this.#userResource.asReadonly();

    // State data (returns signals)
    // The underlying userResource is guarded in app.component.ts, so the user property can be safely used.
    readonly user = this.#userResource.value.asReadonly();

    // Actions: Named as <verb><noun>. returns observables.
    /**
     * @returns an observable which is true if the logout request completed, undefined otherwise.
     */
    doLogout(): Observable<boolean> {
        return this.#http.get<boolean>(`${this.#apiBase}/logout`)
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    this.#logging.authLogoutFailed(getProblemDetailsFromError(error));
                    return of(true);
                }),
                tap(() => {
                    this.#userResource.set(null);
                }),
                take(1)
            );
    }

    /**
     * select to an organisation.
     */
    selectOrganisation(newOrganisationId: string): Observable<boolean> {
        return this.#http.post<boolean>(`${this.#apiBase}/selectOrganisation`, { organisationId: newOrganisationId })
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    const problemDetails = getProblemDetailsFromError(error, "Unable to select organisation");
                    this.#logging.authSelectOrganisationFailed(problemDetails);
                    return throwError(() => new AppError(problemDetails.title, problemDetails.detail, { cause: error }));
                }),
                take(1)
            );
    }
}