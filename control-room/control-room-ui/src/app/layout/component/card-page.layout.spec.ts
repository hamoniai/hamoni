import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { describe, expect, it } from 'vitest';
import { CardPageLayout } from './card-page.layout';

/** A routed page used to prove that the layout renders its child through the outlet. */
@Component({
  selector: 'app-card-page-test-child',
  standalone: true,
  template: `<section data-testid="routed-card">Card content</section>`,
})
class CardPageTestChild {}

/** Render the layout with only the router it needs to compile the outlet. */
function renderLayout(wide?: boolean) {
  TestBed.configureTestingModule({ providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(CardPageLayout);
  if (wide !== undefined) {
    fixture.componentRef.setInput('wide', wide);
  }
  fixture.detectChanges();
  return fixture;
}

/** Return the width-constrained content column of a rendered layout. */
function getColumn(element: HTMLElement): HTMLElement | null {
  return element.querySelector<HTMLElement>('[data-testid="card-page-column"]');
}

describe('CardPageLayout', () => {
  it('limits the content column to 440px by default', () => {
    // Given / When
    const fixture = renderLayout();

    // Then
    expect(getColumn(fixture.nativeElement)?.style.maxWidth).toBe('440px');
  });

  it('widens the content column to 560px when wide', () => {
    // Given / When
    const fixture = renderLayout(true);

    // Then
    expect(getColumn(fixture.nativeElement)?.style.maxWidth).toBe('560px');
  });

  it('renders the routed page inside the content column', async () => {
    // Given
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          {
            path: 'page',
            component: CardPageLayout,
            children: [{ path: '', component: CardPageTestChild }],
          },
        ]),
      ],
    });
    const harness = await RouterTestingHarness.create();

    // When
    await harness.navigateByUrl('/page');

    // Then
    const column = getColumn(harness.fixture.nativeElement);
    expect(column?.querySelector('[data-testid="routed-card"]')).not.toBeNull();
  });

  it('pins the shared footer below the main content', () => {
    // Given / When
    const fixture = renderLayout();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    const main = host.querySelector('main');
    const footer = host.querySelector('app-card-page-footer');
    expect(footer).not.toBeNull();
    expect(main?.nextElementSibling).toBe(footer);
    expect(getColumn(host)?.querySelector('app-card-page-footer')).toBeNull();
  });
});
