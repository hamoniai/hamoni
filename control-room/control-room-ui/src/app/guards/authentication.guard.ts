import { HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CanActivateFn, RedirectCommand, Router } from '@angular/router';
import { filter, map, take } from 'rxjs/operators';
import { AuthService } from '@/app/services/auth.service';
import { createBackendNotReadyProblemDetails } from '@/app/services/logging.service';
import { environment } from '@/environments/environment';

/**
 * Guards authenticated. If the return from the service is anything but a 200 or 401
 * redirect to the error page as the backend is in a bad state.
 */
export const authenticatedGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // The service will return a user or 401. 401 implies unauthenticated. Anything else is a true error.
  return toObservable(authService.userResourceStatus).pipe(
    filter((data): data is NonNullable<typeof data> => data !== undefined && data !== null),
    take(1),
    map(value => {
      switch (value) {
        case HttpStatusCode.Ok:
          return true;
        case HttpStatusCode.Unauthorized:
          return false;
        default:
          return new RedirectCommand(router.createUrlTree([environment.errorPage]), { skipLocationChange: true, state: createBackendNotReadyProblemDetails() })
      }
    })
  );
};

/**
 * Guards non-authenticated. If the return from the service is anything but a 200 or 401
 * redirect to the error page as the backend is in a bad state.
 */
export const notAuthenticatedGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // The service will return a user or 401. 401 implies unauthenticated. Anything else is a true error.
  return toObservable(authService.userResourceStatus).pipe(
    filter((data): data is NonNullable<typeof data> => data !== undefined && data !== null),
    take(1),
    map(value => {
      switch (value) {
        case HttpStatusCode.Unauthorized:
          return true;
        case HttpStatusCode.Ok:
          return false;
        default:
          return new RedirectCommand(router.createUrlTree([environment.errorPage]), { skipLocationChange: true, state: createBackendNotReadyProblemDetails() })
      }
    })
  );
};
