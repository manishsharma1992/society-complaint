import { describe, expect, it } from 'vitest';
import { Shift } from '../../core/workforce';
import { keeps, rangeOf } from './shifts.page';

const base: Shift = {
  id: 1, staffId: 3, startedAt: '2026-11-07T00:30:00Z', endedAt: '2026-11-07T08:30:00Z', minutesCounted: 480,
  start: null, end: null, review: null, needsAttention: false,
};

describe('shift review filters', () => {
  it('keeps flagged shifts, unreviewed ended shifts, or everything', () => {
    const flagged = { ...base, needsAttention: true };
    const reviewed = { ...base, review: { decision: 'CONFIRMED' as const, reviewerId: 2, reviewedAt: '2026-11-07T09:00:00Z', reason: null } };
    const open = { ...base, endedAt: null };

    expect([base, flagged, reviewed, open].filter((s) => keeps('attention', s))).toEqual([flagged]);
    expect([base, flagged, reviewed, open].filter((s) => keeps('unreviewed', s))).toEqual([base, flagged]);
    expect([base, flagged, reviewed, open].filter((s) => keeps('all', s))).toHaveLength(4);
  });

  it('builds local ranges that end just after now', () => {
    const now = new Date(2026, 10, 7, 15, 0);
    expect(rangeOf('today', now).from).toEqual(new Date(2026, 10, 7));
    expect(rangeOf('week', now).from).toEqual(new Date(2026, 10, 1));
    expect(rangeOf('month', now).from).toEqual(new Date(2026, 10, 1));
    expect(rangeOf('month', now).to.getTime()).toBeGreaterThan(now.getTime());
  });
});
