import { AsyncPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChangeDetectionStrategy, Component, ElementRef, HostListener, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { TranslocoDirective } from '@jsverse/transloco';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { AppStateService, THEME_DARK } from '@/app/services/app-state.service';
import { UserOrganisation } from '@/app/services/auth.service';

/**
 * The user chip and profile popup in the navigation rail.
 *
 * The chip and popup read the application state for the user, the
 * organisations, the selected organisation and the theme. Organisation
 * switching and the advanced-mode preference remain future work.
 */
@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [AsyncPipe, FormsModule, ToggleSwitchModule, TranslocoDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <ng-container *transloco="let t">
      @if (appState.appStateUser(); as user) {
        <div class="relative">
          <button
            type="button"
            class="flex w-full items-center gap-2.5 rounded-sm border border-border bg-bg p-2 text-left transition-colors duration-100 hover:border-muted"
            aria-haspopup="true"
            aria-controls="user-profile-popup"
            [attr.aria-expanded]="open()"
            (click)="toggle()"
          >
            <span
              class="grid h-7 w-7 flex-none place-items-center rounded-full border border-border bg-inset font-mono text-cap text-fg"
              aria-hidden="true"
              >{{ user.initials }}</span
            >
            <span class="min-w-0 truncate">
              <span class="block text-sm text-fg">{{ user.name }}</span>
              <span class="block text-cap text-muted">{{ selectedOrganisation()?.name }}</span>
            </span>
            <i class="pi pi-chevron-down ml-auto flex-none text-muted" aria-hidden="true"></i>
          </button>

          <div
            id="user-profile-popup"
            class="absolute bottom-full left-0 z-[60] mb-2.5 max-h-[min(72vh,520px)] w-[292px] overflow-y-auto rounded-lg border border-border bg-surface"
            [class.hidden]="!open()"
          >
            <div class="flex items-center gap-2.5 px-4 pt-3.5 pb-2.5">
              <span
                class="grid h-8 w-8 flex-none place-items-center rounded-full border border-border bg-inset font-mono text-meta text-fg"
                aria-hidden="true"
                >{{ user.initials }}</span
              >
              <span class="min-w-0">
                <span class="block text-sm text-fg">{{ user.name }}</span>
                <span class="block text-meta text-muted">{{ user.email }}</span>
              </span>
            </div>

            <div>
              <p class="px-4 pt-3 pb-1 font-mono text-cap uppercase tracking-[0.07em] text-muted">
                {{ t('layout.app.nav.userProfile.organisation') }}
              </p>
              <div class="flex flex-col">
                @if (organisations$ | async; as organisations) {
                  @for (organisation of organisations; track organisation.id) {
                    <button
                      type="button"
                      data-testid="organisation-row"
                      (click)="appState.setOrganisation(organisation.id)"
                      class="flex w-full items-center gap-2.5 border-b border-border px-4 py-3 text-left transition-colors duration-100 last:border-b-0 hover:bg-fg-faint"
                    >
                      <span class="min-w-0 flex-1">
                        <span class="block text-sm text-fg">{{ organisation.name }}</span>
                        <span class="mt-0.5 block text-meta text-muted">
                          {{
                            isCurrent(organisation)
                              ? t('layout.app.nav.userProfile.member')
                              : t('layout.app.nav.userProfile.available')
                          }}
                        </span>
                      </span>
                      <i
                        class="pi pi-check flex-none text-[14px] text-fg"
                        [class.invisible]="!isCurrent(organisation)"
                        aria-hidden="true"
                      ></i>
                      @if (isCurrent(organisation)) {
                        <span class="sr-only">{{ t('layout.app.nav.userProfile.currentOrganisation') }}</span>
                      }
                    </button>
                  }
                }
              </div>
            </div>

            <div>
              <p class="px-4 pt-3 pb-1 font-mono text-cap uppercase tracking-[0.07em] text-muted">
                {{ t('layout.app.nav.userProfile.preferences') }}
              </p>
              <div class="flex flex-col gap-3.5 px-4 pt-2 pb-4">
                <label
                  for="pref-dark"
                  class="flex w-full cursor-pointer items-center justify-between text-sm text-muted"
                >
                  <span>{{ t('layout.app.nav.userProfile.darkMode') }}</span>
                  <p-toggleswitch
                    inputId="pref-dark"
                    data-testid="dark-mode"
                    [ngModel]="appState.appStateTheme() === THEME_DARK"
                    (ngModelChange)="appState.toggleTheme()"
                  />
                </label>

                <label
                  for="pref-advanced"
                  class="flex w-full cursor-pointer items-center justify-between text-sm text-muted"
                >
                  <span>{{ t('layout.app.nav.userProfile.advancedMode') }}</span>
                  <p-toggleswitch inputId="pref-advanced" [ngModel]="false" />
                </label>
              </div>
            </div>
          </div>
        </div>
      }
    </ng-container>
  `,
})
export class AppUserProfile {
  protected readonly appState = inject(AppStateService);
  protected readonly THEME_DARK = THEME_DARK;

  readonly #open = signal(false);
  readonly open = this.#open.asReadonly();

  readonly #elementRef = inject(ElementRef);

  protected readonly organisations$ = toObservable(this.appState.appStateOrganisations);

  readonly selectedOrganisation = computed(
    () =>
      this.appState
        .appStateOrganisations()
        .find((organisation) => organisation.id === this.appState.appStateSelectedOrganisation()) ?? null,
  );

  /** Open or close the profile popup. */
  toggle(): void {
    this.#open.update((open) => !open);
  }

  /** Whether the organisation is the currently selected one. */
  isCurrent(organisation: UserOrganisation): boolean {
    return organisation.id === this.appState.appStateSelectedOrganisation();
  }

  /** Close the popup when the click is outside this component. */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.#elementRef.nativeElement.contains(event.target as Node)) {
      this.#open.set(false);
    }
  }
}
