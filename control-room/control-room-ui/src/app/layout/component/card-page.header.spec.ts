import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { CardPageHeader } from './card-page.header';

/** A page that supplies the title and, optionally, the context. */
@Component({
  selector: 'app-test-card-page-header-host',
  standalone: true,
  imports: [CardPageHeader],
  template: `
    <card-page-header>
      <span card-page-title>Page title</span>
      @if (showContext) {
        <span #cardPageContext card-page-context>404</span>
      }
    </card-page-header>
  `,
})
class CardPageHeaderTestHost {
  showContext = true;
}

describe('CardPageHeader', () => {
  it('shows the Hamoni logo and wordmark', () => {
    // Given / When
    const fixture = TestBed.createComponent(CardPageHeaderTestHost);
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    const logo = host.querySelector<HTMLImageElement>('img');
    expect(logo?.getAttribute('src')).toBe('/img/logo.svg');
    expect(host.textContent).toContain('Hamoni');
  });

  it('projects the title and the top right context', () => {
    // Given / When
    const fixture = TestBed.createComponent(CardPageHeaderTestHost);
    fixture.detectChanges();
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    const header = host.querySelector('card-page-header');
    expect(header?.querySelector('h1')?.textContent?.trim()).toBe('Page title');
    expect(header?.querySelector('[data-testid="card-page-title"]')?.textContent?.trim()).toBe('Page title');
    expect(header?.querySelector('[data-testid="card-page-context"]')?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      'Error 404',
    );
  });

  it('does not render the context block when the page supplies none', () => {
    // Given
    const fixture = TestBed.createComponent(CardPageHeaderTestHost);
    fixture.componentInstance.showContext = false;

    // When
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    expect(host.textContent).not.toContain('Error');
    expect(host.querySelector('[data-testid="card-page-context"]')).toBeNull();
  });
});
