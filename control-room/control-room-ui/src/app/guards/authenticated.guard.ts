import { inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { CanActivateFn, RedirectCommand, Router } from '@angular/router';
import { catchError, filter, map, take } from 'rxjs/operators';
import { of } from 'rxjs';
import { AuthService } from '@/app/services/auth.service';
import { environment } from '@/environments/environment';

/**
 * Guards access to authenticated pages. If the guard fails, it will redirect to the login page rather than show an error.
 * This is in contrast to unauthorised guards, which will show an error message rather than redirect.
 */
export const authenticatedGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  // The service will return a user or an error. This means null values are not ready.
  // Map it to a boolean for the guard.
  return toObservable(authService.userResource.value).pipe(
    filter((data): data is NonNullable<typeof data> => data !== undefined && data !== null),
    catchError((error) => {
      return of(false); 
    }),
    take(1),
    map(value =>
      (!!value) || new RedirectCommand(router.createUrlTree([environment.loginPage]), { skipLocationChange: false })
    )
  );
};
