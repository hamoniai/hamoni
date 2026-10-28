import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { AppUserProfile } from './app.user-profile';

describe('AppUserProfile', () => {
  it('shows the current account and the organisation choices', () => {
    // Given / When
    const fixture = TestBed.createComponent(AppUserProfile);
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    expect(host.textContent).toContain('Celia Yu');
    expect(host.textContent).toContain('celia.yu@acme.com');
    expect(host.textContent).toContain('Acme Corporation');
    expect(host.textContent).toContain('Northwind Logistics');
  });

  it('shows the preference controls', () => {
    // Given / When
    const fixture = TestBed.createComponent(AppUserProfile);
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    expect(host.textContent).toContain('Dark mode');
    expect(host.textContent).toContain('Advanced mode');
  });

  it('opens and closes the profile popup from the chip', () => {
    // Given
    const fixture = TestBed.createComponent(AppUserProfile);
    fixture.detectChanges();
    const host: HTMLElement = fixture.nativeElement;
    const chip = host.querySelector<HTMLButtonElement>('button[aria-haspopup="true"]');
    const popup = host.querySelector<HTMLElement>('#user-profile-popup');
    expect(chip?.getAttribute('aria-expanded')).toBe('false');
    expect(popup?.classList.contains('hidden')).toBe(true);

    // When
    chip?.click();
    fixture.detectChanges();

    // Then
    expect(chip?.getAttribute('aria-expanded')).toBe('true');
    expect(popup?.classList.contains('hidden')).toBe(false);

    // When
    chip?.click();
    fixture.detectChanges();

    // Then
    expect(chip?.getAttribute('aria-expanded')).toBe('false');
    expect(popup?.classList.contains('hidden')).toBe(true);
  });
});
