import { TestBed } from '@angular/core/testing';
import { httpResource, HttpResourceRef } from '@angular/common/http';
import { provideLumberjack } from '@ngworker/lumberjack';
import { provideLumberjackConsoleDriver } from '@ngworker/lumberjack/console-driver';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi, type Mocked } from 'vitest';
import { AppStateService } from './app-state.service';
import { AuthService, User } from './auth.service';
import { OrganisationService } from './organisation.service';


describe('AppStateService', () => {
    let appStateService: AppStateService;
    let organisationService: OrganisationService;

    let authService: Mocked<Partial<AuthService>> = {
        userResource: undefined
    };

    // Dummy http resources
    let mockUserResource: HttpResourceRef<User | undefined>;

    beforeEach(() => {
        // Need to mock the AuthService entirely to prevent the default httpResource from loading
        TestBed.configureTestingModule({
            providers: [
                provideLumberjack(),
                provideLumberjackConsoleDriver(),
                AppStateService,
                { provide: AuthService, useValue: authService },
                OrganisationService
            ]
        });

        mockUserResource = TestBed.runInInjectionContext(() => {
            return httpResource<User | undefined>(() => undefined);
        });
        (authService as any).userResource = mockUserResource

        organisationService = TestBed.inject(OrganisationService);
        appStateService = TestBed.inject(AppStateService);
    });

    it('should load organisations only once', async () => {
        // Given
        const dataUser = {id: window.crypto.randomUUID(), name: 'Bob Smith', email: 'bob.smith@example.com'};
        const dataaOrganisations = [
            {id: window.crypto.randomUUID(), name: "Organisation 1"},
            {id: window.crypto.randomUUID(), name: "Organisation 2"},
        ];

        vi.spyOn(authService, 'userResource', 'get').mockReturnValue(mockUserResource)
        const osListSpy = vi.spyOn(organisationService, 'list').mockReturnValue(of(dataaOrganisations));

        // When
        mockUserResource.set(dataUser);

        // Need to wait because of the async nature
        await vi.waitFor(() => {
            if (!appStateService.appStateLoaded()) {
                throw new Error('Signal not ready yet');
            }
        }, { timeout: 1000, interval: 50});

        // Then
        expect(appStateService.appState().user).toStrictEqual(dataUser);
        expect(appStateService.appState().organisations).toEqual(dataaOrganisations);
        expect(osListSpy).toHaveBeenCalledTimes(1);
    });
});