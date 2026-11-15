import { ChangeDetectionStrategy, Component, inject, input, linkedSignal, signal } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { EMOJI, Emoji, FeedApi, Reactions } from '../core/feed';

const STORAGE_PREFIX = 'reaction:';

/**
 * One row of emoji under a post, with their counts. Anyone can react, signed in or not: one emoji per post, a tap
 * on another one moves it, a tap on your own takes it back. The server tells visitors apart; this device only
 * remembers which one it chose, to show it pressed.
 */
@Component({
  selector: 'app-reaction-bar',
  imports: [TranslocoDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="bar" role="group" *transloco="let t" [attr.aria-label]="t('reactions.label')">
      @for (emoji of emojis; track emoji.name) {
        <button
          type="button"
          class="press"
          [attr.aria-pressed]="mine() === emoji.name"
          [attr.aria-label]="t('reactions.' + emoji.name) + ', ' + count(emoji.name)"
          [disabled]="busy()"
          (click)="toggle(emoji.name)">
          <span class="glyph" aria-hidden="true">{{ emoji.glyph }}</span>
          @if (count(emoji.name) > 0) {
            <span class="count" aria-hidden="true">{{ count(emoji.name) }}</span>
          }
        </button>
      }
      @if (failed()) {
        <span class="warn" role="alert">{{ t('reactions.failed') }}</span>
      }
    </div>
  `,
  styles: `
    :host { display: block; }
    .bar { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2xs); }
    button {
      min-height: 44px;
      min-width: 44px;
      padding-inline: var(--space-xs);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      border: var(--rule-hair) solid var(--color-rule);
      border-radius: 999px;
      background: var(--color-stock);
      color: var(--color-ink-2);
      font: inherit;
      font-size: var(--text-sm);
      cursor: pointer;
    }
    button[aria-pressed='true'] {
      border-color: var(--color-brand);
      background: color-mix(in oklab, var(--color-brand) 12%, var(--color-stock));
      color: var(--color-brand-deep);
      font-weight: 500;
    }
    button:focus-visible { outline: 2px solid var(--color-focus); outline-offset: 2px; }
    button:disabled { cursor: progress; }
    .glyph { font-size: 1.15em; line-height: 1; }
    .count { font-variant-numeric: tabular-nums; }
    .warn { color: var(--color-error); font-size: var(--text-sm); }
  `,
})
export class ReactionBar {
  readonly slug = input.required<string>();
  readonly reactions = input.required<Reactions>();

  private readonly api = inject(FeedApi);
  protected readonly emojis = EMOJI;
  protected readonly counts = linkedSignal(() => this.reactions());
  protected readonly mine = linkedSignal<Emoji | null>(() => remembered(this.slug()));
  protected readonly busy = signal(false);
  protected readonly failed = signal(false);

  protected count(emoji: Emoji): number {
    return shownCount(this.counts(), this.mine(), emoji);
  }

  protected toggle(emoji: Emoji): void {
    const slug = this.slug();
    const next = choiceAfterTap(this.mine(), emoji);
    this.busy.set(true);
    this.failed.set(false);
    (next ? this.api.react(slug, next) : this.api.takeBackReaction(slug)).subscribe({
      next: (counts) => {
        this.counts.set(counts);
        this.mine.set(next);
        remember(slug, next);
        this.busy.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.busy.set(false);
      },
    });
  }
}

/** Public reads may be cached for a minute, so your own pick never shows as 0. */
export function shownCount(counts: Reactions, mine: Emoji | null, emoji: Emoji): number {
  return Math.max(counts[emoji] ?? 0, mine === emoji ? 1 : 0);
}

/** A tap on your own emoji takes it back; a tap on another one moves your reaction there. */
export function choiceAfterTap(mine: Emoji | null, tapped: Emoji): Emoji | null {
  return mine === tapped ? null : tapped;
}

function remembered(slug: string): Emoji | null {
  try {
    const value = localStorage.getItem(STORAGE_PREFIX + slug);
    return EMOJI.some((e) => e.name === value) ? (value as Emoji) : null;
  } catch {
    return null;
  }
}

function remember(slug: string, emoji: Emoji | null): void {
  try {
    if (emoji) {
      localStorage.setItem(STORAGE_PREFIX + slug, emoji);
    } else {
      localStorage.removeItem(STORAGE_PREFIX + slug);
    }
  } catch {
    // private mode or blocked storage: the server still has the reaction; only the pressed look is lost
  }
}
