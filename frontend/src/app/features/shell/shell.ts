import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideChartColumn, lucideInbox, lucideListChecks, lucideUserRound, lucideUserRoundCheck, lucideWrench } from '@ng-icons/lucide';
import { Roles } from '../../core/roles';
import { SessionStore } from '../../core/session';
import { LanguageSwitch } from '../../shared/language-switch';

/**
 * The signed-in frame. Phones: a top bar and a bottom tab bar (tabs navigate, never act), translucent so content
 * scrolls beneath it. From 60rem the same two destinations sit in the top bar.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslocoDirective, NgIcon, LanguageSwitch],
  providers: [provideIcons({ lucideChartColumn, lucideInbox, lucideListChecks, lucideUserRound, lucideUserRoundCheck, lucideWrench })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="shell" *transloco="let t">
      <a class="skip" href="#main">{{ t('home.title') }}</a>
      <header class="top">
        <a routerLink="/" class="wordmark">
          <span class="society">{{ t('app.society') }}</span>
          <span class="residents">{{ t('app.residents') }}</span>
        </a>
        <nav class="top-nav" [attr.aria-label]="t('nav.main')">
          @for (tab of tabs(); track tab.path) {
            <a [routerLink]="tab.path" routerLinkActive="active" ariaCurrentWhenActive="page">{{ t(tab.label) }}</a>
          }
        </nav>
        <app-language-switch />
      </header>

      <main id="main" tabindex="-1">
        <router-outlet />
      </main>

      <nav class="tabs" [attr.aria-label]="t('nav.main')" [style.--tab-count]="tabs().length">
        @for (tab of tabs(); track tab.path) {
          <a [routerLink]="tab.path" routerLinkActive="active" ariaCurrentWhenActive="page" class="press">
            <ng-icon [name]="tab.icon" aria-hidden="true" />
            <span>{{ t(tab.label) }}</span>
          </a>
        }
      </nav>
    </div>
  `,
  styles: `
    :host { display: block; }
    .shell { min-height: 100dvh; }
    .skip { position: absolute; left: var(--space-sm); top: -100px; z-index: var(--z-toast); padding: var(--space-xs) var(--space-sm); background: var(--color-stock); }
    .skip:focus-visible { top: var(--space-sm); }
    .top {
      max-width: 1120px;
      margin-inline: auto;
      padding-inline: var(--gutter);
      min-height: 64px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--space-md);
    }
    .wordmark { display: flex; flex-direction: column; line-height: 1.1; text-decoration: none; }
    .society { font-family: var(--font-display); font-size: var(--text-lg); color: var(--color-ink); }
    .residents { font-size: var(--text-sm); color: var(--color-ink-muted); }
    .top-nav { display: none; }
    main { max-width: 1120px; margin-inline: auto; padding: 0 var(--gutter) calc(96px + env(safe-area-inset-bottom)); outline: none; }

    /* bottom tab bar: a light material over the content, with the home-indicator area kept clear */
    .tabs {
      position: fixed;
      inset: auto 0 0;
      z-index: var(--z-sticky-nav);
      display: grid;
      grid-template-columns: repeat(var(--tab-count, 2), minmax(0, 1fr));
      padding-bottom: env(safe-area-inset-bottom);
      background: color-mix(in oklab, var(--color-paper) 82%, transparent);
      backdrop-filter: blur(18px) saturate(160%);
      border-top: var(--rule-hair) solid var(--color-rule);
    }
    .tabs a {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;
      min-height: 56px;
      font-size: var(--text-sm);
      color: var(--color-ink-muted);
      text-decoration: none;
    }
    .tabs a ng-icon { font-size: 22px; }
    .tabs a.active { color: var(--color-brand-deep); }
    .tabs a:active { transform: scale(0.97); }

    @media (prefers-reduced-transparency: reduce) {
      .tabs { background: var(--color-paper); backdrop-filter: none; }
    }

    @media (min-width: 60rem) {
      .tabs { display: none; }
      main { padding-bottom: var(--space-2xl); }
      .top-nav { display: flex; gap: var(--space-lg); margin-inline-start: auto; }
      .top-nav a {
        position: relative;
        display: grid;
        place-items: center;
        min-height: 44px;
        color: var(--color-ink-muted);
        text-decoration: none;
        transition: color var(--dur-short) var(--ease-out);
      }
      .top-nav a::after {
        content: '';
        position: absolute;
        inset: auto 0 8px;
        height: 2px;
        background: var(--color-brand);
        transform: scaleX(0);
        transition: transform var(--dur-short) var(--ease-out);
      }
      .top-nav a.active { color: var(--color-ink); }
      .top-nav a.active::after { transform: scaleX(1); }
      .top-nav a:hover { color: var(--color-ink); }
    }
  `,
})
export default class Shell {
  private readonly roles = inject(Roles);

  constructor() {
    // pick up an approval or a new role given since this login; the saved session stands if this fails
    inject(SessionStore).renew().subscribe({ error: () => {} });
  }

  /**
   * Each person's own work comes first (five tabs at most). Admins get the staff report as a tab; staff open their
   * own numbers from their jobs screen, which also keeps an admin who is also staff at five tabs.
   */
  protected readonly tabs = computed(() => [
    ...(this.roles.isAdmin() ? [{ path: '/assign', label: 'nav.assign', icon: 'lucideInbox' }] : []),
    ...(this.roles.isStaff() ? [{ path: '/jobs', label: 'nav.jobs', icon: 'lucideWrench' }] : []),
    { path: '/complaints', label: 'nav.complaints', icon: 'lucideListChecks' },
    ...(this.roles.isSocietyAdmin() ? [{ path: '/signups', label: 'nav.signups', icon: 'lucideUserRoundCheck' }] : []),
    ...(this.roles.isAdmin() && !this.roles.isStaff() ? [{ path: '/staff', label: 'nav.staff', icon: 'lucideChartColumn' }] : []),
    { path: '/profile', label: 'nav.profile', icon: 'lucideUserRound' },
  ]);
}
