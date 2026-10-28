import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { provideTranslocoTesting } from '@/app/testing/transloco-testing';
import { LoginComponent } from './login';

describe('LoginComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [provideTranslocoTesting()] });
  });

  it('renders the public sign-in card', () => {
    // Given / When
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    expect(host.querySelector('[data-testid="card-page-title"]')?.textContent?.trim()).toBe('Welcome back');
    expect(host.textContent).toContain('Use the account you use for work.');

    const labels = Array.from(host.querySelectorAll('button')).map((button) => button.textContent?.trim());
    expect(labels).toEqual(['Continue with Microsoft', 'Continue with Google', 'Continue with Apple']);
  });

  it('does not render the error context block', () => {
    // Given / When
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();

    // Then
    const host: HTMLElement = fixture.nativeElement;
    expect(host.querySelector('[data-testid="card-page-context"]')).toBeNull();
    expect(host.textContent).not.toContain('Error');
  });
});
