import { Routes } from '@angular/router';
import { AppLayout } from './app/layout/component/app.layout';
import { authenticatedGuard } from './app/guards/authenticated.guard';
import { nonProductionGuard } from './app/guards/non-production.guard';
import { ErrorComponent } from './app/pages/error/error'
import { createUnknownProblemDetails, createNotFoundProblemDetails } from '@/app/services/logging.service';


export const routes: Routes = [
    // Main app pages
    {
        // FIXME: because this has an auteenticated guard and '' matches the url, then it returns a redirect to the error page. It still should show unauthenticated.
        // However, it should actually be authenticated....
        // actually it's worse. It should redirect to eh lgoin page, which would be not found, however, it should also be logged in. Debug the guard.
        path: '',
        component: AppLayout,
        children: [
            { path: '', pathMatch: 'full', loadComponent: () => import('./app/pages/home/home').then(c => c.HomeComponent) },
            { path: 'components', pathMatch: 'full', loadComponent: () => import('./app/pages/home/components').then(c => c.ComponentsComponent), canActivate: [nonProductionGuard] },
            //{ path: 'configuration', pathMatch: 'full', loadComponent: () => import('./app/pages/configuration/configuration-versions').then(c => c.ConfigurationVersions) },
            //{ path: 'configuration/editor', pathMatch: 'full', loadComponent: () => import('./app/pages/configuration/configuration-editor').then(c => c.Configuration) },
        ],
        canActivate: [authenticatedGuard]
    },
    // The following pages utilise login style view
    // TODO
    /*{
        path: 'login',
        component: OnboardingLayout,
        children: [
            { path: '', pathMatch: 'full', loadComponent: () => import('./app/pages/login/login').then(c => c.LoginComponent) },
        ]
    },*/
    { path: 'error', component: ErrorComponent, data: createUnknownProblemDetails() },
    { path: '**', component: ErrorComponent, data: createNotFoundProblemDetails() }
];
