import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withDisabledInitialNavigation, withInMemoryScrolling } from '@angular/router';
import { provideLumberjack } from '@ngworker/lumberjack';
import { providePrimeNG } from 'primeng/config';
import { definePreset } from '@primeuix/themes';
import Nora from '@primeuix/themes/nora';
import { provideLumberjackConsoleDriver } from '@ngworker/lumberjack/console-driver';

import { routes } from './app.routes';

// Define the app theme.
// https://sasharedtest.z6.web.core.windows.net/theming
// https://primeng.org/theming/styled#definepreset
const Hamoni = definePreset(Nora, {
  primitive: {
  },
  semantic: {
    primary: {
      50: '{purple.50}',
      100: '{purple.100}',
      200: '{purple.200}',
      300: '{purple.300}',
      400: '{purple.400}',
      500: '{purple.500}',
      600: '{purple.600}',
      700: '{purple.700}',
      800: '{purple.800}',
      900: '{purple.900}',
      950: '{purple.950}'
    },
    colorScheme: {
      light: {
        surface: {
          0: '#ffffff',
          50: '{zinc.50}',
          100: '{zinc.100}',
          200: '{zinc.200}',
          300: '{zinc.300}',
          400: '{zinc.400}',
          500: '{zinc.500}',
          600: '{zinc.600}',
          700: '{zinc.700}',
          800: '{zinc.800}',
          900: '{zinc.900}',
          950: '{zinc.950}'
        }
      },
      dark: {
        surface: {
          0: '#ffffff',
          50: '{zinc.50}',
          100: '{zinc.100}',
          200: '{zinc.200}',
          300: '{zinc.300}',
          400: '{zinc.400}',
          500: '{zinc.500}',
          600: '{zinc.600}',
          700: '{zinc.700}',
          800: '{zinc.800}',
          900: '{zinc.900}',
          950: '{zinc.950}'
        }
      }
    },
    content: {
      borderRadius: '{border.radius.md}'
    }
  }
});


export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(
      withFetch(),
      withInterceptors([
        (req, next) => next(req.clone({ withCredentials: true })) // Auto attach the cookie to the request 
      ])
    ),
    provideLumberjack(),
    provideLumberjackConsoleDriver(),
    provideRouter(routes,
      withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' }),
      withDisabledInitialNavigation()
    ),
    providePrimeNG({
      theme: {
        preset: Hamoni,
        options: { darkModeSelector: '.app-dark' }
      }
    })
  ]
};
