import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';

/**
 * The top header of the Control Room shell.
 *
 * It carries the rail toggle, the current-page breadcrumb, the global search
 * and the actions affordance. The actions panel is static and hidden until the
 * interaction is wired.
 *
 * Copy is written inline because the project has no resource bundle yet
 * (CODING-STANDARDS §18.3).
 */
@Component({
  selector: 'app-header',
  standalone: true,
  imports: [TranslocoDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <header
      class="flex min-h-header flex-wrap items-center gap-4 border-b border-border bg-bg px-7 py-3"
      *transloco="let t"
    >
      <button
        type="button"
        class="inline-flex h-6 items-center justify-center rounded-md border border-transparent px-2.5 font-mono text-cap text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
        aria-expanded="true"
        [attr.aria-label]="t('layout.app.header.collapseNavigation')"
        [attr.title]="t('layout.app.header.collapseNavigation')"
      >
        <i class="pi pi-chevron-left text-[14px]" aria-hidden="true"></i>
      </button>

      <span class="flex items-center gap-2 font-mono text-cap uppercase tracking-[0.05em] text-muted">{{
        t('pages.home.title')
      }}</span>

      <label
        class="ml-auto flex h-8 min-w-[260px] items-center gap-2 rounded-md border border-border bg-bg px-2.5 transition-[border-color,box-shadow] duration-100 focus-within:border-accent focus-within:shadow-[0_0_0_3px_var(--accent-soft)]"
      >
        <i class="pi pi-search text-[14px] text-muted" aria-hidden="true"></i>
        <input
          type="search"
          class="min-w-0 flex-1 border-0 bg-transparent text-sm text-fg outline-none placeholder:text-muted"
          [attr.placeholder]="t('layout.app.header.searchPlaceholder')"
          [attr.aria-label]="t('layout.app.header.searchAriaLabel')"
        />
        <span class="flex-none rounded-xs border border-border px-1.5 py-px font-mono text-[10px] text-muted">⌘F</span>
      </label>

      <div class="relative">
        <button
          type="button"
          class="relative inline-grid h-8 w-8 place-items-center rounded-sm border border-border text-muted transition-colors duration-100 hover:border-muted hover:bg-fg-soft hover:text-fg"
          aria-haspopup="true"
          aria-expanded="false"
          [attr.title]="t('layout.app.header.actions')"
          [attr.aria-label]="t('layout.app.header.actionsAriaLabel', { count: 2 })"
        >
          <i class="pi pi-bell" aria-hidden="true"></i>
          <span
            class="absolute -right-1.5 -top-1.5 grid h-4 min-w-4 place-items-center rounded-full border border-bg bg-fg px-1 font-mono text-[10px] font-bold leading-none text-bg"
            aria-hidden="true"
            >2</span
          >
        </button>

        <div
          class="absolute right-0 top-[calc(100%+10px)] z-[60] hidden max-h-[min(70vh,460px)] w-[380px] overflow-y-auto rounded-lg border border-border bg-surface"
        >
          <div class="flex flex-wrap items-center gap-4 border-b border-border px-4 py-3">
            <span class="text-cap font-semibold uppercase tracking-[0.07em] text-muted">{{
              t('layout.app.header.actionsPanel.waiting')
            }}</span>
            <span class="ml-auto text-meta text-muted">2</span>
          </div>

          <div class="flex flex-col">
            <div
              class="flex items-center gap-4 border-b border-border px-3.5 py-3 transition-colors duration-100 last:border-b-0 hover:bg-fg-faint"
            >
              <span class="min-w-0 flex-1">
                <span class="block font-semibold text-fg">{{ t('layout.app.header.actionsPanel.approveTitle') }}</span>
                <div class="text-meta text-muted">
                  {{ t('layout.app.header.actionsPanel.approveDetail', { name: 'Dana Okafor', days: 2 }) }}
                </div>
              </span>
              <button
                type="button"
                class="h-6 flex-none rounded-md border border-border px-2.5 font-mono text-cap text-fg transition-colors duration-100 hover:border-muted hover:bg-fg-soft"
              >
                {{ t('layout.app.header.actionsPanel.review') }}
              </button>
            </div>

            <div class="flex items-center gap-4 px-3.5 py-3 transition-colors duration-100 hover:bg-fg-faint">
              <span class="min-w-0 flex-1">
                <span class="block font-semibold text-fg">{{ t('layout.app.header.actionsPanel.accessTitle') }}</span>
                <div class="text-meta text-muted">
                  {{ t('layout.app.header.actionsPanel.accessDetail', { name: 'Dana Okafor' }) }}
                </div>
              </span>
              <button
                type="button"
                class="h-6 flex-none rounded-md border border-border px-2.5 font-mono text-cap text-fg transition-colors duration-100 hover:border-muted hover:bg-fg-soft"
              >
                {{ t('layout.app.header.actionsPanel.review') }}
              </button>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-2 border-t border-border px-4 py-3">
            <span class="text-meta text-muted">
              {{ t('layout.app.header.actionsPanel.footnote') }}
            </span>
          </div>
        </div>
      </div>
    </header>
  `,
})
export class AppHeader {}
