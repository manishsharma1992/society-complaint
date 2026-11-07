import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { Observable, forkJoin, map, of } from 'rxjs';
import { ComplaintsApi, StaffPerformance } from '../../core/complaints';
import { dateTime } from '../../core/format';
import { LanguageService } from '../../core/language';
import { Tower } from '../../core/models';
import { People, Person } from '../../core/people';
import { ResidencyApi } from '../../core/residency';
import { Roles } from '../../core/roles';
import { SessionStore } from '../../core/session';
import { Shift, ShiftHours, WorkforceApi } from '../../core/workforce';

export type Preset = 'thisMonth' | 'lastMonth' | 'thisYear' | 'lastYear' | 'custom';
export type SortKey = 'name' | 'assigned' | 'resolved' | 'sla' | 'toResolve' | 'overdue' | 'hours';

export interface Row {
  staffId: number;
  name: string;
  onDuty: boolean;
  perf: StaffPerformance;
  shifts: number;
  minutesWorked: number;
}

const ZERO = (staffId: number): StaffPerformance => ({
  staffId, assigned: 0, started: 0, resolved: 0, rejected: 0, medianMinutesToStart: null,
  medianMinutesToResolve: null, resolvedWithinSla: 0, openNow: 0, overdueNow: 0,
});

/** [from, to) in local time (the society's time zone) for a preset; custom uses the two dates, both inclusive. */
export function periodOf(preset: Preset, customFrom: string, customTo: string, now = new Date()): { from: Date; to: Date } | null {
  const y = now.getFullYear();
  const m = now.getMonth();
  switch (preset) {
    case 'thisMonth': return { from: new Date(y, m, 1), to: new Date(y, m + 1, 1) };
    case 'lastMonth': return { from: new Date(y, m - 1, 1), to: new Date(y, m, 1) };
    case 'thisYear': return { from: new Date(y, 0, 1), to: new Date(y + 1, 0, 1) };
    case 'lastYear': return { from: new Date(y - 1, 0, 1), to: new Date(y, 0, 1) };
    case 'custom': {
      if (!customFrom || !customTo) return null;
      const [fy, fm, fd] = customFrom.split('-').map(Number);
      const [ty, tm, td] = customTo.split('-').map(Number);
      const from = new Date(fy, fm - 1, fd);
      const to = new Date(ty, tm - 1, td + 1);
      return to > from ? { from, to } : null;
    }
  }
}

/** Share of resolved complaints that met the SLA, or null when nothing was resolved. */
export const slaShare = (p: StaffPerformance) => (p.resolved === 0 ? null : p.resolvedWithinSla / p.resolved);

/**
 * The staff report: per maintenance staff member, the complaints they were given in a period, how fast and how
 * reliably they fixed them, what is still open, and the hours they were on shift. Admins see everyone (a tower
 * admin only their tower); staff see themselves. The server decides who may see what.
 */
@Component({
  selector: 'app-staff-report-page',
  imports: [TranslocoDirective, HlmButtonImports, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './staff-report.page.html',
  styleUrl: './staff-report.page.css',
})
export default class StaffReportPage {
  private readonly complaintsApi = inject(ComplaintsApi);
  private readonly workforce = inject(WorkforceApi);
  private readonly people = inject(People);
  private readonly residency = inject(ResidencyApi);
  private readonly lang = inject(LanguageService).current;
  private readonly me = inject(SessionStore).user;
  private readonly transloco = inject(TranslocoService);
  protected readonly roles = inject(Roles);

  protected readonly presets: Preset[] = ['thisMonth', 'lastMonth', 'thisYear', 'lastYear', 'custom'];
  protected readonly sortKeys: SortKey[] = ['resolved', 'assigned', 'sla', 'toResolve', 'overdue', 'hours', 'name'];

  protected readonly preset = signal<Preset>('thisMonth');
  protected readonly customFrom = signal('');
  protected readonly customTo = signal('');
  /** null: the whole society (society admin only). */
  protected readonly towerId = signal<number | null>(this.roles.isSocietyAdmin() ? null : (this.roles.adminTowers()[0] ?? null));
  protected readonly sortKey = signal<SortKey>('resolved');
  protected readonly descending = signal(true);

  protected readonly state = signal<'loading' | 'ready' | 'error' | 'needsDates'>('loading');
  protected readonly rows = signal<Row[]>([]);
  protected readonly onDuty = signal<Shift[]>([]);
  protected readonly towers = signal<Tower[]>([]);

  /** Admins compare everyone; staff only see their own numbers. */
  protected readonly seesEveryone = this.roles.isAdmin;
  /** Shift hours are society-wide, so jobs per shift only make sense for the whole society. */
  protected readonly showsPerShift = computed(() => this.towerId() === null);
  protected readonly towerChoices = computed(() =>
    this.roles.isSocietyAdmin() ? this.towers() : this.towers().filter((t) => this.roles.adminTowers().includes(t.id)),
  );

  protected readonly sorted = computed(() => {
    const key = this.sortKey();
    const sign = this.descending() ? -1 : 1;
    const value = (row: Row): number | string => {
      switch (key) {
        case 'name': return row.name.toLocaleLowerCase();
        case 'assigned': return row.perf.assigned;
        case 'resolved': return row.perf.resolved;
        case 'sla': return slaShare(row.perf) ?? -1;
        case 'toResolve': return row.perf.medianMinutesToResolve ?? Number.MAX_SAFE_INTEGER;
        case 'overdue': return row.perf.overdueNow;
        case 'hours': return row.minutesWorked;
      }
    };
    return [...this.rows()].sort((a, b) => {
      const va = value(a);
      const vb = value(b);
      const order = typeof va === 'string' ? va.localeCompare(vb as string) : va - (vb as number);
      return order * sign || a.name.localeCompare(b.name);
    });
  });

  protected readonly totals = computed(() => {
    const rows = this.rows();
    const sum = (f: (r: Row) => number) => rows.reduce((total, r) => total + f(r), 0);
    const resolved = sum((r) => r.perf.resolved);
    return {
      assigned: sum((r) => r.perf.assigned),
      resolved,
      sla: resolved === 0 ? null : sum((r) => r.perf.resolvedWithinSla) / resolved,
      overdue: sum((r) => r.perf.overdueNow),
      minutesWorked: sum((r) => r.minutesWorked),
    };
  });

  constructor() {
    if (this.seesEveryone()) {
      this.residency.towers().subscribe({ next: (towers) => this.towers.set(towers), error: () => {} });
    }
    effect(() => this.load());
  }

  protected load(): void {
    const period = periodOf(this.preset(), this.customFrom(), this.customTo());
    const towerId = this.towerId();
    if (!period) {
      this.state.set('needsDates');
      return;
    }
    const range = { from: period.from.toISOString(), to: period.to.toISOString() };
    this.state.set('loading');
    const staff$: Observable<Person[]> = this.seesEveryone()
      ? this.people.staff()
      : of(this.me() ? [{ id: this.me()!.id, name: this.me()!.name ?? '' }] : []);
    forkJoin({
      report: this.complaintsApi.staffReport(range.from, range.to, towerId),
      hours: this.workforce.hours(range),
      staff: staff$,
      onDuty: this.seesEveryone() ? this.workforce.onDuty() : of([] as Shift[]),
    })
      .pipe(map(({ report, hours, staff, onDuty }) => ({ rows: this.merge(report, hours, staff), onDuty })))
      .subscribe({
        next: ({ rows, onDuty }) => {
          this.rows.set(rows);
          this.onDuty.set(onDuty);
          this.people.ensure(onDuty.map((s) => s.staffId));
          this.state.set('ready');
        },
        error: () => this.state.set('error'),
      });
  }

  /** Every staff member gets a row, even without complaints or shifts in the period. */
  private merge(report: StaffPerformance[], hours: ShiftHours[], staff: Person[]): Row[] {
    const byId = new Map<number, Row>();
    const row = (id: number) => {
      let r = byId.get(id);
      if (!r) {
        r = { staffId: id, name: '', onDuty: false, perf: ZERO(id), shifts: 0, minutesWorked: 0 };
        byId.set(id, r);
      }
      return r;
    };
    staff.forEach((p) => (row(p.id).name = p.name));
    report.forEach((p) => (row(p.staffId).perf = p));
    hours.forEach((h) => Object.assign(row(h.staffId), { shifts: h.shifts, minutesWorked: h.minutesWorked, onDuty: h.onDutyNow }));
    const rows = [...byId.values()];
    this.people.ensure(rows.filter((r) => !r.name).map((r) => r.staffId));
    return rows;
  }

  protected nameOf(row: { staffId: number; name?: string }): string {
    return row.name || this.people.names()[row.staffId] || `#${row.staffId}`;
  }

  protected sortBy(key: SortKey): void {
    if (this.sortKey() === key) {
      this.descending.update((d) => !d);
    } else {
      this.sortKey.set(key);
      this.descending.set(key !== 'name' && key !== 'toResolve');
    }
  }

  protected ariaSort(key: SortKey): 'ascending' | 'descending' | null {
    return this.sortKey() === key ? (this.descending() ? 'descending' : 'ascending') : null;
  }

  protected onTower(value: string): void {
    this.towerId.set(value === '' ? null : Number(value));
  }

  protected percent(share: number | null): string {
    return share === null ? '—' : new Intl.NumberFormat(this.locale(), { style: 'percent', maximumFractionDigits: 0 }).format(share);
  }

  protected hours(minutes: number): string {
    return new Intl.NumberFormat(this.locale(), { maximumFractionDigits: 1 }).format(minutes / 60);
  }

  /** Resolved complaints per 8-hour shift worked, or null without shift hours. */
  protected perShift(row: Row): string {
    if (row.minutesWorked < 60) return '—';
    return new Intl.NumberFormat(this.locale(), { maximumFractionDigits: 1 }).format(row.perf.resolved / (row.minutesWorked / 480));
  }

  protected slaOf(row: Row): number | null {
    return slaShare(row.perf);
  }

  /** "2 d 4 h", "3 h 20 min", "45 min" (the two largest units); a dash when there is nothing to measure. */
  protected duration(minutes: number | null): string {
    if (minutes === null) return '—';
    const units: [number, string][] = [
      [Math.floor(minutes / 1440), 'd'],
      [Math.floor((minutes % 1440) / 60), 'h'],
      [minutes % 60, 'min'],
    ];
    const first = units.findIndex(([n]) => n > 0);
    if (first === -1) return this.transloco.translate('report.unit.min', { n: 0 });
    return units
      .slice(first, first + 2)
      .filter(([n]) => n > 0)
      .map(([n, unit]) => this.transloco.translate('report.unit.' + unit, { n }))
      .join(' ');
  }

  protected since(iso: string): string {
    return dateTime(iso, this.lang());
  }

  private locale(): string {
    return `${this.lang()}-IN-u-nu-latn`;
  }
}
