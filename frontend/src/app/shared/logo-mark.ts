import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * The app's mark, the "speaking tower": a residents' tower that is also a speech bubble. Drawn in the current text
 * colour (brand green by default). Decorative here, because the society's name always sits right beside it.
 * Master files: frontend/brand/logo/.
 */
@Component({
  selector: 'app-logo-mark',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg viewBox="72 24 112 216" aria-hidden="true" focusable="false">
      <path fill="currentColor" fill-rule="evenodd" d="M92 24 H164 A20 20 0 0 1 184 44 V188 A20 20 0 0 1 164 208 H126 L92 238 V208 A20 20 0 0 1 72 188 V44 A20 20 0 0 1 92 24 Z M96 56 h24 v24 h-24 Z M136 56 h24 v24 h-24 Z M96 100 h24 v24 h-24 Z M136 100 h24 v24 h-24 Z M96 144 h24 v24 h-24 Z M136 144 h24 v24 h-24 Z" />
    </svg>
  `,
  styles: `
    :host { display: inline-block; color: var(--color-brand); line-height: 0; flex: none; }
    svg { height: 100%; width: auto; display: block; }
  `,
})
export class LogoMark {}
