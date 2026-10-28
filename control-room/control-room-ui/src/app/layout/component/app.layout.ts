import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppHeader } from './app.header';
import { AppNav } from './app.nav';

/**
 * The Control Room application shell.
 *
 * A fixed-width navigation rail on the left and the main column on the right:
 * the top header, then the routed page inside the content area. The page
 * scrolls as one document, so the header is not pinned.
 */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, AppNav, AppHeader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="grid min-h-screen grid-cols-[244px_minmax(0,1fr)] bg-bg">
      <app-nav></app-nav>

      <div class="flex min-w-0 flex-col">
        <app-header></app-header>

        <main class="flex min-w-0 flex-1 flex-col gap-10 px-7 pt-7 pb-16">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
})
export class AppLayout {}
