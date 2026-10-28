import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { AppLayout } from './app.layout';

/** Render the shell with the router the outlet needs. */
function renderLayout(): HTMLElement {
  TestBed.configureTestingModule({ providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(AppLayout);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('AppLayout', () => {
  it('renders the navigation rail, header and routed content area', () => {
    // Given / When
    const host = renderLayout();

    // Then
    expect(host.querySelector('app-nav')).not.toBeNull();
    expect(host.querySelector('app-header')).not.toBeNull();
    expect(host.querySelector('router-outlet')).not.toBeNull();
  });

  it('renders the static navigation groups and items', () => {
    // Given / When
    const host = renderLayout();

    // Then
    for (const label of [
      'Start work',
      'Work',
      'Inbox',
      'Activity',
      'Manage',
      'Processes',
      'Tasks',
      'People',
      'Documents',
      'Design',
      'Projects',
      'Templates',
      'Configure',
      'Organisation',
      'Teams',
      'Security',
    ]) {
      expect(host.textContent).toContain(label);
    }
    expect(host.textContent).toContain('11');
    expect(host.textContent).toContain('47');
    expect(host.textContent).toContain('8');
  });

  it('renders the header breadcrumb and global search', () => {
    // Given / When
    const host = renderLayout();

    // Then
    expect(host.textContent).toContain('Home');
    expect(host.querySelector<HTMLInputElement>('input[type="search"]')?.getAttribute('placeholder')).toBe(
      'Search or jump to…',
    );
  });

  it('renders the user profile chip in the rail footer', () => {
    // Given / When
    const host = renderLayout();

    // Then
    expect(host.querySelector('app-user-profile')).not.toBeNull();
    expect(host.textContent).toContain('Celia Yu');
    expect(host.textContent).toContain('Acme Corporation');
  });
});
