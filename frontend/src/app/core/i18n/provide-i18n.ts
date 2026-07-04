import { Injectable } from '@angular/core';
import {
  MissingTranslationHandler,
  MissingTranslationHandlerParams,
  provideTranslateService,
  TranslateLoader,
} from '@ngx-translate/core';
import { Observable, of } from 'rxjs';
import { TRANSLATIONS } from './translations';

/** Serves the in-memory pt-BR bundle (no HTTP round-trip; the bundle ships with the app). */
@Injectable()
export class InMemoryTranslateLoader implements TranslateLoader {
  getTranslation(lang: string): Observable<Record<string, string>> {
    return of(TRANSLATIONS[lang] ?? TRANSLATIONS['pt-BR']);
  }
}

/** Surfaces missing keys loudly in dev/test instead of rendering silent raw keys. */
@Injectable()
export class ReportMissingTranslationHandler implements MissingTranslationHandler {
  readonly missing = new Set<string>();

  handle(params: MissingTranslationHandlerParams): string {
    this.missing.add(params.key);
    console.warn(`missing pt-BR translation for key '${params.key}'`);
    return params.key;
  }
}

/** i18n providers shared by the app bootstrap and the unit tests (single source of truth). */
export function provideI18n() {
  return provideTranslateService({
    fallbackLang: 'pt-BR',
    lang: 'pt-BR',
    loader: { provide: TranslateLoader, useClass: InMemoryTranslateLoader },
    missingTranslationHandler: {
      provide: MissingTranslationHandler,
      useClass: ReportMissingTranslationHandler,
    },
  });
}
