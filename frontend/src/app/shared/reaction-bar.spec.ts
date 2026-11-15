import { describe, expect, it } from 'vitest';
import { choiceAfterTap, shownCount } from './reaction-bar';

describe('reaction bar', () => {
  it('takes back your own emoji and moves to another one', () => {
    expect(choiceAfterTap(null, 'HEART')).toBe('HEART');
    expect(choiceAfterTap('HEART', 'HEART')).toBeNull();
    expect(choiceAfterTap('HEART', 'ANGRY')).toBe('ANGRY');
  });

  it('never shows your own pick as 0 while a cached count lags behind', () => {
    expect(shownCount({}, 'HEART', 'HEART')).toBe(1);
    expect(shownCount({ HEART: 4 }, 'HEART', 'HEART')).toBe(4);
    expect(shownCount({ HEART: 4 }, 'HEART', 'SAD')).toBe(0);
  });
});
