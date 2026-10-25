import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { LANGUAGES, LanguageService } from '../core/language';

/** English, हिन्दी, मराठी: each named in its own script; the chosen one is underlined in the brand green. */
@Component({
  selector: 'app-language-switch',
  imports: [TranslocoDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="switch" role="group" *transloco="let t" [attr.aria-label]="t('lang.label')">
      @for (language of languages; track language.code) {
        <button
          type="button"
          class="press"
          [attr.lang]="language.code"
          [attr.aria-pressed]="language.code === current()"
          (click)="choose(language.code)">
          {{ language.name }}
        </button>
      }
    </div>
  `,
  styles: `
    .switch { display: flex; gap: var(--space-2xs); }
    button {
      position: relative;
      min-height: 44px;
      padding-inline: var(--space-xs);
      background: none;
      border: 0;
      color: var(--color-ink-muted);
      font-size: var(--text-sm);
      cursor: pointer;
      transition: color var(--dur-short) var(--ease-out), transform var(--dur-micro) var(--ease-out);
    }
    button::after {
      content: '';
      position: absolute;
      inset-inline: var(--space-xs);
      bottom: 10px;
      height: 2px;
      background: var(--color-brand);
      transform: scaleX(0);
      transition: transform var(--dur-short) var(--ease-out);
    }
    button[aria-pressed='true'] { color: var(--color-ink); }
    button[aria-pressed='true']::after { transform: scaleX(1); }
    @media (hover: hover) and (pointer: fine) {
      button:hover { color: var(--color-ink); }
    }
  `,
})
export class LanguageSwitch {
  private readonly language = inject(LanguageService);
  protected readonly languages = LANGUAGES;
  protected readonly current = this.language.current;

  protected choose(code: (typeof LANGUAGES)[number]['code']): void {
    this.language.use(code);
  }
}
