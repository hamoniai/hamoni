import { TestBed } from '@angular/core/testing';
import { httpResource, HttpResourceRef } from '@angular/common/http';
import { provideLumberjack } from '@ngworker/lumberjack';
import { provideLumberjackConsoleDriver } from '@ngworker/lumberjack/console-driver';
import { defer, of } from 'rxjs';
import { describe, it, expect, beforeEach, vi, type Mocked } from 'vitest';
import { AppStateService } from './app-state.service';
import { AuthService, User, UserOrganisation } from './auth.service';


describe('AppStateService', () => {
    // Dummy http resources
    let mockUserResource: HttpResourceRef<User | undefined>;

    // Data references
    let dataUser: User;
    let dataUserOrganisations: UserOrganisation[] = []

    // Mocks & services
    let appStateService: AppStateService;
    let authService: Mocked<Partial<AuthService>> = {
        userResource: undefined,
        getProfileOrganisations: vi.fn().mockReturnValue(defer(() => of(dataUserOrganisations)))
    };


    beforeEach(() => {
        // Need to mock the AuthService entirely to prevent the default httpResource from loading
        TestBed.configureTestingModule({
            providers: [
                provideLumberjack(),
                provideLumberjackConsoleDriver(),
                AppStateService,
                { provide: AuthService, useValue: authService },
            ]
        });

        mockUserResource = TestBed.runInInjectionContext(() => {
            return httpResource<User | undefined>(() => undefined);
        });
        (authService as any).userResource = mockUserResource

        appStateService = TestBed.inject(AppStateService);
    });

    it('should load organisations only once', async () => {
        // Given
        dataUser = {
            id: window.crypto.randomUUID(),
            name: 'Bob Smith',
            email: 'bob.smith@example.com',
            issuer: 'https://accounts.google.com',
            issuerSubject: '107654321098765432109'
        };
        dataUserOrganisations = [
            {id: window.crypto.randomUUID(), name: "Organisation 1"},
            {id: window.crypto.randomUUID(), name: "Organisation 2"},
        ];

        vi.spyOn(authService, 'userResource', 'get').mockReturnValue(mockUserResource)

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
        expect(appStateService.appState().organisations).toEqual(dataUserOrganisations);
        expect(authService.getProfileOrganisations).toHaveBeenCalledTimes(1);
    });
});