import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

/** Links to this site's legal pages, which are plain HTML outside the app: hence href, not routerLink. */
@Component({
  selector: 'app-legal-links',
  imports: [TranslocoDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="legal" *transloco="let t" [attr.aria-label]="t('legal.title')">
      <a [href]="base() + '#privacy'">{{ t('legal.privacy') }}</a>
      <a [href]="base() + '#terms'">{{ t('legal.terms') }}</a>
      <a [href]="base() + '#grievance'">{{ t('legal.grievance') }}</a>
    </nav>
  `,
  styles: `
    .legal { display: flex; flex-wrap: wrap; gap: 0 var(--space-sm); margin-top: var(--space-md); font-size: var(--text-sm); }
    a { display: inline-flex; align-items: center; min-height: 44px; color: var(--color-brand-deep); text-decoration: underline; text-decoration-thickness: 1px; text-underline-offset: 0.2em; }
  `,
})
export class LegalLinks {
  private readonly transloco = inject(TranslocoService);
  private readonly lang = toSignal(this.transloco.langChanges$, { initialValue: this.transloco.getActiveLang() });
  protected readonly base = computed(() => (this.lang() === 'en' ? '/legal/' : `/${this.lang()}/legal/`));
}
