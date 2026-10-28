import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';

/**
 * The user chip and profile popup in the navigation rail.
 *
 * The data is static for now. The chip opens and closes the popup; switching
 * organisation and the preferences come later.
 *
 * Copy is written inline because the project has no resource bundle yet
 * (CODING-STANDARDS §18.3).
 */
@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [TranslocoDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="relative" *transloco="let t">
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
          >CY</span
        >
        <span class="min-w-0 truncate">
          <span class="block text-sm text-fg">Celia Yu</span>
          <span class="block text-cap text-muted">Acme Corporation</span>
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
            >CY</span
          >
          <span class="min-w-0">
            <span class="block text-sm text-fg">Celia Yu</span>
            <span class="block text-meta text-muted">celia.yu@acme.com</span>
          </span>
        </div>

        <div>
          <p class="px-4 pt-3 pb-1 font-mono text-cap uppercase tracking-[0.07em] text-muted">
            {{ t('layout.app.nav.userProfile.organisation') }}
          </p>
          <div class="flex flex-col">
            <button
              type="button"
              class="flex w-full items-center gap-2.5 border-b border-border px-4 py-3 text-left transition-colors duration-100 hover:bg-fg-faint"
            >
              <span class="min-w-0 flex-1">
                <span class="block text-sm text-fg">Acme Corporation</span>
                <span class="mt-0.5 block text-meta text-muted">{{ t('layout.app.nav.userProfile.member') }}</span>
              </span>
              <i class="pi pi-check flex-none text-[14px] text-fg" aria-hidden="true"></i>
              <span class="sr-only">{{ t('layout.app.nav.userProfile.currentOrganisation') }}</span>
            </button>
            <button
              type="button"
              class="flex w-full items-center gap-2.5 px-4 py-3 text-left transition-colors duration-100 hover:bg-fg-faint"
            >
              <span class="min-w-0 flex-1">
                <span class="block text-sm text-fg">Northwind Logistics</span>
                <span class="mt-0.5 block text-meta text-muted">{{ t('layout.app.nav.userProfile.available') }}</span>
              </span>
              <i class="pi pi-check invisible flex-none text-[14px] text-fg" aria-hidden="true"></i>
            </button>
          </div>
        </div>

        <div>
          <p class="px-4 pt-3 pb-1 font-mono text-cap uppercase tracking-[0.07em] text-muted">
            {{ t('layout.app.nav.userProfile.preferences') }}
          </p>
          <div class="flex flex-col gap-3.5 px-4 pt-2 pb-4">
            <div class="flex w-full items-center justify-between text-sm text-muted">
              <span>{{ t('layout.app.nav.userProfile.darkMode') }}</span>
              <span class="relative h-[18px] w-[34px] rounded-full border border-muted bg-bg opacity-50">
                <span class="absolute left-[2px] top-[2px] h-3 w-3 translate-x-4 rounded-full bg-fg"></span>
              </span>
            </div>
            <div class="flex w-full items-center justify-between text-sm text-muted">
              <span>{{ t('layout.app.nav.userProfile.advancedMode') }}</span>
              <span class="relative h-[18px] w-[34px] rounded-full border border-border bg-inset">
                <span class="absolute left-[2px] top-[2px] h-3 w-3 rounded-full bg-muted"></span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class AppUserProfile {
  readonly #open = signal(false);
  readonly open = this.#open.asReadonly();

  /** Open or close the profile popup. */
  toggle(): void {
    this.#open.update((open) => !open);
  }
}
