import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { CardPageFooter } from './card-page.footer';

describe('CardPageFooter', () => {
  it('shows the copyright and legal links in order on one line', () => {
    // Given / When
    const fixture = TestBed.createComponent(CardPageFooter);
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    const footer = host.querySelector<HTMLElement>('[data-testid="card-page-footer"]');
    expect(footer?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      `© ${new Date().getFullYear()} Hamoni Limited·Privacy policy·Terms of service`,
    );

    const links = Array.from(footer?.querySelectorAll('a') ?? []).map((link) => link.textContent?.trim());
    expect(links).toEqual([`© ${new Date().getFullYear()} Hamoni Limited`, 'Privacy policy', 'Terms of service']);

    const separators = Array.from(footer?.querySelectorAll('span[aria-hidden="true"]') ?? []).map((separator) =>
      separator.textContent?.trim(),
    );
    expect(separators).toEqual(['·', '·']);
  });

  it('links the copyright to hamoni.ai', () => {
    // Given / When
    const fixture = TestBed.createComponent(CardPageFooter);
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    const link = host.querySelector<HTMLAnchorElement>('[data-testid="copyright"] a');
    expect(link?.textContent?.replace(/\s+/g, ' ').trim()).toBe(`© ${new Date().getFullYear()} Hamoni Limited`);
    expect(link?.getAttribute('href')).toBe('https://hamoni.ai');
  });
});
