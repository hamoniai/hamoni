import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  afterNextRender,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardPageHeader } from '@/app/layout/component/card-page.header';
import { ProblemMessage, createUnknownProblemDetails } from '@/app/services/logging.service';

/** The request identifier is the `requestId` parameter of the problem instance URI. */
function requestIdFromInstance(instance: string): string {
  const match = instance.match(/[?&]requestId=([^&]+)/);
  if (!match) {
    return '';
  }

  try {
    return decodeURIComponent(match[1]);
  } catch {
    return match[1];
  }
}

/** Whether a route value is a complete problem the page can render. */
function isProblemMessage(value: unknown): value is ProblemMessage {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate['type'] === 'string' &&
    typeof candidate['title'] === 'string' &&
    typeof candidate['status'] === 'number' &&
    typeof candidate['detail'] === 'string' &&
    typeof candidate['instance'] === 'string'
  );
}

/**
 * Display an error page from a ProblemMessage.
 *
 * Several routes navigate here with different problems, so the problem is read
 * from the navigation state. A route that declares the problem as route data
 * instead — the catch-all not-found route — is used as the fallback. Both are
 * ProblemMessage objects; neither is a route parameter.
 *
 * Copy is written inline because the project has no resource bundle yet
 * (CODING-STANDARDS §18.3).
 */
@Component({
  selector: 'app-error',
  standalone: true,
  imports: [ButtonModule, RouterLink, CardPageHeader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <section
      class="w-full rounded-lg border border-border bg-surface p-8 max-[560px]:rounded-md max-[560px]:p-6"
      aria-labelledby="card-page-title"
    >
      <card-page-header>
        <span card-page-title>{{ problemMessage.title }}</span>
        @if (problemMessage.status) {
          <span #cardPageContext card-page-context>{{ problemMessage.status }}</span>
        }
      </card-page-header>

      <p
        data-problem-detail
        class="mt-3 max-w-[54ch] text-body leading-[1.65] text-muted text-pretty break-words"
        [hidden]="!problemMessage.detail"
      >
        {{ problemMessage.detail }}
      </p>

      @if (requestId) {
        <div data-problem-ref class="mt-7 flex flex-wrap items-center gap-x-3 gap-y-[10px] border-t border-border pt-5">
          <span class="text-cap font-semibold uppercase tracking-[0.07em] text-muted">Request ID</span>
          <code data-problem-id class="select-all font-mono text-sm tracking-[0.08em] text-fg">{{ requestId }}</code>
          <button
            pButton
            type="button"
            severity="secondary"
            class="ml-auto min-w-24 max-[560px]:h-11 max-[560px]:min-w-[104px]"
            [attr.aria-label]="'Copy request ID ' + requestId"
            (click)="copyRequestId()"
          >
            @if (copied()) {
              <i class="pi pi-check" aria-hidden="true"></i>
            } @else {
              <i class="pi pi-copy" aria-hidden="true"></i>
            }
            <span>{{ copied() ? 'Copied' : 'Copy' }}</span>
          </button>
        </div>
        <p data-problem-hint class="mt-2 text-meta text-muted">Quote this if you report the problem.</p>
      }

      <div class="mt-7">
        <a pButton routerLink="/" severity="primary" size="large" class="max-[560px]:h-11">Back to home</a>
      </div>

      <span class="sr-only" role="status" aria-live="polite">{{ copyAnnouncement() }}</span>
    </section>
  `,
})
export class ErrorComponent {
  readonly #router = inject(Router);
  readonly #route = inject(ActivatedRoute);
  readonly #destroyRef = inject(DestroyRef);

  /** The problem to display, from navigation state or the route's data. */
  readonly problemMessage: ProblemMessage = this.#resolveProblemMessage();

  /** The request reference shown to the person, when the problem carries one. */
  readonly requestId: string = requestIdFromInstance(this.problemMessage.instance);

  readonly #copied = signal(false);
  readonly copied = this.#copied.asReadonly();

  readonly #copyAnnouncement = signal('');
  readonly copyAnnouncement = this.#copyAnnouncement.asReadonly();

  private readonly cardPageHeader = viewChild(CardPageHeader);

  #resetCopyTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    // Announce the error by moving focus to the heading, not the action.
    afterNextRender(() => this.cardPageHeader()?.focusTitle());
    this.#destroyRef.onDestroy(() => clearTimeout(this.#resetCopyTimer));
  }

  /** Copy the request reference, with the legacy path as a fallback. */
  copyRequestId(): void {
    if (!this.requestId) {
      return;
    }

    if (window.isSecureContext && navigator.clipboard) {
      navigator.clipboard.writeText(this.requestId).then(
        () => this.#confirmCopy(),
        () => this.#copyWithLegacyApi(),
      );
      return;
    }

    this.#copyWithLegacyApi();
  }

  /** Show the copied state for a moment, then return the control to Copy. */
  #confirmCopy(): void {
    this.#copied.set(true);
    this.#copyAnnouncement.set(`Request ID ${this.requestId} copied`);
    clearTimeout(this.#resetCopyTimer);
    this.#resetCopyTimer = setTimeout(() => {
      this.#copied.set(false);
      this.#copyAnnouncement.set('');
    }, 2200);
  }

  /** Copy through the browser's older selection API when the clipboard is unavailable. */
  #copyWithLegacyApi(): void {
    if (this.#legacyCopy(this.requestId)) {
      this.#confirmCopy();
    } else {
      this.#copyAnnouncement.set(`Could not copy — the request ID is ${this.requestId}`);
    }
  }

  /** Select and copy text with the deprecated API kept for insecure contexts. */
  #legacyCopy(text: string): boolean {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', '');
    textarea.style.position = 'fixed';
    textarea.style.top = '-1000px';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();

    try {
      return document.execCommand('copy');
    } catch {
      return false;
    } finally {
      document.body.removeChild(textarea);
    }
  }

  /** Prefer the navigation state, then the route's own problem data. */
  #resolveProblemMessage(): ProblemMessage {
    const navigationState = this.#router.currentNavigation()?.extras?.state;
    if (isProblemMessage(navigationState)) {
      return navigationState;
    }

    const routeData = this.#route.snapshot.data;
    if (isProblemMessage(routeData)) {
      return routeData;
    }

    return createUnknownProblemDetails();
  }
}
