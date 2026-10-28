import { HttpStatusCode } from '@angular/common/http';
import { signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { httpResource, HttpResourceRef } from '@angular/common/http';
import { provideLumberjack } from '@ngworker/lumberjack';
import { provideLumberjackConsoleDriver } from '@ngworker/lumberjack/console-driver';
import { defer, of } from 'rxjs';
import { describe, it, expect, beforeEach, vi, type Mocked } from 'vitest';
import { AppStateService } from './app-state.service';
import { AuthService, User, UserOrganisation } from './auth.service';
import { provideTranslocoTesting } from '@/app/testing/transloco-testing';

describe('AppStateService', () => {
  // Dummy http resources
  let mockUserResource: HttpResourceRef<User | undefined>;

  // Data references
  let dataUser: User;
  let dataUserOrganisations: UserOrganisation[] = [];

  // Mocks & services
  let appStateService: AppStateService;
  const authService: Mocked<Partial<AuthService>> = {
    userResource: undefined,
    userResourceStatus: signal(undefined),
    getProfileOrganisations: vi.fn().mockReturnValue(defer(() => of(dataUserOrganisations))),
  };

  beforeEach(() => {
    // Keep each test independent of any preference left behind by another test.
    localStorage.clear();

    // Need to mock the AuthService entirely to prevent the default httpResource from loading
    TestBed.configureTestingModule({
      imports: [provideTranslocoTesting()],
      providers: [
        provideLumberjack(),
        provideLumberjackConsoleDriver(),
        AppStateService,
        { provide: AuthService, useValue: authService },
      ],
    });

    mockUserResource = TestBed.runInInjectionContext(() => {
      return httpResource<User | undefined>(() => undefined);
    });
    (authService as unknown as { userResource: HttpResourceRef<User | undefined> }).userResource = mockUserResource;

    appStateService = TestBed.inject(AppStateService);
  });

  /**
   * Set the http resource mock data
   * @param status The http status
   * @param user The user object to set (or undefined)
   */
  function setResourceData(status: number, user: User | undefined) {
    mockUserResource.set(user);
    (authService.userResourceStatus as WritableSignal<number | undefined>).set(status);
  }

  // test cases
  it('should start with advanced mode off by default', () => {
    // Given - a fresh application state with no stored preference

    // When - the service is created by the setup

    // Then
    expect(appStateService.appStateAdvancedMode()).toBe(false);
  });

  it('should set advanced mode and persist it', () => {
    // Given - advanced mode is off
    expect(appStateService.appStateAdvancedMode()).toBe(false);

    // When
    appStateService.setAdvancedMode(true);
    TestBed.tick();

    // Then
    expect(appStateService.appStateAdvancedMode()).toBe(true);
    expect(localStorage.getItem('app.advanced-mode')).toBe('true');
  });

  it('should toggle advanced mode on and off', () => {
    // Given - advanced mode is off

    // When - it is toggled on
    appStateService.toggleAdvancedMode();

    // Then
    expect(appStateService.appStateAdvancedMode()).toBe(true);

    // When - it is toggled off again
    appStateService.toggleAdvancedMode();

    // Then
    expect(appStateService.appStateAdvancedMode()).toBe(false);
  });

  it('should restore advanced mode from local storage', () => {
    // Given - a stored preference for advanced mode
    localStorage.setItem('app.advanced-mode', 'true');

    // When - a new application state is created
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [provideTranslocoTesting()],
      providers: [
        provideLumberjack(),
        provideLumberjackConsoleDriver(),
        AppStateService,
        { provide: AuthService, useValue: authService },
      ],
    });
    const restoredUserResource = TestBed.runInInjectionContext(() =>
      httpResource<User | undefined>(() => undefined),
    );
    (authService as unknown as { userResource: HttpResourceRef<User | undefined> }).userResource =
      restoredUserResource;
    const restoredService = TestBed.inject(AppStateService);

    // Then
    expect(restoredService.appStateAdvancedMode()).toBe(true);
  });

  it('should load organisations only once', async () => {
    // Given
    dataUser = {
      id: window.crypto.randomUUID(),
      name: 'Bob Smith',
      initials: 'BS',
      email: 'bob.smith@example.com',
      issuer: 'https://accounts.google.com',
      issuerSubject: '107654321098765432109',
    };
    dataUserOrganisations = [
      { id: window.crypto.randomUUID(), name: 'Organisation 1' },
      { id: window.crypto.randomUUID(), name: 'Organisation 2' },
    ];

    vi.spyOn(authService, 'userResource', 'get').mockReturnValue(mockUserResource);

    // When
    setResourceData(HttpStatusCode.Ok, dataUser);

    // Need to wait because of the async nature
    await vi.waitFor(
      () => {
        if (!appStateService.appStateLoaded()) {
          throw new Error('Signal not ready yet');
        }
      },
      { timeout: 1000, interval: 50 },
    );

    // Then
    expect(appStateService.appState().user).toStrictEqual(dataUser);
    expect(appStateService.appState().organisations).toEqual(dataUserOrganisations);
    expect(authService.getProfileOrganisations).toHaveBeenCalledTimes(1);
  });
});
