import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';

/** For visitors without an account: the public feed. */
@Component({
  selector: 'app-login-browse',
  imports: [TranslocoDirective, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p class="browse" *transloco="let t">
      <span class="muted">{{ t('login.browseLead') }}</span>
      <a class="link press" routerLink="/public">{{ t('login.browse') }}</a>
    </p>
  `,
  styles: `
    .browse {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0 var(--space-xs);
      margin-top: var(--space-md);
      padding-top: var(--space-sm);
      border-top: var(--rule-hair) solid var(--color-rule);
      font-size: var(--text-sm);
    }
    .muted { color: var(--color-ink-muted); }
    /* a link, not a button: 44px tall, and free to wrap (the Hindi text is long on a phone) */
    .link { display: inline-flex; align-items: center; min-height: 44px; white-space: normal; color: var(--color-brand-deep); text-decoration: underline; text-decoration-thickness: 1px; text-underline-offset: 0.2em; }
  `,
})
export class LoginBrowse {}
