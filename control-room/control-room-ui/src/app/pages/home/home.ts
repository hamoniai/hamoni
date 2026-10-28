import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';

/**
 * Main home/landing page.
 */
@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterModule, TranslocoDirective],
  template: `
    <ng-container *transloco="let t">
      <p>{{ t('pages.home.placeholder') }}</p>
      <router-outlet></router-outlet>
    </ng-container>
  `,
})
export class HomeComponent {}
