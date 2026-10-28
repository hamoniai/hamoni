import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideTranslocoTesting } from '@/app/testing/transloco-testing';
import { createNotFoundProblemDetails, ProblemMessage } from '@/app/services/logging.service';
import { ErrorComponent } from './error';

/** The problem a caller passes through navigation state. */
const problem: ProblemMessage = {
  type: 'https://hamoni.ai/problems/not-found',
  title: "We couldn't find that page",
  status: 404,
  detail: 'The page you asked for does not exist, or it has moved.',
  instance: 'urn:hamoni:error?requestId=A7K2Q&code=404',
};

/** Return the trimmed, whitespace-normalised text of a descendant. */
function text(element: Element, selector: string): string {
  return element.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
}

describe('ErrorComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      teardown: { destroyAfterEach: true },
      imports: [provideTranslocoTesting()],
      providers: [
        provideRouter([
          { path: 'state', component: ErrorComponent, data: createNotFoundProblemDetails() },
          { path: 'data', component: ErrorComponent, data: createNotFoundProblemDetails() },
          { path: 'bare', component: ErrorComponent },
        ]),
      ],
    });
  });

  /** Navigate to a route and return the harness and the rendered error host. */
  async function render(
    path: string,
    state?: ProblemMessage,
  ): Promise<{ harness: RouterTestingHarness; host: HTMLElement }> {
    const harness = await RouterTestingHarness.create();
    const router = TestBed.inject(Router);
    await router.navigateByUrl(path, state ? { state } : {});

    harness.detectChanges();
    const host = harness.routeNativeElement;
    if (!host) {
      throw new Error(`The ${path} route did not render the error component.`);
    }
    return { harness, host };
  }

  it('renders the ProblemMessage carried in the navigation state', async () => {
    // Given / When
    const { host } = await render('/state', problem);

    // Then
    expect(text(host, '[data-testid="card-page-title"]')).toBe(problem.title);
    expect(text(host, '[data-testid="card-page-context"]')).toBe('Error 404');
    expect(text(host, '[data-problem-detail]')).toBe(problem.detail);
    expect(text(host, '[data-problem-id]')).toBe('A7K2Q');
    expect(host.querySelector('[data-problem-hint]')).not.toBeNull();
  });

  it('falls back to the ProblemMessage in the route data', async () => {
    // Given / When
    const { host } = await render('/data');

    // Then
    expect(text(host, '[data-testid="card-page-title"]')).toBe('Page not found');
    expect(text(host, '[data-testid="card-page-context"]')).toBe('Error 404');
  });

  it('falls back to the unknown problem when no problem is supplied', async () => {
    // Given / When
    const { host } = await render('/bare');

    // Then
    expect(text(host, '[data-testid="card-page-title"]')).toBe('Unknown error');
    expect(host.querySelector('[data-problem-ref]')).toBeNull();
  });

  it('copies the request id and confirms it', async () => {
    // Given
    const { harness, host } = await render('/state', problem);
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    Object.defineProperty(window, 'isSecureContext', { value: true, configurable: true });

    // When
    host.querySelector<HTMLButtonElement>('button[pButton]')?.click();

    // Then
    await vi.waitFor(() => expect(writeText).toHaveBeenCalledWith('A7K2Q'));
    harness.detectChanges();
    expect(text(host, 'button[pButton]')).toContain('Copied');
    expect(text(host, '[role="status"]')).toBe('Request ID A7K2Q copied');
  });
});
