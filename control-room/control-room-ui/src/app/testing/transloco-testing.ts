import { ModuleWithProviders } from '@angular/core';
import { TranslocoTestingModule, TranslocoTestingOptions } from '@jsverse/transloco';
import enGB from '../../../public/i18n/en-GB.json';

const options: TranslocoTestingOptions = {
  langs: { 'en-GB': enGB },
  preloadLangs: true,
  translocoConfig: {
    availableLangs: ['en-GB'],
    defaultLang: 'en-GB',
    fallbackLang: 'en-GB',
    reRenderOnLangChange: true,
    prodMode: false,
  },
};

/** Provide Transloco with the shipped en-GB pack preloaded for unit tests. */
export function provideTranslocoTesting(): ModuleWithProviders<TranslocoTestingModule> {
  return TranslocoTestingModule.forRoot(options);
}
