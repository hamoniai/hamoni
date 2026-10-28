import { Component, computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { describe, expect, it } from 'vitest';
import { AppState, AppStateService, THEME_DARK } from '@/app/services/app-state.service';
import { provideTranslocoTesting } from '@/app/testing/transloco-testing';
import { AppLayout } from './app.layout';

/** A routed page used to prove that the layout renders through its outlet. */
@Component({
  selector: 'app-test-routed-page',
  standalone: true,
  template: `<p data-testid="routed-page">Routed page</p>`,
})
class TestRoutedPage {}

/** Provide the minimal app state the shell needs to render. */
function provideAppState() {
  const appState = signal<AppState>({
    user: null,
    organisations: null,
    selectedOrganisation: null,
    theme: THEME_DARK,
    lang: 'en-GB',
  });

  return {
    provide: AppStateService,
    useValue: {
      appStateUser: computed(() => appState().user),
      appStateOrganisations: computed(() => appState().organisations ?? []),
      appStateSelectedOrganisation: computed(() => appState().selectedOrganisation),
      appStateTheme: computed(() => appState().theme),
      toggleTheme: () => undefined,
      setOrganisation: () => undefined,
    },
  };
}

describe('AppLayout', () => {
  it('renders the routed page inside the content area', async () => {
    // Given
    TestBed.configureTestingModule({
      imports: [provideTranslocoTesting()],
      providers: [
        provideRouter([{ path: 'page', component: AppLayout, children: [{ path: '', component: TestRoutedPage }] }]),
        provideAppState(),
      ],
    });
    const harness = await RouterTestingHarness.create();

    // When
    await harness.navigateByUrl('/page');

    // Then
    const host = harness.fixture.nativeElement as HTMLElement;
    expect(host.querySelector('main [data-testid="routed-page"]')).not.toBeNull();
  });
});
