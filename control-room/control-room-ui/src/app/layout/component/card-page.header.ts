import { ChangeDetectionStrategy, Component, ElementRef, contentChild, viewChild } from '@angular/core';

/**
 * The header inside a standalone card page.
 *
 * It owns the whole top of the card: the Hamoni identity, the optional
 * right-hand context, and the page heading. A page supplies only the text for
 * the title and the context through content projection, so all markup, type
 * and layout stay in one place.
 *
 * The context block is only rendered when the page supplies one. A page
 * without a context — the sign-in pages — therefore has no hidden `Error`
 * prefix in its structure. A page that wants a context marks its element with
 * both `card-page-context` (the projection slot) and `#cardPageContext` (the
 * presence query).
 */
@Component({
  // The selector mirrors the class name; the card page components do not carry
  // the project-wide `app` prefix.
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: 'card-page-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <header data-testid="card-page-header">
      <div class="flex items-center justify-between gap-4 mb-7 max-[560px]:mb-6">
        <span class="inline-flex min-w-0 items-center gap-[9px]">
          <img src="/img/logo.svg" alt="" width="280" height="288" class="h-[22px] w-auto flex-none" />
          <span class="font-display text-[15px] font-bold tracking-[-0.012em] text-fg">Hamoni</span>
        </span>
        @if (context()) {
          <span
            data-testid="card-page-context"
            class="flex-none font-mono text-[22px] leading-none tracking-[-0.01em] tabular-nums text-muted max-[560px]:text-[20px]"
          >
            <span class="sr-only">Error </span>
            <ng-content select="[card-page-context]"></ng-content>
          </span>
        }
      </div>
      <h1
        #title
        id="card-page-title"
        data-testid="card-page-title"
        tabindex="-1"
        class="font-display text-[30px] font-bold leading-[1.2] tracking-[-0.021em] text-fg text-balance break-words focus-visible:outline-offset-4 max-[560px]:text-[24px]"
      >
        <ng-content select="[card-page-title]"></ng-content>
      </h1>
    </header>
  `,
})
export class CardPageHeader {
  /** The optional right-hand context text supplied by the page. */
  protected readonly context = contentChild('cardPageContext');

  private readonly title = viewChild<ElementRef<HTMLHeadingElement>>('title');

  /** Move focus to the heading, so a page is announced without focusing an action. */
  focusTitle(): void {
    this.title()?.nativeElement.focus({ preventScroll: true });
  }
}
