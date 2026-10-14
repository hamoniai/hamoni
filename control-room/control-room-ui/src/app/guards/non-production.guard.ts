import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { environment } from '@/environments/environment';

/**
 * Guards access to non-production only routes.
 */
export const nonProductionGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);

  return !environment.production || router.createUrlTree([environment.errorPage])
};
