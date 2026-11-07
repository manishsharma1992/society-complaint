import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { relativeTime } from '../core/format';
import { FeedPost } from '../core/feed';
import { LanguageService } from '../core/language';

/** A published post in the feed: which society (and tower), when, what, and up to three photos. Opens the post. */
@Component({
  selector: 'app-feed-post-card',
  imports: [TranslocoDirective, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="card stock press" [routerLink]="['/feed', post().slug]" *transloco="let t">
      <p class="meta">
        <span class="who">{{ t('feed.verified') }}</span>
        <span>{{ post().society }}{{ post().tower ? ' · ' + post().tower : '' }}</span>
        <span class="when">{{ published() }}</span>
      </p>
      <h2>{{ post().title }}</h2>
      <p class="body">{{ post().body }}</p>
      @if (post().photos.length > 0) {
        <div class="photos" [attr.data-count]="shown().length">
          @for (photo of shown(); track photo.url; let last = $last) {
            <span class="photo">
              <img [src]="photo.url" alt="" loading="lazy" [width]="photo.widthPx" [height]="photo.heightPx" />
              @if (last && more() > 0) {
                <span class="more">+{{ more() }}</span>
              }
            </span>
          }
        </div>
        <span class="sr-only">{{ t('feed.photoCount', { count: post().photos.length }) }}</span>
      }
    </a>
  `,
  styles: `
    :host { display: block; }
    .card { display: grid; gap: var(--space-xs); padding: var(--space-md); color: inherit; text-decoration: none; }
    .card:focus-visible { outline: 2px solid var(--color-focus); outline-offset: 2px; }
    .meta { display: flex; flex-wrap: wrap; gap: var(--space-2xs) var(--space-sm); font-size: var(--text-sm); color: var(--color-ink-muted); }
    .who { color: var(--color-brand-deep); font-weight: 500; }
    .when { margin-inline-start: auto; }
    h2 { font-size: var(--text-lg); color: var(--color-ink); text-wrap: balance; }
    .body { color: var(--color-ink-2); display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; }
    .photos { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-2xs); }
    .photos[data-count='1'] { grid-template-columns: 1fr; }
    .photos[data-count='2'] { grid-template-columns: 1fr 1fr; }
    .photo { position: relative; aspect-ratio: 4 / 3; border-radius: 8px; overflow: hidden; background: var(--color-paper-2); }
    .photos[data-count='1'] .photo { aspect-ratio: 16 / 9; }
    .photo img { width: 100%; height: 100%; object-fit: cover; display: block; }
    .more { position: absolute; inset: 0; display: grid; place-items: center; background: color-mix(in oklab, var(--color-ink) 50%, transparent); color: var(--color-paper); font-size: var(--text-lg); }
    .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
  `,
})
export class FeedPostCard {
  readonly post = input.required<FeedPost>();
  private readonly lang = inject(LanguageService).current;

  protected readonly published = computed(() => relativeTime(this.post().publishedAt, this.lang()));
  protected readonly shown = computed(() => this.post().photos.slice(0, 3));
  protected readonly more = computed(() => Math.max(0, this.post().photos.length - 3));
}
