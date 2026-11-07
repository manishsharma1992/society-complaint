import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { dateTime } from '../../core/format';
import { LanguageService } from '../../core/language';
import { People } from '../../core/people';
import { SessionStore } from '../../core/session';
import { ClockEvidence, Shift, WorkforceApi } from '../../core/workforce';

export type Range = 'today' | 'week' | 'month';
export type Show = 'attention' | 'unreviewed' | 'all';

/** [from, to) in local time for a range ending now (today, the last 7 days, this calendar month). */
export function rangeOf(range: Range, now = new Date()): { from: Date; to: Date } {
  const to = new Date(now.getTime() + 60_000);
  switch (range) {
    case 'today': return { from: new Date(now.getFullYear(), now.getMonth(), now.getDate()), to };
    case 'week': return { from: new Date(now.getFullYear(), now.getMonth(), now.getDate() - 6), to };
    case 'month': return { from: new Date(now.getFullYear(), now.getMonth(), 1), to };
  }
}

/** Which shifts a filter keeps: those flagged, those nobody has reviewed yet (ended only), or all. */
export function keeps(show: Show, shift: Shift): boolean {
  switch (show) {
    case 'attention': return shift.needsAttention;
    case 'unreviewed': return shift.endedAt !== null && shift.review === null;
    case 'all': return true;
  }
}

/**
 * Admins check staff shifts: is it really the staff member in both selfies, and were they at the society? Then they
 * confirm the shift or dispute it with a reason. Nobody can review their own shift (the server refuses it too).
 */
@Component({
  selector: 'app-shifts-page',
  imports: [TranslocoDirective, HlmButtonImports, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shifts.page.html',
  styleUrl: './shifts.page.css',
})
export default class ShiftsPage {
  private readonly api = inject(WorkforceApi);
  private readonly people = inject(People);
  private readonly lang = inject(LanguageService).current;
  private readonly me = inject(SessionStore).user;

  protected readonly ranges: Range[] = ['today', 'week', 'month'];
  protected readonly shows: Show[] = ['attention', 'unreviewed', 'all'];
  protected readonly range = signal<Range>('week');
  protected readonly show = signal<Show>('unreviewed');
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly shifts = signal<Shift[]>([]);
  protected readonly visible = computed(() => this.shifts().filter((s) => keeps(this.show(), s)));
  /** The shift whose dispute reason is being written, and the text so far. */
  protected readonly disputing = signal<number | null>(null);
  protected readonly reason = signal('');
  protected readonly saving = signal<number | null>(null);
  protected readonly failed = signal<number | null>(null);

  constructor() {
    effect(() => this.load(this.range()));
  }

  protected load(range: Range = this.range()): void {
    const { from, to } = rangeOf(range);
    this.state.set('loading');
    this.api.list({ from: from.toISOString(), to: to.toISOString() }).subscribe({
      next: (shifts) => {
        this.shifts.set(shifts);
        this.people.ensure(shifts.flatMap((s) => [s.staffId, ...(s.review ? [s.review.reviewerId] : [])]));
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected nameOf(id: number): string {
    return this.people.names()[id] ?? `#${id}`;
  }

  protected isMine(shift: Shift): boolean {
    return shift.staffId === this.me()?.id;
  }

  protected when(iso: string): string {
    return dateTime(iso, this.lang());
  }

  protected hours(minutes: number): string {
    return new Intl.NumberFormat(`${this.lang()}-IN-u-nu-latn`, { maximumFractionDigits: 1 }).format(minutes / 60);
  }

  protected km(meters: number | null): string {
    return new Intl.NumberFormat(`${this.lang()}-IN-u-nu-latn`, { maximumFractionDigits: 1 }).format((meters ?? 0) / 1000);
  }

  protected round(n: number): number {
    return Math.round(n);
  }

  /** A map of where the phone was, to compare with the selfie's background. */
  protected mapUrl(e: ClockEvidence): string | null {
    return e.latitude === null || e.longitude === null ? null : `https://www.google.com/maps?q=${e.latitude},${e.longitude}`;
  }

  protected confirm(shift: Shift): void {
    this.send(shift, 'CONFIRM', null);
  }

  protected startDispute(shift: Shift): void {
    this.disputing.set(shift.id);
    this.reason.set('');
  }

  protected sendDispute(shift: Shift): void {
    const reason = this.reason().trim();
    if (reason) this.send(shift, 'DISPUTE', reason);
  }

  private send(shift: Shift, decision: 'CONFIRM' | 'DISPUTE', reason: string | null): void {
    this.saving.set(shift.id);
    this.failed.set(null);
    this.api.review(shift.id, decision, reason).subscribe({
      next: (updated) => {
        this.shifts.update((all) => all.map((s) => (s.id === updated.id ? updated : s)));
        this.people.ensure(updated.review ? [updated.review.reviewerId] : []);
        this.disputing.set(null);
        this.saving.set(null);
      },
      error: () => {
        this.failed.set(shift.id);
        this.saving.set(null);
      },
    });
  }
}
