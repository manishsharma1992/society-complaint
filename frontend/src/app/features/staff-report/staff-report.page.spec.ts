import { describe, expect, it } from 'vitest';
import { periodOf, slaShare } from './staff-report.page';

describe('staff report period', () => {
  const now = new Date(2026, 10, 7, 15, 0); // 7 Nov 2026, local time

  it('covers whole local months and years, end exclusive', () => {
    expect(periodOf('thisMonth', '', '', now)).toEqual({ from: new Date(2026, 10, 1), to: new Date(2026, 11, 1) });
    expect(periodOf('lastMonth', '', '', now)).toEqual({ from: new Date(2026, 9, 1), to: new Date(2026, 10, 1) });
    expect(periodOf('thisYear', '', '', now)).toEqual({ from: new Date(2026, 0, 1), to: new Date(2027, 0, 1) });
    expect(periodOf('lastYear', '', '', now)).toEqual({ from: new Date(2025, 0, 1), to: new Date(2026, 0, 1) });
  });

  it('crosses the year boundary for last month in January', () => {
    expect(periodOf('lastMonth', '', '', new Date(2027, 0, 15))).toEqual({ from: new Date(2026, 11, 1), to: new Date(2027, 0, 1) });
  });

  it('includes both custom dates and needs both, in order', () => {
    expect(periodOf('custom', '2026-11-01', '2026-11-07', now)).toEqual({ from: new Date(2026, 10, 1), to: new Date(2026, 10, 8) });
    expect(periodOf('custom', '2026-11-07', '2026-11-07', now)).toEqual({ from: new Date(2026, 10, 7), to: new Date(2026, 10, 8) });
    expect(periodOf('custom', '', '2026-11-07', now)).toBeNull();
    expect(periodOf('custom', '2026-11-08', '2026-11-07', now)).toBeNull();
  });

  it('measures on-time share among resolved complaints only', () => {
    const base = { staffId: 1, assigned: 5, started: 4, rejected: 0, medianMinutesToStart: null, medianMinutesToResolve: null, openNow: 1, overdueNow: 0 };
    expect(slaShare({ ...base, resolved: 4, resolvedWithinSla: 3 })).toBe(0.75);
    expect(slaShare({ ...base, resolved: 0, resolvedWithinSla: 0 })).toBeNull();
  });
});
