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
 * redirect to the error page as the backend is in a bad state. If it gets a 401, then
 * to avoid the route navigation being cancelled, we redirect to the login page. I.e. this
 * page exists, but you do not have permission for it yet.
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
          return new RedirectCommand(router.createUrlTree([environment.loginPage], { queryParams: { returnUrl: state.url } }), { skipLocationChange: false })
        default:
          return new RedirectCommand(router.createUrlTree([environment.errorPage]), { skipLocationChange: true, state: createBackendNotReadyProblemDetails() })
      }
    })
  );
};
