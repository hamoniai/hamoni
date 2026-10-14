import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
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

/**
 * Create a problem message from an http error.
 * @param error The http error object
 * @returns A parsed RFC9457 problem message
 */
export function getProblemDetailsFromError(error: HttpErrorResponse, defaultTitle?: string): ProblemMessage {
    error.url
    if (error.error && typeof error.error === 'object' && 'detail' in error.error) {
        return {
            type: error.error.type || 'about:blank',
            title: defaultTitle || error.error.title || "Unknown error",
            status: error.error.status || undefined,
            detail: error.error.detail || undefined,
            instance: error.error.instance || undefined,
            requestId: error.error.requestId || (error.headers.has("Request-Id") ? (error.headers.get("Request-Id") as string) : undefined)
        };
    } else if (error.message) {
        return {
            type: 'about:blank',
            title: defaultTitle || "Unknown error",
            status: error.status,
            detail: error.message,
            requestId: error.headers.has("Request-Id") ? (error.headers.get("Request-Id") as string) : undefined
        };
    } else {
        return {
            type: 'about:blank',
            title: defaultTitle || "Unknown error",
            status: error.status,
            detail: "Unknown error",
            requestId: error.headers.has("Request-Id") ? (error.headers.get("Request-Id") as string) : undefined
        };
    }
}

/**
 * Logger creation
 */
@Injectable({
    providedIn: 'root'
})
export class LoggingService extends LumberjackLogger<ProblemMessage> {
    static scope = 'Hamoni Control Room';

    // Auth loggers
    authLogoutFailed = this.createWarningLogger('Logout failed')
        .withScope(LoggingService.scope)
        .build();

    authSelectOrganisationFailed = this.createWarningLogger('Failed to select organisation')
        .withScope(LoggingService.scope)
        .build();

    // Startup loggers
    userIdentityFailed = this.createErrorLogger('Failed to get the curent user identity')
        .withScope(LoggingService.scope)
        .build();
        
    subscriptionSyncEntitlementsFailed = this.createErrorLogger('Subscription SyncEntitlements update failed')
        .withScope(LoggingService.scope)
        .build();
}