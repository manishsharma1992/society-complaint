import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { dateTime } from '../../core/format';
import { FeedApi, OwnPost } from '../../core/feed';
import { LanguageService } from '../../core/language';

/** A resident's own posts and where each stands: waiting for review, published, or not published and why. */
@Component({
  selector: 'app-my-posts-page',
  imports: [TranslocoDirective, RouterLink, HlmButtonImports],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      <a routerLink="/feed" class="back">{{ t('post.back') }}</a>
      <h1>{{ t('myPosts.title') }}</h1>
      @if (sent) {
        <p class="sent stock" role="status">{{ t('myPosts.sent') }}</p>
      }
      @switch (state()) {
        @case ('loading') { <p class="muted" aria-busy="true">{{ t('feed.loading') }}</p> }
        @case ('error') { <p class="warn" role="alert">{{ t('feed.failed') }}</p> }
        @case ('ready') {
          @if (posts().length === 0) {
            <div class="empty">
              <p>{{ t('myPosts.empty') }}</p>
              <a hlmBtn size="lg" routerLink="/feed/new">{{ t('feed.newPost') }}</a>
            </div>
          }
          <ul class="list ruled">
            @for (post of posts(); track post.id) {
              <li class="row">
                <div class="top">
                  <h2>{{ post.title }}</h2>
                  <span class="badge" [attr.data-status]="post.status">{{ t('myPosts.status.' + post.status) }}</span>
                </div>
                <p class="muted small">{{ when(post.submittedAt) }} · {{ t('myPosts.photos', { count: post.evidence.length }) }}</p>
                @for (video of post.videos; track $index) {
                  <p class="small" [class.reason]="video.status === 'FAILED'" [class.muted]="video.status !== 'FAILED'">
                    {{ t('myPosts.video.' + video.status, { reason: video.failureReason }) }}
                  </p>
                }
                @if (reasonOf(post); as reason) {
                  <p class="reason">{{ t('myPosts.reason', { reason: reason }) }}</p>
                }
                @if (post.status === 'PUBLISHED') {
                  <a [routerLink]="['/feed', post.slug]" class="open">{{ t('myPosts.open') }}</a>
                }
              </li>
            }
          </ul>
        }
      }
    </ng-container>
  `,
  styles: `
    :host { display: block; padding-top: var(--space-xs); max-width: 46rem; }
    .back { min-height: 44px; display: inline-flex; align-items: center; color: var(--color-brand-deep); }
    h1 { font-size: var(--text-2xl); margin-bottom: var(--space-md); }
    .sent { padding: var(--space-md); margin-bottom: var(--space-md); color: var(--color-ink-2); }
    .list { list-style: none; margin: 0; padding: 0; }
    .row { padding-block: var(--space-sm); display: grid; gap: var(--space-2xs); }
    .top { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--space-sm); }
    h2 { font-size: var(--text-lg); }
    .badge { font-size: var(--text-sm); padding: 2px var(--space-xs); border-radius: var(--radius-pill); background: var(--color-paper-2); color: var(--color-ink-2); white-space: nowrap; }
    .badge[data-status='PUBLISHED'] { background: color-mix(in oklab, var(--color-success) 18%, var(--color-paper)); color: var(--color-ink); }
    .badge[data-status='REJECTED'], .badge[data-status='REMOVED'] { background: color-mix(in oklab, var(--color-error) 15%, var(--color-paper)); color: var(--color-error); }
    .reason { color: var(--color-ink-2); }
    .open { min-height: 44px; display: inline-flex; align-items: center; color: var(--color-brand-deep); }
    .empty { display: grid; gap: var(--space-md); justify-items: start; color: var(--color-ink-2); padding-block: var(--space-md); }
    .muted { color: var(--color-ink-muted); }
    .small { font-size: var(--text-sm); }
    .warn { color: var(--color-error); }
  `,
})
export default class MyPostsPage {
  private readonly lang = inject(LanguageService).current;
  protected readonly posts = signal<OwnPost[]>([]);
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  /** Set when arriving right after sending a post. */
  protected readonly sent = history.state?.sent === true;

  constructor() {
    inject(FeedApi).myPosts().subscribe({
      next: (posts) => {
        this.posts.set(posts);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected when(iso: string): string {
    return dateTime(iso, this.lang());
  }

  /** Why the moderator rejected, hid or removed it, from the latest moderation entry. */
  protected reasonOf(post: OwnPost): string | null {
    if (post.status === 'PUBLISHED' || post.status === 'PENDING_REVIEW') return null;
    return [...post.moderation].reverse().find((m) => m.reason)?.reason ?? null;
  }
}
