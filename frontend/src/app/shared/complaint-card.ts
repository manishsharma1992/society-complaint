import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideImage } from '@ng-icons/lucide';
import { LanguageService } from '../core/language';
import { dateTime, relativeTime } from '../core/format';
import { Complaint, isOngoing } from '../core/models';

/**
 * The app's signature object: a complaint as a numbered card on cotton stock, its facts set as a ruled
 * wall label beneath. The status is always a word in its chip, never colour alone.
 */
@Component({
  selector: 'app-complaint-card',
  imports: [TranslocoDirective, NgIcon],
  providers: [provideIcons({ lucideImage })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="ticket stock" *transloco="let t">
      <header>
        <p class="no num">{{ t('card.number', { id: complaint().id }) }}</p>
        <span class="chip" [attr.data-status]="complaint().status">{{ t('status.' + complaint().status) }}</span>
      </header>
      <h2>{{ complaint().title }}</h2>
      <dl class="label ruled">
        <div><dt class="visually-hidden">{{ t('raise.categoryLabel') }}</dt><dd>{{ categoryName() ? t('category.' + categoryName()) : '' }}</dd></div>
        <div><dt class="visually-hidden">{{ t('raise.locationLabel') }}</dt><dd>{{ where() ?? t('card.commonArea') + ', ' + (towerName() ?? t('card.wholeSociety')) }}</dd></div>
        <div class="time">
          @if (ongoing()) {
            <dd [class.late]="overdue()">{{ overdue() ? t('card.overdue', { when: due() }) : t('card.due', { when: due() }) }}</dd>
          } @else {
            <dd class="muted">{{ t('card.raised', { when: raised() }) }}</dd>
          }
          @if (complaint().photos.length) {
            <dd class="muted photos"><ng-icon name="lucideImage" aria-hidden="true" /><span>{{ t('card.photos', { count: complaint().photos.length }) }}</span></dd>
          }
        </div>
      </dl>
    </article>
  `,
  styles: `
    :host { display: block; height: 100%; }
    .ticket { height: 100%; align-content: start; padding: var(--space-md) var(--space-md) var(--space-sm); display: grid; gap: var(--space-xs); }
    header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-sm); }
    .no { font-size: var(--text-sm); color: var(--color-ink-muted); }
    h2 { font-family: var(--font-display); font-weight: var(--serif-small); font-size: var(--text-lg); line-height: 1.25; max-width: none; color: var(--color-ink); }
    .label { margin: var(--space-2xs) 0 0; font-size: var(--text-sm); }
    .label > div { display: flex; flex-wrap: wrap; justify-content: space-between; gap: var(--space-2xs) var(--space-sm); padding-block: var(--space-xs); }
    .label > div + div { border-top: var(--rule-hair) solid var(--color-rule); }
    dd { margin: 0; color: var(--color-ink-2); }
    .muted { color: var(--color-ink-muted); }
    .late { color: var(--color-error); }
    .photos { display: inline-flex; align-items: center; gap: var(--space-2xs); }
    .chip {
      flex: none;
      padding: 2px var(--space-xs);
      border: var(--rule-hair) solid currentColor;
      border-radius: var(--radius-pill);
      font-size: var(--text-sm);
      line-height: 1.5;
      color: var(--color-ink);
    }
    .chip[data-status='ASSIGNED'], .chip[data-status='IN_PROGRESS'] { background: var(--color-lime); border-color: var(--color-lime); }
    .chip[data-status='RESOLVED'] { color: var(--color-brand-deep); }
    .chip[data-status='CLOSED'] { background: var(--color-paper-3); border-color: var(--color-paper-3); color: var(--color-ink-2); }
    .chip[data-status='REJECTED'] { color: var(--color-error); }
  `,
})
export class ComplaintCard {
  readonly complaint = input.required<Complaint>();
  /** English category name, the key for its translation. */
  readonly categoryName = input<string | null>(null);
  readonly towerName = input<string | null>(null);
  /** The flat label, e.g. "B2-503"; null for a common area. */
  readonly where = input<string | null>(null);

  private readonly lang = inject(LanguageService).current;
  /** The due time matters only while the work is still to be done; once resolved, the clock has stopped. */
  protected readonly ongoing = computed(() => isOngoing(this.complaint().status) && this.complaint().status !== 'RESOLVED');
  protected readonly overdue = computed(() => this.ongoing() && new Date(this.complaint().dueAt) < new Date());
  protected readonly due = computed(() => dateTime(this.complaint().dueAt, this.lang()));
  protected readonly raised = computed(() => relativeTime(this.complaint().raisedAt, this.lang()));
}
