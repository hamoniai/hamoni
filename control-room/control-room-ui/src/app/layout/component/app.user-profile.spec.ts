import { computed, signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AppState, AppStateService, THEME_DARK, THEME_LIGHT } from '@/app/services/app-state.service';
import { provideTranslocoTesting } from '@/app/testing/transloco-testing';
import { AppUserProfile } from './app.user-profile';

const user = {
  id: 'user-1',
  name: 'Celia Yu',
  initials: 'CY',
  email: 'celia.yu@acme.com',
  issuer: 'https://issuer.example',
  issuerSubject: 'subject-1',
};

const organisations = [
  { id: 'org-acme', name: 'Acme Corporation' },
  { id: 'org-northwind', name: 'Northwind Logistics' },
];

describe('AppUserProfile', () => {
  let appState: WritableSignal<AppState>;
  let toggleTheme: ReturnType<typeof vi.fn>;
  let setOrganisation: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    appState = signal<AppState>({
      user,
      organisations,
      selectedOrganisation: 'org-acme',
      theme: THEME_DARK,
      lang: 'en-GB',
    });
    toggleTheme = vi.fn(() => {
      appState.update((state) => ({ ...state, theme: state.theme === THEME_DARK ? THEME_LIGHT : THEME_DARK }));
    });
    setOrganisation = vi.fn((organisationId: string) => {
      appState.update((state) => ({ ...state, selectedOrganisation: organisationId }));
    });

    TestBed.configureTestingModule({
      imports: [provideTranslocoTesting()],
      providers: [
        {
          provide: AppStateService,
          useValue: {
            appStateUser: computed(() => appState().user),
            appStateOrganisations: computed(() => appState().organisations ?? []),
            appStateSelectedOrganisation: computed(() => appState().selectedOrganisation),
            appStateTheme: computed(() => appState().theme),
            toggleTheme,
            setOrganisation,
          },
        },
      ],
    });
  });

  it('shows the current account and the organisation choices', () => {
    // Given / When
    const fixture = TestBed.createComponent(AppUserProfile);
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    expect(host.textContent).toContain('Celia Yu');
    expect(host.textContent).toContain('celia.yu@acme.com');
    expect(host.textContent).toContain('Acme Corporation');
    expect(host.textContent).toContain('Northwind Logistics');
  });

  it('renders nothing when there is no user', () => {
    // Given
    appState.update((state) => ({ ...state, user: null }));

    // When
    const fixture = TestBed.createComponent(AppUserProfile);
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    expect(host.querySelector('button[aria-haspopup="true"]')).toBeNull();
    expect(host.textContent?.trim()).toBe('');
  });

  it('marks the selected organisation as current', () => {
    // Given / When
    const fixture = TestBed.createComponent(AppUserProfile);
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    const rows = Array.from(host.querySelectorAll<HTMLButtonElement>('[data-testid="organisation-row"]'));
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent ?? '').toContain('Member');
    expect(rows[0].querySelector('i')?.classList.contains('invisible')).toBe(false);
    expect(rows[1].textContent ?? '').toContain('Available');
    expect(rows[1].querySelector('i')?.classList.contains('invisible')).toBe(true);
  });

  it('switches organisation through the app state when a row is clicked', () => {
    // Given
    const fixture = TestBed.createComponent(AppUserProfile);
    fixture.detectChanges();
    const host: HTMLElement = fixture.nativeElement;
    const rows = host.querySelectorAll<HTMLButtonElement>('[data-testid="organisation-row"]');

    // When
    rows[1].click();
    fixture.detectChanges();

    // Then
    expect(setOrganisation).toHaveBeenCalledWith('org-northwind');
    expect(rows[1].textContent ?? '').toContain('Member');
    expect(rows[0].textContent ?? '').toContain('Available');
    expect(host.textContent).toContain('Northwind Logistics');
  });

  it('opens and closes the profile popup from the chip', () => {
    // Given
    const fixture = TestBed.createComponent(AppUserProfile);
    fixture.detectChanges();
    const host: HTMLElement = fixture.nativeElement;
    const chip = host.querySelector<HTMLButtonElement>('button[aria-haspopup="true"]');
    const popup = host.querySelector<HTMLElement>('#user-profile-popup');
    expect(chip?.getAttribute('aria-expanded')).toBe('false');
    expect(popup?.classList.contains('hidden')).toBe(true);

    // When
    chip?.click();
    fixture.detectChanges();

    // Then
    expect(chip?.getAttribute('aria-expanded')).toBe('true');
    expect(popup?.classList.contains('hidden')).toBe(false);

    // When
    chip?.click();
    fixture.detectChanges();

    // Then
    expect(chip?.getAttribute('aria-expanded')).toBe('false');
    expect(popup?.classList.contains('hidden')).toBe(true);
  });

  it('closes the popup when clicking outside', () => {
    // Given
    const fixture = TestBed.createComponent(AppUserProfile);
    fixture.detectChanges();
    const host: HTMLElement = fixture.nativeElement;
    host.querySelector<HTMLButtonElement>('button[aria-haspopup="true"]')?.click();
    fixture.detectChanges();
    const popup = host.querySelector<HTMLElement>('#user-profile-popup');
    expect(popup?.classList.contains('hidden')).toBe(false);

    // When
    document.body.click();
    fixture.detectChanges();

    // Then
    expect(popup?.classList.contains('hidden')).toBe(true);
  });

  it('shows the dark mode state and toggles it through the app state', async () => {
    // Given
    const fixture = TestBed.createComponent(AppUserProfile);
    fixture.detectChanges();
    const host: HTMLElement = fixture.nativeElement;
    host.querySelector<HTMLButtonElement>('button[aria-haspopup="true"]')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const darkMode = host.querySelector<HTMLInputElement>('#pref-dark');
    const darkModeRow = darkMode?.closest('label');
    expect(darkModeRow).not.toBeNull();
    expect(darkMode?.checked).toBe(true);

    // When
    darkModeRow?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    // Then
    expect(toggleTheme).toHaveBeenCalledTimes(1);
    expect(darkMode?.checked).toBe(false);
    expect(host.textContent).toContain('Advanced mode');
  });
});
