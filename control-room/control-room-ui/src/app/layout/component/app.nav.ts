import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
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
  imports: [RouterLink, AppUserProfile],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-col' },
  template: `
    <aside class="flex flex-1 flex-col border-r border-border bg-bg">
      <a routerLink="/" class="flex h-header flex-none items-center gap-2.5 px-3.5" aria-label="Hamoni">
        <img src="/img/logo.svg" alt="" width="280" height="288" class="h-[22px] w-auto flex-none" />
      </a>

      <div class="flex-none px-3.5 pt-7 pb-[22px]">
        <button
          type="button"
          class="flex h-8 w-full items-center justify-center gap-2 rounded-md border border-accent bg-accent px-3.5 text-sm font-semibold text-bg transition-colors duration-100 hover:border-accent-hover hover:bg-accent-hover active:border-accent-active active:bg-accent-active"
        >
          <i class="pi pi-play text-[14px]" aria-hidden="true"></i>
          <span>Start work</span>
        </button>
      </div>

      <nav class="flex flex-1 flex-col gap-6 px-3.5 pt-2.5 pb-4" aria-label="Sections">
        <div class="flex flex-col gap-0.5">
          <p class="px-2.5 pb-1.5 font-mono text-cap uppercase tracking-[0.07em] text-muted">Work</p>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-inbox flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>Inbox</span>
            <span class="ml-auto font-mono text-cap text-muted">11</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-chart-bar flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>Activity</span>
          </a>
        </div>

        <div class="flex flex-col gap-0.5">
          <p class="px-2.5 pb-1.5 font-mono text-cap uppercase tracking-[0.07em] text-muted">Manage</p>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-list flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>Processes</span>
            <span class="ml-auto font-mono text-cap text-muted">47</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-check-square flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>Tasks</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-users flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>People</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-file flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>Documents</span>
          </a>
        </div>

        <div class="flex flex-col gap-0.5">
          <p class="px-2.5 pb-1.5 font-mono text-cap uppercase tracking-[0.07em] text-muted">Design</p>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-box flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>Projects</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-clone flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>Templates</span>
          </a>
        </div>

        <div class="flex flex-col gap-0.5">
          <p class="px-2.5 pb-1.5 font-mono text-cap uppercase tracking-[0.07em] text-muted">Configure</p>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-building flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>Organisation</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-sitemap flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>Teams</span>
            <span class="ml-auto font-mono text-cap text-muted">8</span>
          </a>
          <a
            href="#"
            class="flex items-center gap-2.5 rounded-sm border border-transparent px-2.5 py-2 text-sm text-muted transition-colors duration-100 hover:bg-fg-soft hover:text-fg"
          >
            <i class="pi pi-shield flex-none text-[16px] opacity-90" aria-hidden="true"></i>
            <span>Security</span>
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
