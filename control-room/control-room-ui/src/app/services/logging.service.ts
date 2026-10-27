import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { LumberjackLogger, LumberjackLogPayload } from '@ngworker/lumberjack';

/**
 * An RFC9457 style message
 */
export interface ProblemMessage extends LumberjackLogPayload {
    readonly type?: string;
    readonly title: string;
    readonly status?: number;
    readonly detail: string;
    readonly instance?: string;
    readonly requestId?: string;
}

const UNKNOWN_ERROR_TITLE = 'Unknown error';
const UNKNOWN_ERROR_DETAIL = 'An unknown error has occured. Please try logging in again.';
const NOTFOUND_ERROR_TITLE = 'Page not found';
const NOTFOUND_ERROR_DETAIL = 'The requested page was not found.';
const UNAUTHORISED_ERROR_TITLE = 'Page not found';
const UNAUTHORISED_ERROR_DETAIL = 'The requested page was not found.';
const BACKENDNOTREADY_ERROR_TITLE = 'Application not ready';
const BACKENDNOTREADY_ERROR_DETAIL = 'An invalid response was recieved from the backend.';

/**
 * Create a problem message from an http error.
 * @param error The http error object
 * @returns A parsed RFC9457 problem message
 */
export function getProblemDetailsFromHttpError(error: HttpErrorResponse, defaultTitle?: string): ProblemMessage {
    if (error.error && typeof error.error === 'object' && 'detail' in error.error) {
        return {
            type: error.error.type || 'about:blank',
            title: defaultTitle || error.error.title || UNKNOWN_ERROR_TITLE,
            status: error.error.status || undefined,
            detail: error.error.detail || undefined,
            instance: error.error.instance || undefined,
            requestId: error.error.requestId || (error.headers.has("Request-Id") ? (error.headers.get("Request-Id") as string) : undefined)
        };
    } else if (error.message) {
        return {
            type: 'about:blank',
            title: defaultTitle || UNKNOWN_ERROR_TITLE,
            status: error.status,
            detail: error.message,
            requestId: error.headers.has("Request-Id") ? (error.headers.get("Request-Id") as string) : undefined
        };
    } else {
        return {
            type: 'about:blank',
            title: defaultTitle || UNKNOWN_ERROR_TITLE,
            status: error.status,
            detail: UNKNOWN_ERROR_DETAIL,
            requestId: error.headers.has("Request-Id") ? (error.headers.get("Request-Id") as string) : undefined
        };
    }
}

export function createGenericProblemDetails(status: number, title: string, detail: string, type = "about:blank", requestId?: string): ProblemMessage {
    return {
        type: type,
        title: title,
        status: status,
        detail: detail,
        requestId: requestId
    };
}

export function createUnknownProblemDetails(): ProblemMessage {
    return createGenericProblemDetails(HttpStatusCode.InternalServerError, UNKNOWN_ERROR_TITLE, UNKNOWN_ERROR_DETAIL);
}

export function createNotFoundProblemDetails(): ProblemMessage {
    return createGenericProblemDetails(HttpStatusCode.NotFound, NOTFOUND_ERROR_TITLE, NOTFOUND_ERROR_DETAIL);
}

export function createUnauthorisedProblemDetails(): ProblemMessage {
    return createGenericProblemDetails(HttpStatusCode.Forbidden, UNAUTHORISED_ERROR_TITLE, UNAUTHORISED_ERROR_DETAIL);
}

export function createBackendNotReadyProblemDetails(): ProblemMessage {
    return createGenericProblemDetails(HttpStatusCode.InternalServerError, BACKENDNOTREADY_ERROR_TITLE, BACKENDNOTREADY_ERROR_DETAIL);
}


/**
 * Logger creation
 */
@Injectable({
    providedIn: 'root'
})
export class LoggingService extends LumberjackLogger<ProblemMessage> {
    static scope = 'Hamoni Control Room';

    // API loggers
    apiCallFailed = this.createWarningLogger('API call failed')
        .withScope(LoggingService.scope)
        .build();

    // Startup loggers
    userIdentityFailed = this.createErrorLogger('Failed to get the current user identity')
        .withScope(LoggingService.scope)
        .build();
        
    subscriptionSyncEntitlementsFailed = this.createErrorLogger('Subscription SyncEntitlements update failed')
        .withScope(LoggingService.scope)
        .build();
}