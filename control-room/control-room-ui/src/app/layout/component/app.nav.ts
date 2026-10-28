import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { AppUserProfile } from './app.user-profile';

/**
 * The left navigation rail.
 *
 * The brand, the page action, the grouped sections and the user chip are static
 * for now; routing and the collapse behaviour come later.
 *
 * Copy is written inline because the project has no resource bundle yet
 * (CODING-STANDARDS §18.3).
 */
@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [RouterLink, AppUserProfile, TranslocoDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-col' },
  template: `
    <aside class="flex flex-1 flex-col border-r border-border bg-bg" *transloco="let t">
      <a routerLink="/" class="flex h-header flex-none items-center gap-2.5 px-3.5">
        <img
          src="/img/logo.svg"
          [alt]="t('brand.logoAlt')"
          width="280"
          height="288"
          class="h-[22px] w-auto flex-none"
        />
      </a>

      <div class="flex-none px-3.5 pt-7 pb-[22px]">
        <button
          type="button"
          class="flex h-8 w-full items-center justify-center gap-2 rounded-md border border-accent bg-accent px-3.5 text-sm font-semibold text-bg transition-colors duration-100 hover:border-accent-hover hover:bg-accent-hover active:border-accent-active active:bg-accent-active"
        >
          <i class="pi pi-play text-[14px]" aria-hidden="true"></i>
          <span>{{ t('layout.app.nav.startWork') }}</span>
        </button>
      </div>

      <nav class="flex flex-1 flex-col gap-6 px-3.5 pt-2.5 pb-4" [attr.aria-label]="t('layout.app.nav.sections')">
        <div class="flex flex-col gap-0.5">
          <p class="px-2.5 pb-1.5 font-mono text-cap uppercase tracking-[0.07em] text-muted">
            {{ t('layout.app.nav.groups.work') }}
          </p>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-inbox flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.inbox.title') }}</span>
            <span class="ml-auto font-mono text-cap text-muted">11</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-chart-bar flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.activity.title') }}</span>
          </a>
        </div>

        <div class="flex flex-col gap-0.5">
          <p class="px-2.5 pb-1.5 font-mono text-cap uppercase tracking-[0.07em] text-muted">
            {{ t('layout.app.nav.groups.manage') }}
          </p>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-list flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.processes.title') }}</span>
            <span class="ml-auto font-mono text-cap text-muted">47</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-check-square flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.tasks.title') }}</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-users flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.people.title') }}</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-file flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.documents.title') }}</span>
          </a>
        </div>

        <div class="flex flex-col gap-0.5">
          <p class="px-2.5 pb-1.5 font-mono text-cap uppercase tracking-[0.07em] text-muted">
            {{ t('layout.app.nav.groups.design') }}
          </p>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-box flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.projects.title') }}</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-clone flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.templates.title') }}</span>
          </a>
        </div>

        <div class="flex flex-col gap-0.5">
          <p class="px-2.5 pb-1.5 font-mono text-cap uppercase tracking-[0.07em] text-muted">
            {{ t('layout.app.nav.groups.configure') }}
          </p>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-building flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.organisation.title') }}</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-sitemap flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.teams.title') }}</span>
            <span class="ml-auto font-mono text-cap text-muted">8</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-shield flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>{{ t('pages.security.title') }}</span>
          </a>
        </div>
      </nav>

      <div class="mt-auto flex flex-col gap-2 p-3.5">
        <app-user-profile></app-user-profile>
      </div>
    </aside>
  `,
})
export class AppNav {}
