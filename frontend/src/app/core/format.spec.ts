import { describe, expect, it } from 'vitest';
import { clipLength } from './format';

describe('clipLength', () => {
  it('shows a video length as m:ss, rounded to the second', () => {
    expect(clipLength(6_000)).toBe('0:06');
    expect(clipLength(59_600)).toBe('1:00');
    expect(clipLength(61_000)).toBe('1:01');
  });
});
