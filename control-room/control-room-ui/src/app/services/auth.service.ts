import { HttpClient, HttpErrorResponse, HttpStatusCode, httpResource } from '@angular/common/http';
import { effect, inject, Injectable } from '@angular/core';
import { catchError, of, take, tap, Observable } from 'rxjs';
import { LoggingService, getProblemDetailsFromHttpError } from '@/app/services/logging.service';
import { environment } from '@/environments/environment';

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

    // Http resources. For cases when the underlying resource is needed. These should be readonly.
    // Note that this will trigger under all conditions, even if accessing unauthenticated pages. That is
    // okay because this is a light service. However, it may not resolve if the user is not logged in.
    readonly #userResource = httpResource<User | undefined>(() => `${this.#apiBase}/whoami`);
    readonly userResource = this.#userResource.asReadonly();

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