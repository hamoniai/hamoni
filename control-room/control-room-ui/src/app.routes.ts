import { Routes } from '@angular/router';
import { AppLayout } from './app/layout/component/app.layout';
import { authenticatedGuard } from './app/guards/authenticated.guard';
import { ErrorComponent, ErrorDetails, stateErrorDetailsKey } from './app/pages/error/error'

export const routes: Routes = [
    {
        path: '',
        component: AppLayout,
        /*children: [
            { path: '', pathMatch: 'full', loadComponent: () => import('./app/pages/dashboard/dashboard').then(c => c.Dashboard) },
            { path: 'configuration', pathMatch: 'full', loadComponent: () => import('./app/pages/configuration/configuration-versions').then(c => c.ConfigurationVersions) },
            { path: 'configuration/editor', pathMatch: 'full', loadComponent: () => import('./app/pages/configuration/configuration-editor').then(c => c.Configuration) },
        ],*/
        canActivate: [authenticatedGuard]
    },
    { path: 'error', component: ErrorComponent, data: {[stateErrorDetailsKey]: {code: 500, message: "Unknown error"} as ErrorDetails} },
    { path: '**', component: ErrorComponent, data: {[stateErrorDetailsKey]: {code: 404, message: "Page not found"} as ErrorDetails} }
];
