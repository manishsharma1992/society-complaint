import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { FeedApi, FeedPost } from '../../core/feed';
import { FeedPostCard } from '../../shared/feed-post-card';

const PAGE_SIZE = 10;

/**
 * The public feed, no login needed: what residents of this society have documented, once a moderator has
 * published it. Anyone can react with an emoji and report a post; only signed-in residents post.
 */
@Component({
  selector: 'app-public-feed-page',
  imports: [TranslocoDirective, HlmButtonImports, FeedPostCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      <div class="head">
        <h1>{{ t('public.title') }}</h1>
        <p class="lead">{{ t('public.lead') }}</p>
      </div>

      @switch (state()) {
        @case ('error') {
          <div class="notice" role="alert">
            <p>{{ t('feed.failed') }}</p>
            <button hlmBtn variant="outline" size="lg" type="button" (click)="reload()">{{ t('report.retry') }}</button>
          </div>
        }
        @default {
          @if (posts().length === 0 && state() === 'ready') {
            <div class="empty">
              <h2>{{ t('feed.emptyTitle') }}</h2>
              <p>{{ t('feed.emptyBody') }}</p>
            </div>
          }
          <ul class="list">
            @for (post of posts(); track post.slug) {
              <li><app-feed-post-card [post]="post" base="/public" /></li>
            }
          </ul>
          @if (state() === 'loading') {
            <p class="muted" aria-busy="true">{{ t('feed.loading') }}</p>
          } @else if (hasMore()) {
            <button hlmBtn variant="outline" size="lg" type="button" class="more" (click)="loadMore()">{{ t('feed.more') }}</button>
          }
        }
      }
    </ng-container>
  `,
  styles: `
    :host { display: block; }
    .head { padding-block: var(--space-sm) var(--space-md); }
    h1 { font-size: var(--text-2xl); text-wrap: balance; }
    .lead { color: var(--color-ink-2); max-width: 60ch; }
    .notice { padding: var(--space-md) 0; color: var(--color-ink-2); display: grid; gap: var(--space-sm); justify-items: start; }
    .empty { padding: var(--space-xl) 0; display: grid; gap: var(--space-md); color: var(--color-ink-2); max-width: 60ch; }
    .empty h2 { font-size: var(--text-xl); color: var(--color-ink); }
    .list { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--space-md); align-items: start; }
    .muted { color: var(--color-ink-muted); padding-block: var(--space-md); }
    .more { margin-top: var(--space-md); }
    @media (min-width: 48rem) { .list { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
  `,
})
export default class PublicFeedPage {
  private readonly api = inject(FeedApi);

  protected readonly posts = signal<FeedPost[]>([]);
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly hasMore = signal(false);
  private page = 0;

  constructor() {
    this.fetch();
  }

  protected reload(): void {
    this.page = 0;
    this.posts.set([]);
    this.fetch();
  }

  protected loadMore(): void {
    this.page++;
    this.fetch();
  }

  private fetch(): void {
    this.state.set('loading');
    this.api.publicFeed(this.page, PAGE_SIZE).subscribe({
      next: (posts) => {
        this.posts.update((all) => [...all, ...posts]);
        this.hasMore.set(posts.length === PAGE_SIZE);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }
}
