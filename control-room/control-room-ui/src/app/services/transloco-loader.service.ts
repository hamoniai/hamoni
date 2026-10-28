import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Translation, TranslocoLoader } from '@jsverse/transloco';
import { catchError, throwError, Observable } from 'rxjs';
import { LoggingService, getProblemDetailsFromHttpError } from '@/app/services/logging.service';

/**
 * Loads a language pack from the public i18n folder.
 *
 * The pack is requested when Transloco needs it, so the initial application
 * load does not wait for it. Wiring an initial preload is a later change.
 */
@Injectable({ providedIn: 'root' })
export class TranslocoLoaderService implements TranslocoLoader {
  readonly #http = inject(HttpClient);
  readonly #logging = inject(LoggingService);

  getTranslation(lang: string): Observable<Translation> {
    return this.#http.get<Translation>(`/i18n/${lang}.json`)
      .pipe(
        catchError((error: HttpErrorResponse) => {
          const problemDetails = getProblemDetailsFromHttpError(error, `Unable to load translation 'lang'`);
          this.#logging.apiCallFailed(problemDetails);
          return throwError(() => error);
        })
      );
  }
}
