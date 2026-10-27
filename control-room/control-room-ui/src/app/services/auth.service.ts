import { HttpClient, HttpErrorResponse, HttpStatusCode, httpResource } from '@angular/common/http';
import { effect, inject, Injectable } from '@angular/core';
import { catchError, of, take, tap, throwError, Observable } from 'rxjs';
import { LoggingService, getProblemDetailsFromHttpError } from '@/app/services/logging.service';
import { environment } from '@/environments/environment';
import { AppError } from '@/app.exceptions';
import { createHydrationQueryParameters } from '@/app.util';


/**
 * The representation of an organisation. This is not the full organisation details, it's just a lightweight
 * selection of pointers.
 */
export interface UserOrganisation {
    /**
     * The identifier of the organisation (UUID)
     */
    id: string;
    /**
     * The displayable name of the organisation
     */
    name: string;
}


/**
 * The representation of a user
 */
export interface User {
    /**
     * The identifier of the user in the application (UUID)
     */
    id: string;
    /**
     * The display name of the user.
     */
    name: string;
    /**
     * The email address of the user.
     */
    email: string;
    /**
     * The OIDC issuer
     */
    issuer: string;
    /**
     * The subject at the OIDC issuer
     */
    issuerSubject: string;
    /**
     * The organisations that this user has access to
     */
    organisations?: UserOrganisation[] 
}

/**
 * Service for authentication helpers. This class has slightly irregular naming as there is no "auth" object.
 */
@Injectable({
    providedIn: 'root'
})
export class AuthService {
    readonly #apiBase = environment.apiBase + '/auth';
    readonly #http = inject(HttpClient);
    readonly #logging = inject(LoggingService);

    // Http resources. For cases when the underlying resource is needed. These should be readonly.
    // Note that this will trigger under all conditions, even if accessing unauthenticated pages. That is
    // okay because this is a light service. However, it may not resolve if the user is not logged in.
    // This is special, it starts on startup, which is different to most other services, but we want this done
    // as soon as possible. Need to expose the status differently because the asReadOnly() strips the http properties.
    readonly #userResource = httpResource<User | undefined>(() => `${this.#apiBase}/profile`);
    readonly userResource = this.#userResource.asReadonly();
    readonly userResourceStatus = this.#userResource.statusCode;

    // State data (returns signals)

    constructor() {
        // Handler to catch an error from the user service and log it.
        effect(() => {
            const error = this.#userResource.error() as HttpErrorResponse;

            if (error) {
                // If the error is unauthorised. If a 401, then goto the login page. Anything else, goto the error page.
                if (error.status != HttpStatusCode.Unauthorized) {
                    const problemDetails = getProblemDetailsFromHttpError(error);
                    this.#logging.apiCallFailed(problemDetails);
                }                    
            }
        });
    }

    // Actions: Named as <verb>(<noun>)?. returns observables.
    /**
     * Get the authentication profile
     * @returns the User object
     */
    getProfile(hydrate?: string[]): Observable<User> {
        return this.#http.get<User>(`${this.#apiBase}/profile`, { params: createHydrationQueryParameters(hydrate) })
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    const problemDetails = getProblemDetailsFromHttpError(error, "Unable to get authentication profile");
                    this.#logging.apiCallFailed(problemDetails);
                    return throwError(() => new AppError(problemDetails.title, problemDetails.detail, { cause: error }));
                }),
                take(1)
            );
    }

    /**
     * Get the authentication profile organisations 
     * @returns the User object
     */
    getProfileOrganisations(): Observable<UserOrganisation[]> {
        return this.#http.get<UserOrganisation[]>(`${this.#apiBase}/profile/organisations`)
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    const problemDetails = getProblemDetailsFromHttpError(error, "Unable to get authentication profile organisations");
                    this.#logging.apiCallFailed(problemDetails);
                    return throwError(() => new AppError(problemDetails.title, problemDetails.detail, { cause: error }));
                }),
                take(1)
            );
    }

    /**
     * @returns an observable which is true if the logout request completed, undefined otherwise.
     */
    doLogout(): Observable<boolean> {
        return this.#http.get<boolean>(`${this.#apiBase}/logout`)
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    const problemDetails = getProblemDetailsFromHttpError(error, "Unable to logout");
                    this.#logging.apiCallFailed(problemDetails);
                    return of(true);
                }),
                tap(() => {
                    this.#userResource.set(undefined);
                }),
                take(1)
            );
    }

    /**
     * Accessor method, we don't want to expose the underlying resource directly, as it is not readonly.
     */
    reloadUser(): void {
        this.#userResource.reload();
    }
}