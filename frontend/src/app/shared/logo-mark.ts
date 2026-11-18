import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Garden City Seva's mark: a home inside a leaf. Drawn in the current text colour (brand green by default).
 * Decorative here, because the app's name always sits right beside it.
 */
@Component({
  selector: 'app-logo-mark',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg viewBox="14 6 92 110" aria-hidden="true" focusable="false">
      <path fill="currentColor" fill-rule="evenodd" d="M60 6 C96 28 112 62 98 92 C90 110 74 116 60 116 C46 116 30 110 22 92 C8 62 24 28 60 6 Z M60 44 L36 64 V92 H52 V74 H68 V92 H84 V64 Z" />
    </svg>
  `,
  styles: `
    :host { display: inline-block; color: var(--color-brand); line-height: 0; flex: none; }
    svg { height: 100%; width: auto; display: block; }
  `,
})
export class LogoMark {}
