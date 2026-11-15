import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { SessionStore } from '../../core/session';
import { LanguageSwitch } from '../../shared/language-switch';
import { LogoMark } from '../../shared/logo-mark';

/** The frame of the public feed, for everyone: the logo (to the homepage), the languages, and the way into the app. */
@Component({
  selector: 'app-public-layout',
  imports: [TranslocoDirective, RouterLink, RouterOutlet, HlmButtonImports, LanguageSwitch, LogoMark],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page" *transloco="let t">
      <header class="bar">
        <a class="brand" href="/">
          <app-logo-mark class="mark" />
          <span class="wordmark">
            <span class="society">{{ t('app.society') }}</span>
            <span class="residents">{{ t('app.residents') }}</span>
          </span>
        </a>
        <app-language-switch />
      </header>
      <main>
        <router-outlet />
      </main>
      <footer class="foot">
        @if (signedIn()) {
          <a hlmBtn variant="outline" size="lg" routerLink="/feed">{{ t('public.openApp') }}</a>
        } @else {
          <p>{{ t('public.postHint') }}</p>
          <a hlmBtn variant="outline" size="lg" routerLink="/login">{{ t('public.signIn') }}</a>
        }
      </footer>
    </div>
  `,
  styles: `
    :host { display: block; }
    .page { min-height: 100dvh; max-width: 1120px; margin-inline: auto; padding: 0 var(--gutter) var(--space-xl); }
    .bar { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--space-sm); min-height: 64px; }
    .brand { display: flex; align-items: center; gap: 10px; min-height: 44px; color: inherit; text-decoration: none; border-radius: var(--radius-card); }
    .brand:hover .society { text-decoration: underline; text-underline-offset: 0.2em; }
    .brand:focus-visible { outline: 2px solid var(--color-focus); outline-offset: 4px; }
    .mark { height: 38px; }
    .wordmark { display: flex; flex-direction: column; line-height: 1.1; }
    .society { font-family: var(--font-display); font-size: var(--text-lg); color: var(--color-ink); }
    .residents { font-size: var(--text-sm); color: var(--color-ink-muted); }
    .foot {
      margin-top: var(--space-xl);
      padding-top: var(--space-md);
      border-top: var(--rule-hair) solid var(--color-rule);
      display: grid;
      gap: var(--space-sm);
      justify-items: start;
      color: var(--color-ink-2);
      max-width: 60ch;
    }
  `,
})
export default class PublicLayout {
  protected readonly signedIn = inject(SessionStore).signedIn;
}
