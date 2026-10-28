import { inject } from '@angular/core';
import { Routes, Router } from '@angular/router';
import { AppLayout } from './app/layout/component/app.layout';
import { CardPageLayout } from './app/layout/component/card-page.layout';
import { authenticatedGuard, notAuthenticatedGuard } from './app/guards/authentication.guard';
import { nonProductionGuard } from './app/guards/non-production.guard';
import { ErrorComponent } from './app/pages/error/error'
import { createUnknownProblemDetails, createNotFoundProblemDetails } from '@/app/services/logging.service';


export const routes: Routes = [
    // Error pages go first to not trigger guards
    {
        path: 'error',
        component: CardPageLayout,
        data: { wide: true },
        children: [
            { path: '', component: ErrorComponent, data: createUnknownProblemDetails() },
        ]
    },

    // Login component got next as it's also not authenticated. Will only trigger here is specifcally called
    // The implication is that even if logged in and making a request to the login page, it will send the user
    // down the login path. This is okay.
    {
        path: 'login',
        component: CardPageLayout,
        data: { wide: false },
        children: [
            { path: '', pathMatch: 'full', loadComponent: () => import('./app/pages/auth/login').then(c => c.LoginComponent) },
        ]
    },

    // // TODO: authenticated guard only returns true or false, not redirects.
    // // a new notAuthenticatedGuard protects the login page

    // // Main app pages - all protected by a guard.
    // {
    //     path: '',
    //     component: AppLayout,
    //     children: [
    //         { path: '', pathMatch: 'full', loadComponent: () => import('./app/pages/home/home').then(c => c.HomeComponent) },
    //         { path: 'components', pathMatch: 'full', loadComponent: () => import('./app/pages/home/components').then(c => c.ComponentsComponent), canActivate: [nonProductionGuard] },
    //         //{ path: 'configuration', pathMatch: 'full', loadComponent: () => import('./app/pages/configuration/configuration-versions').then(c => c.ConfigurationVersions) },
    //         //{ path: 'configuration/editor', pathMatch: 'full', loadComponent: () => import('./app/pages/configuration/configuration-editor').then(c => c.Configuration) },
    //     ],
    //     canActivate: [authenticatedGuard]
    // },

    // // Catch any request that is specifically not logged in, and redirect. If actually a 404, then this will skip
    // {
    //     path: '**',
    //     redirectTo: (routeData) => {
    //         const router = inject(Router);
    //         return router.createUrlTree(['/login'], {
    //             queryParams: { returnUrl: routeData.url }
    //         });
    //     },
    //     canActivate: [notAuthenticatedGuard]
    // },

    // Catch-all, would implicitly require being logged in.
    {
        path: '**',
        component: CardPageLayout,
        data: { wide: true },
        children: [
            { path: '', loadComponent: () => import('./app/pages/error/error').then(c => c.ErrorComponent), data: createNotFoundProblemDetails() },
        ]
    }
];
