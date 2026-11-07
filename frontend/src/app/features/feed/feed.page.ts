import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucidePlus } from '@ng-icons/lucide';
import { FeedApi, FeedPost } from '../../core/feed';
import { Roles } from '../../core/roles';
import { SessionStore } from '../../core/session';
import { FeedPostCard } from '../../shared/feed-post-card';

const PAGE_SIZE = 10;

/**
 * The home page: what residents of this society and the societies nearby have documented, once a moderator has
 * published it. Residents start a new post here; the society admin sees what waits for review.
 */
@Component({
  selector: 'app-feed-page',
  imports: [TranslocoDirective, RouterLink, HlmButtonImports, NgIcon, FeedPostCard],
  providers: [provideIcons({ lucidePlus })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      <div class="head">
        <div>
          <h1>{{ t('feed.title') }}</h1>
          <p class="lead">{{ t('feed.lead') }}</p>
        </div>
        @if (canPost()) {
          <a hlmBtn size="xl" routerLink="/feed/new" class="desktop"><ng-icon name="lucidePlus" aria-hidden="true" />{{ t('feed.newPost') }}</a>
        }
      </div>

      <nav class="links" [attr.aria-label]="t('feed.title')">
        @if (canPost()) {
          <a routerLink="/feed/mine">{{ t('feed.myPosts') }}</a>
        }
        @if (roles.isSocietyAdmin()) {
          <a routerLink="/feed/moderation" class="moderation">
            {{ waiting() > 0 ? t('feed.waiting', { count: waiting() }) : t('feed.moderation') }}
          </a>
        }
      </nav>

      @if (user()?.status !== 'ACTIVE') {
        <p class="notice stock">{{ t('feed.notApproved') }}</p>
      } @else {
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
                @if (canPost()) {
                  <a hlmBtn size="lg" routerLink="/feed/new">{{ t('feed.firstPost') }}</a>
                }
              </div>
            }
            <ul class="list">
              @for (post of posts(); track post.slug) {
                <li><app-feed-post-card [post]="post" /></li>
              }
            </ul>
            @if (state() === 'loading') {
              <p class="muted" aria-busy="true">{{ t('feed.loading') }}</p>
            } @else if (hasMore()) {
              <button hlmBtn variant="outline" size="lg" type="button" class="more" (click)="loadMore()">{{ t('feed.more') }}</button>
            }
          }
        }
      }

      @if (canPost()) {
        <div class="dock">
          <a hlmBtn size="xl" routerLink="/feed/new" class="w-full"><ng-icon name="lucidePlus" aria-hidden="true" />{{ t('feed.newPost') }}</a>
        </div>
      }
    </ng-container>
  `,
  styles: `
    :host { display: block; padding-bottom: 88px; }
    .head { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-md); padding-block: var(--space-sm) var(--space-xs); }
    h1 { font-size: var(--text-2xl); text-wrap: balance; }
    .lead { color: var(--color-ink-2); max-width: 60ch; }
    .links { display: flex; flex-wrap: wrap; gap: var(--space-2xs) var(--space-lg); margin-bottom: var(--space-sm); }
    .links a { min-height: 44px; display: inline-flex; align-items: center; color: var(--color-brand-deep); font-weight: 500; }
    .moderation { color: var(--color-ink) !important; }
    .notice { padding: var(--space-md); color: var(--color-ink-2); display: grid; gap: var(--space-sm); justify-items: start; }
    .empty { padding: var(--space-xl) 0; display: grid; gap: var(--space-md); justify-items: start; color: var(--color-ink-2); max-width: 60ch; }
    .empty h2 { font-size: var(--text-xl); color: var(--color-ink); }
    .list { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--space-md); align-items: start; }
    .muted { color: var(--color-ink-muted); padding-block: var(--space-md); }
    .more { margin-top: var(--space-md); }
    .desktop { display: none; }
    .dock {
      position: fixed;
      inset: auto 0 calc(57px + env(safe-area-inset-bottom));
      z-index: var(--z-sticky);
      padding: var(--space-sm) var(--gutter);
      background: linear-gradient(to top, var(--color-paper) 70%, transparent);
    }
    @media (min-width: 48rem) { .list { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (min-width: 60rem) {
      :host { padding-bottom: 0; }
      .dock { display: none; }
      .desktop { display: inline-flex; }
    }
  `,
})
export default class FeedPage {
  private readonly api = inject(FeedApi);
  protected readonly roles = inject(Roles);
  protected readonly user = inject(SessionStore).user;

  protected readonly posts = signal<FeedPost[]>([]);
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly hasMore = signal(false);
  protected readonly waiting = signal(0);
  protected readonly canPost = () => this.user()?.status === 'ACTIVE' && this.roles.isResident();
  private page = 0;

  constructor() {
    if (this.user()?.status === 'ACTIVE') {
      this.fetch();
    }
    if (this.roles.isSocietyAdmin()) {
      this.api.queue().subscribe({ next: (queue) => this.waiting.set(queue.length), error: () => {} });
    }
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
    this.api.feed(this.page, PAGE_SIZE).subscribe({
      next: (posts) => {
        this.posts.update((all) => [...all, ...posts]);
        this.hasMore.set(posts.length === PAGE_SIZE);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }
}
