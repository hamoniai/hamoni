import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { CardPageHeader } from '@/app/layout/component/card-page.header';

/**
 * The sign-in page.
 *
 * This is the public page for a visit without an organisation: the page the
 * design shows before any organisation is known. The organisation-only and
 * private-provider variants, and the provider round trips, arrive with the
 * login methods.
 *
 * The card page layout supplies the page shell, the width and the legal footer;
 * the card page header supplies the identity and the heading.
 *
 * Copy is written inline because the project has no resource bundle yet
 * (CODING-STANDARDS §18.3).
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CardPageHeader, TranslocoDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <section
      class="w-full rounded-lg border border-border bg-surface p-8 max-[560px]:rounded-md max-[560px]:p-6"
      aria-labelledby="card-page-title"
      *transloco="let t"
    >
      <card-page-header>
        <span card-page-title>{{ t('pages.login.title') }}</span>
      </card-page-header>

      <p class="mt-3 max-w-[54ch] text-body leading-[1.65] text-muted text-pretty break-words">
        {{ t('pages.login.description') }}
      </p>

      <div class="mt-7 flex flex-col gap-[10px]">
        <button
          type="button"
          data-provider="microsoft"
          class="relative flex h-11 w-full items-center justify-center rounded-md border border-border bg-bg px-4 text-body font-medium text-fg transition-colors duration-100 hover:border-muted hover:bg-raise active:translate-y-px"
        >
          <i class="pi pi-microsoft absolute left-4 text-[17px]" aria-hidden="true"></i>
          <span class="text-center">{{ t('pages.login.components.providers.microsoft') }}</span>
        </button>

        <button
          type="button"
          data-provider="google"
          class="relative flex h-11 w-full items-center justify-center rounded-md border border-border bg-bg px-4 text-body font-medium text-fg transition-colors duration-100 hover:border-muted hover:bg-raise active:translate-y-px"
        >
          <i class="pi pi-google absolute left-4 text-[17px]" aria-hidden="true"></i>
          <span class="text-center">{{ t('pages.login.components.providers.google') }}</span>
        </button>

        <button
          type="button"
          data-provider="apple"
          class="relative flex h-11 w-full items-center justify-center rounded-md border border-border bg-bg px-4 text-body font-medium text-fg transition-colors duration-100 hover:border-muted hover:bg-raise active:translate-y-px"
        >
          <i class="pi pi-apple absolute left-4 text-[17px]" aria-hidden="true"></i>
          <span class="text-center">{{ t('pages.login.components.providers.apple') }}</span>
        </button>
      </div>
    </section>
  `,
})
export class LoginComponent {}
