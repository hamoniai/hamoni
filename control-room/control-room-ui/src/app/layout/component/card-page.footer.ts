import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * The legal footer shown beneath a standalone card page.
 *
 * The sign-in and error screens share the same page footer, placed outside the
 * card because it belongs to the page rather than the object on it.
 *
 * The link labels are written inline because the project has no resource
 * bundle yet; they move to key-based lookup when localisation lands
 * (CODING-STANDARDS §18.3).
 */
@Component({
  // The selector mirrors the class name; the card page components do not carry
  // the project-wide `app` prefix.
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'card-page-footer',
  standalone: true,
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block flex-none' },
  template: `
    <footer data-testid="card-page-footer" class="px-6 pt-[18px] pb-7 text-center text-meta text-muted">
      <span data-testid="copyright">
        <a
          class="underline underline-offset-[3px] hover:text-fg"
          href="https://hamoni.ai"
          target="_blank"
          rel="noopener noreferrer"
          >&copy; {{ currentDate | date: 'yyyy' }} Hamoni Limited</a
        >
      </span>
      <span class="mx-2 opacity-50" aria-hidden="true">·</span>
      <a class="underline underline-offset-[3px] hover:text-fg" href="#">Privacy policy</a>
      <span class="mx-2 opacity-50" aria-hidden="true">·</span>
      <a class="underline underline-offset-[3px] hover:text-fg" href="#">Terms of service</a>
    </footer>
  `,
})
export class CardPageFooter {
  /** The date the copyright year is read from. */
  currentDate = new Date();
}
