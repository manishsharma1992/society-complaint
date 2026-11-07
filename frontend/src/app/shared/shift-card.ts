import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { dateTime } from '../core/format';
import { LanguageService } from '../core/language';
import { ClockCapture, ClockEvidence, Shift, WorkforceApi } from '../core/workforce';
import { ClockCaptureSheet } from './clock-capture';

/**
 * A staff member's clock: on shift since when, or off shift, with the one button to change it. Clocking in or out
 * takes a live selfie and the phone's location (see ClockCaptureSheet), then says whether it was inside the society.
 * The open shift is found among the staff member's shifts of the last two weeks (a forgotten clock-out shows too).
 */
@Component({
  selector: 'app-shift-card',
  imports: [TranslocoDirective, HlmButtonImports, RouterLink, ClockCaptureSheet],
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
          @if (last(); as evidence) {
            <p class="where" [class.warn]="evidence.placement !== 'INSIDE'">
              @switch (evidence.placement) {
                @case ('INSIDE') { {{ t('shift.inside') }} }
                @case ('OUTSIDE') { {{ t('shift.outside', { km: km(evidence.distanceMeters) }) }} }
                @case ('NO_LOCATION') { {{ t('shift.noLocation') }} }
              }
            </p>
          }
          @if (state() === 'error') {
            <p class="error" role="alert">{{ t('shift.failed') }}</p>
          }
        </div>
        <div class="actions">
          @if (open()) {
            <button hlmBtn variant="outline" size="lg" type="button" [disabled]="busy()" (click)="capturing.set('end')">{{ t('shift.end') }}</button>
          } @else {
            <button hlmBtn size="lg" type="button" [disabled]="busy()" (click)="capturing.set('start')">{{ t('shift.start') }}</button>
          }
          <a routerLink="/staff" class="report">{{ t('shift.myReport') }}</a>
        </div>
      }
    </section>
    @if (capturing(); as mode) {
      <app-clock-capture [mode]="mode" (captured)="clock(mode, $event)" (cancelled)="capturing.set(null)" />
    }
  `,
  styles: `
    :host { display: block; margin-bottom: var(--space-md); }
    .shift { padding: var(--space-md); display: grid; gap: var(--space-sm); }
    .status { display: flex; align-items: center; gap: var(--space-xs); font-size: var(--text-lg); color: var(--color-ink); }
    .since, .where { color: var(--color-ink-2); }
    .where.warn { color: var(--color-error); }
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
  /** Where the last clock-in or clock-out of this visit happened, to tell the staff member right away. */
  protected readonly last = signal<ClockEvidence | null>(null);
  protected readonly capturing = signal<'start' | 'end' | null>(null);
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

  protected clock(mode: 'start' | 'end', capture: ClockCapture): void {
    this.capturing.set(null);
    this.busy.set(true);
    (mode === 'start' ? this.api.start(capture) : this.api.end(capture)).subscribe({
      next: (shift) => {
        this.open.set(shift.endedAt === null ? shift : null);
        this.last.set(mode === 'start' ? shift.start : shift.end);
        this.state.set('ready');
        this.busy.set(false);
      },
      error: () => {
        this.state.set('error');
        this.busy.set(false);
      },
    });
  }

  protected km(meters: number | null): string {
    return new Intl.NumberFormat(`${this.lang()}-IN-u-nu-latn`, { maximumFractionDigits: 1 }).format((meters ?? 0) / 1000);
  }
}
