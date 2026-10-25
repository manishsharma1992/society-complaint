import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Translation, TranslocoLoader, TranslocoService } from '@jsverse/transloco';

export type Lang = 'en' | 'hi' | 'mr';

/** The three languages, each named in itself so anyone can find their own. */
export const LANGUAGES: { code: Lang; name: string }[] = [
  { code: 'en', name: 'English' },
  { code: 'hi', name: 'हिन्दी' },
  { code: 'mr', name: 'मराठी' },
];

const STORAGE_KEY = 'mycity.lang';

@Injectable({ providedIn: 'root' })
export class TranslationLoader implements TranslocoLoader {
  private readonly http = inject(HttpClient);

  getTranslation(lang: string) {
    return this.http.get<Translation>(`i18n/${lang}.json`);
  }
}

/** The chosen language: remembered on this device and set on <html lang> for screen readers and fonts. */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly transloco = inject(TranslocoService);
  private readonly document = inject(DOCUMENT);
  readonly current = signal<Lang>(initial());

  constructor() {
    this.apply(this.current());
  }

  use(lang: Lang): void {
    this.current.set(lang);
    this.apply(lang);
    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch {
      // storage blocked: the choice lasts for this visit
    }
  }

  private apply(lang: Lang): void {
    this.transloco.setActiveLang(lang);
    this.document.documentElement.lang = lang;
  }
}

function initial(): Lang {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'en' || saved === 'hi' || saved === 'mr') {
      return saved;
    }
  } catch {
    // fall through to the browser's language
  }
  const browser = (navigator.language || 'en').slice(0, 2);
  return browser === 'hi' || browser === 'mr' ? browser : 'en';
}
