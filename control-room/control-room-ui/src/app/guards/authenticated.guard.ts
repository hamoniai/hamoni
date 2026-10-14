import { inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, filter, map, take, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { AuthService } from '@/app/services/auth.service';
import { LoggingService } from '@/app/services/logging.service';
import { environment } from '@/environments/environment.staging';

/**
 * Guards access to authenticated pages.
 */
export const authenticatedGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const logging = inject(LoggingService);

  // The service will return a user or an error. This means null values are not ready.
  // Map it to a boolean for the guard.
  return toObservable(authService.userResource.value).pipe(
    filter((data): data is NonNullable<typeof data> => data !== undefined && data !== null),
    catchError((error) => {
      logging.userIdentityFailed({ rootCause: error.message });
      return of(false); 
    }),
    take(1),
    map(value => (!!value) || router.createUrlTree([environment.errorPage]))
  );
};
