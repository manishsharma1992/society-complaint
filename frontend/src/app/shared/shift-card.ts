import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { dateTime } from '../core/format';
import { LanguageService } from '../core/language';
import { Shift, WorkforceApi } from '../core/workforce';

/**
 * A staff member's clock: on shift since when, or off shift, with the one button to change it.
 * The open shift is found among the staff member's shifts of the last two weeks (a forgotten clock-out shows too).
 */
@Component({
  selector: 'app-shift-card',
  imports: [TranslocoDirective, HlmButtonImports, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="shift stock" *transloco="let t" [attr.aria-label]="t('shift.title')" aria-live="polite">
      @if (state() === 'loading') {
        <p class="status muted">{{ t('shift.loading') }}</p>
      } @else {
        <div class="text">
          @if (open(); as shift) {
            <p class="status"><span class="dot on" aria-hidden="true"></span>{{ t('shift.on') }}</p>
            <p class="since">{{ t('shift.since', { time: since() }) }}</p>
          } @else {
            <p class="status"><span class="dot" aria-hidden="true"></span>{{ t('shift.off') }}</p>
            <p class="since">{{ t('shift.offHint') }}</p>
          }
          @if (state() === 'error') {
            <p class="error" role="alert">{{ t('shift.failed') }}</p>
          }
        </div>
        <div class="actions">
          @if (open()) {
            <button hlmBtn variant="outline" size="lg" type="button" [disabled]="busy()" (click)="end()">{{ t('shift.end') }}</button>
          } @else {
            <button hlmBtn size="lg" type="button" [disabled]="busy()" (click)="start()">{{ t('shift.start') }}</button>
          }
          <a routerLink="/staff" class="report">{{ t('shift.myReport') }}</a>
        </div>
      }
    </section>
  `,
  styles: `
    :host { display: block; margin-bottom: var(--space-md); }
    .shift { padding: var(--space-md); display: grid; gap: var(--space-sm); }
    .status { display: flex; align-items: center; gap: var(--space-xs); font-size: var(--text-lg); color: var(--color-ink); }
    .since { color: var(--color-ink-2); }
    .muted { color: var(--color-ink-muted); }
    .error { color: var(--color-error); margin-top: var(--space-2xs); }
    .dot { width: 10px; height: 10px; border-radius: 50%; background: var(--color-ink-muted); flex: none; }
    .dot.on { background: var(--color-success); }
    .actions { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-md); }
    .report { min-height: 44px; display: inline-flex; align-items: center; color: var(--color-brand-deep); }
    @media (min-width: 48rem) {
      .shift { grid-template-columns: 1fr auto; align-items: center; }
    }
  `,
})
export class ShiftCard {
  private readonly api = inject(WorkforceApi);
  private readonly lang = inject(LanguageService).current;

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly busy = signal(false);
  protected readonly open = signal<Shift | null>(null);
  protected readonly since = computed(() => {
    const shift = this.open();
    return shift ? dateTime(shift.startedAt, this.lang()) : '';
  });

  constructor() {
    const to = new Date(Date.now() + 60_000);
    const from = new Date(to.getTime() - 14 * 24 * 3600_000);
    this.api.mine({ from: from.toISOString(), to: to.toISOString() }).subscribe({
      next: (shifts) => {
        this.open.set(shifts.find((s) => s.endedAt === null) ?? null);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected start(): void {
    this.change(this.api.start());
  }

  protected end(): void {
    this.change(this.api.end());
  }

  private change(request: ReturnType<WorkforceApi['start']>): void {
    this.busy.set(true);
    request.subscribe({
      next: (shift) => {
        this.open.set(shift.endedAt === null ? shift : null);
        this.state.set('ready');
        this.busy.set(false);
      },
      error: () => {
        this.state.set('error');
        this.busy.set(false);
      },
    });
  }
}
