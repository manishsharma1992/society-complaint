import { describe, expect, it } from 'vitest';
import { Viewer, sectionsFor } from './help.page';

const nobody: Viewer = {
  active: true, resident: false, staff: false, admin: false, societyAdmin: false, platformOwner: false,
  feedOn: true, complaintsOn: true,
};
const ids = (v: Partial<Viewer>) => sectionsFor({ ...nobody, ...v }).map((s) => s.id);

describe('help sections', () => {
  it('shows each role its own how-tos', () => {
    expect(ids({ resident: true })).toEqual(['start', 'feed', 'post', 'complaint', 'privacy']);
    expect(ids({ staff: true })).toEqual(['start', 'feed', 'complaint', 'jobs', 'shift', 'privacy']);
    expect(ids({ admin: true, societyAdmin: true })).toEqual(
      ['start', 'feed', 'complaint', 'assign', 'review', 'moderate', 'signups', 'addons', 'privacy']);
  });

  it('hides add-ons the society has switched off', () => {
    expect(ids({ staff: true, complaintsOn: false })).toEqual(['start', 'feed', 'privacy']);
    expect(ids({ resident: true, feedOn: false })).toEqual(['start', 'complaint', 'privacy']);
  });

  it('shows only getting started and privacy before approval', () => {
    expect(ids({ active: false, resident: true })).toEqual(['start', 'privacy']);
  });
});
