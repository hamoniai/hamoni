import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding, withDisabledInitialNavigation, withInMemoryScrolling, withPreloading, PreloadAllModules } from '@angular/router';
import { provideLumberjack } from '@ngworker/lumberjack';
import { providePrimeNG } from 'primeng/config';
import { provideLumberjackConsoleDriver } from '@ngworker/lumberjack/console-driver';

import { routes } from './app.routes';
import { HamoniPreset } from './styles/hamoni.preset';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(
      withInterceptors([
        (req, next) => next(req.clone({ withCredentials: true })) // Auto attach the cookie to the request 
      ])
    ),
    provideLumberjack(),
    provideLumberjackConsoleDriver(),
    provideRouter(routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' }),
      withDisabledInitialNavigation(),
      withPreloading(PreloadAllModules)
    ),
    providePrimeNG({
      // This is horrible and breaks our security practices as the key gets checked in. Need to find some way to dynamically load this, or move to Optimus UI.
      license: 'eyJpZCI6IjA1ZjU0OTk3LTBhOGYtNDQ0NS1hODYwLWEwYTVjNDA4MTMwMiIsInByb2R1Y3QiOiJwcmltZXVpIiwidGllciI6ImNvbW11bml0eSIsInR5cGUiOiJkZXYiLCJpYXQiOjE3OTIwMzU2OTcsImV4cCI6MTgyMzU3MTY5N30.2nAStOW728PPy3PZrh_0yeQfDF8iaRQpl8PLWBNRMdft6xDah_dIMDzztv4HZ6nTCw9ijdyevANwcduEh5tRAw',
      theme: {
        preset: HamoniPreset,
        options: {
          darkModeSelector: '.app-dark',
          cssLayer: { name: 'primeng', order: 'theme, base, primeng, components, utilities' },
        },
      }
    })
  ]
};
