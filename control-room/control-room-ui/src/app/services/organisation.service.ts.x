import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, take, throwError, Observable } from 'rxjs';
import { AppError } from '@/app.exceptions';
import { LoggingService, getProblemDetailsFromHttpError } from '@/app/services/logging.service';
import { environment } from '@/environments/environment';

/**
 * The representation of an organisation
 */
export interface Organisation {
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
 * Service for organisation management.
 */
@Injectable({
    providedIn: 'root'
})
export class OrganisationService {
    readonly #apiBase = environment.apiBase + '/organisation';
    readonly #http = inject(HttpClient);
    readonly #logging = inject(LoggingService);

    // Http resources

    // State data (returns signals)

    // Actions: Named as <verb>(<noun>)?. returns observables.
    /**
     * Get a list of organisations that the current user can access. The returned list is valid a the time of call.
     * @returns an observable which contains the list of organisations. The list may be empty if no organisations are available.
     */
    list(): Observable<Organisation[]> {
        return this.#http.get<Organisation[]>(`${this.#apiBase}`)
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    const problemDetails = getProblemDetailsFromHttpError(error, "Unable to list organisations");
                    this.#logging.apiCallFailed(problemDetails);
                    return throwError(() => new AppError(problemDetails.title, problemDetails.detail, { cause: error }));
                }),
                take(1)
            );
    }

    /**
     * select to an organisation. This updates teh server side session to use the selected organisation. This is a stateful operation and so it is not idempotent.
     * @param newOrganisationId The identifier of the organisation to select.
     * @returns void, but will throw an error if the organisation cannot be selected.
     */
    select(organisationId: string): Observable<void> {
        return this.#http.post<void>(`${this.#apiBase}/select`, { id: organisationId })
            .pipe(
                catchError((error: HttpErrorResponse) => {
                    const problemDetails = getProblemDetailsFromHttpError(error, "Unable to select organisation");
                    this.#logging.apiCallFailed(problemDetails);
                    return throwError(() => new AppError(problemDetails.title, problemDetails.detail, { cause: error }));
                }),
                take(1),
                map(() => {})
            );
    }
}