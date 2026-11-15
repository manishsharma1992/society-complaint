import { Lang } from './language';

/** Latin digits in every language, so complaint numbers and flat labels (B2-503) read the same everywhere. */
export const localeOf = (lang: Lang) => `${lang}-IN-u-nu-latn`;

export function relativeTime(iso: string, lang: Lang, now = Date.now()): string {
  const seconds = Math.round((new Date(iso).getTime() - now) / 1000);
  const rtf = new Intl.RelativeTimeFormat(localeOf(lang), { numeric: 'auto' });
  const abs = Math.abs(seconds);
  if (abs < 60) return rtf.format(seconds, 'second');
  if (abs < 3600) return rtf.format(Math.round(seconds / 60), 'minute');
  if (abs < 86400) return rtf.format(Math.round(seconds / 3600), 'hour');
  return rtf.format(Math.round(seconds / 86400), 'day');
}

/** A video's length as m:ss, e.g. 0:06 (digits are the same in every language here). */
export function clipLength(durationMs: number): string {
  const total = Math.round(durationMs / 1000);
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`;
}

export function dateTime(iso: string, lang: Lang): string {
  return new Intl.DateTimeFormat(localeOf(lang), { weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }).format(new Date(iso));
}
