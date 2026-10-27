import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CardPageFooter } from './card-page.footer';

/**
 * The routed shell shared by the sign-in and error surfaces.
 *
 * It mirrors the standalone-card page in the screen design: a full-height page
 * with one width-constrained column, in which the routed page renders its
 * single card, and the shared legal footer pinned below it.
 *
 * The card's top is pinned at a consistent offset rather than centred, so the
 * title does not move when the page body changes length. The column is fluid,
 * so the page stays usable on a narrow viewport.
 */
@Component({
  selector: 'app-card-page-layout',
  standalone: true,
  imports: [RouterOutlet, CardPageFooter],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="flex min-h-screen flex-col bg-bg">
      <main
        class="flex flex-1 flex-col items-center px-6 pt-[clamp(48px,16vh,160px)] pb-14 max-[560px]:px-4 max-[560px]:pt-8 max-[560px]:pb-10"
      >
        <div data-testid="card-page-column" class="w-full" [style.max-width.px]="wide ? 560 : 440">
          <router-outlet></router-outlet>
        </div>
      </main>
      <app-card-page-footer></app-card-page-footer>
    </div>
  `,
})
export class CardPageLayout {
  /**
   * Whether the page needs the wider column.
   *
   * `true` allows the column to grow to 560px; `false` limits it to 440px.
   * A narrow viewport still shrinks the column below either value.
   */
  @Input() wide = false;
}
