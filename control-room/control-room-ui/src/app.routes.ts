import { Routes } from '@angular/router';
import { AppLayout } from './app/layout/component/app.layout';
import { authenticatedGuard } from './app/guards/authenticated.guard';
import { ErrorComponent } from './app/pages/error/error'
import { createUnknownProblemDetails, createNotFoundProblemDetails } from '@/app/services/logging.service';


export const routes: Routes = [
    // Main app pages
    {
        path: '',
        component: AppLayout,
        children: [
            { path: '', pathMatch: 'full', loadComponent: () => import('./app/pages/home/home').then(c => c.HomeComponent) },
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
