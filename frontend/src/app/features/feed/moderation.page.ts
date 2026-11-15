import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { clipLength, dateTime } from '../../core/format';
import { Decision, FeedApi, OwnPost } from '../../core/feed';
import { LanguageService } from '../../core/language';

type Queue = 'PENDING_REVIEW' | 'HIDDEN';

/**
 * The society admin checks posts against the content policy before anyone sees them, and decides on posts that
 * readers' reports have hidden. Every decision, with its reason, goes to the append-only moderation log.
 */
@Component({
  selector: 'app-moderation-page',
  imports: [TranslocoDirective, RouterLink, HlmButtonImports],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      <a routerLink="/feed" class="back">{{ t('post.back') }}</a>
      <h1>{{ t('moderation.title') }}</h1>
      <p class="lead">{{ t('moderation.lead') }}</p>

      <div class="tabs" role="tablist">
        @for (q of queues; track q) {
          <button type="button" role="tab" [attr.aria-selected]="queue() === q" (click)="queue.set(q)">{{ t('moderation.queue.' + q) }}</button>
        }
      </div>

      @switch (state()) {
        @case ('loading') { <p class="muted" aria-busy="true">{{ t('feed.loading') }}</p> }
        @case ('error') { <p class="warn" role="alert">{{ t('feed.failed') }}</p> }
        @case ('ready') {
          @if (posts().length === 0) {
            <p class="muted empty">{{ t('moderation.empty.' + queue()) }}</p>
          }
          <ul class="list">
            @for (post of posts(); track post.id) {
              <li class="post stock">
                <p class="meta">{{ post.tower ?? t('moderation.noTower') }} · {{ when(post.submittedAt) }}</p>
                <h2>{{ post.title }}</h2>
                <p class="body">{{ post.body }}</p>
                @for (video of post.videos; track $index) {
                  @if (video.status === 'READY') {
                    <video class="video" controls playsinline preload="none" [poster]="video.posterUrl!" [src]="video.url!"
                           [attr.aria-label]="t('post.videoLabel', { length: length(video.durationMs!) })"></video>
                  } @else if (video.status === 'PROCESSING') {
                    <p class="muted small" role="status">{{ t('moderation.videoProcessing') }}</p>
                  }
                }
                @if (post.evidence.length) {
                  <div class="photos">
                    @for (photo of post.evidence; track photo.url; let i = $index) {
                      <a [href]="photo.url" target="_blank" rel="noopener" class="photo">
                        <img [src]="photo.url" [alt]="t('post.photoAlt', { n: i + 1, total: post.evidence.length })" loading="lazy" />
                      </a>
                    }
                  </div>
                }
                @if (post.reports.length) {
                  <div class="reports">
                    <h3>{{ t('moderation.reports', { count: post.reports.length }) }}</h3>
                    <ul>
                      @for (r of post.reports; track $index) { <li>“{{ r.reason }}”</li> }
                    </ul>
                  </div>
                }

                @if (deciding()?.id === post.id) {
                  <label class="field">
                    <span>{{ t('moderation.reasonFor.' + deciding()!.decision) }}</span>
                    <textarea rows="3" maxlength="500" [value]="reason()" (input)="reason.set($any($event.target).value)"></textarea>
                  </label>
                  <div class="actions">
                    <button hlmBtn variant="destructive" size="lg" type="button" [disabled]="!reason().trim() || saving()"
                            (click)="decide(post, deciding()!.decision)">{{ t('moderation.confirm.' + deciding()!.decision) }}</button>
                    <button hlmBtn variant="ghost" size="lg" type="button" (click)="deciding.set(null)">{{ t('clock.cancel') }}</button>
                  </div>
                } @else {
                  <div class="actions">
                    <button hlmBtn size="lg" type="button" [disabled]="saving()" (click)="decide(post, 'publish')">
                      {{ t(queue() === 'HIDDEN' ? 'moderation.restore' : 'moderation.publish') }}
                    </button>
                    @if (queue() === 'PENDING_REVIEW') {
                      <button hlmBtn variant="outline" size="lg" type="button" (click)="ask(post, 'reject')">{{ t('moderation.reject') }}</button>
                    } @else {
                      <button hlmBtn variant="outline" size="lg" type="button" (click)="ask(post, 'remove')">{{ t('moderation.remove') }}</button>
                    }
                  </div>
                }
                @if (failed() === post.id) { <p class="warn" role="alert">{{ t('moderation.failed') }}</p> }
              </li>
            }
          </ul>
        }
      }
    </ng-container>
  `,
  styles: `
    :host { display: block; padding-top: var(--space-xs); }
    .back { min-height: 44px; display: inline-flex; align-items: center; color: var(--color-brand-deep); }
    h1 { font-size: var(--text-2xl); }
    .lead { color: var(--color-ink-2); max-width: 60ch; margin-bottom: var(--space-md); }
    .tabs { display: flex; gap: var(--space-xs); margin-bottom: var(--space-md); border-bottom: var(--rule-hair) solid var(--color-rule); }
    .tabs button { all: unset; cursor: pointer; min-height: 44px; padding-inline: var(--space-sm); color: var(--color-ink-muted); border-bottom: 2px solid transparent; }
    .tabs button[aria-selected='true'] { color: var(--color-ink); border-bottom-color: var(--color-brand); }
    .tabs button:focus-visible { outline: 2px solid var(--color-focus); }
    .list { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--space-md); }
    .post { padding: var(--space-md); display: grid; gap: var(--space-xs); align-content: start; }
    .meta { font-size: var(--text-sm); color: var(--color-ink-muted); }
    h2 { font-size: var(--text-lg); }
    .body { white-space: pre-line; color: var(--color-ink-2); }
    .photos { display: grid; grid-template-columns: repeat(auto-fill, minmax(96px, 1fr)); gap: var(--space-2xs); }
    .video { display: block; width: auto; max-width: 100%; height: auto; max-height: 60vh; border-radius: 8px; background: var(--color-ink); }
    .photo { aspect-ratio: 1; border-radius: 8px; overflow: hidden; background: var(--color-paper-2); }
    .photo img { width: 100%; height: 100%; object-fit: cover; }
    .reports { padding: var(--space-sm); border-radius: 10px; background: color-mix(in oklab, var(--color-error) 8%, var(--color-paper)); }
    .reports h3 { font-size: var(--text-sm); color: var(--color-error); }
    .reports ul { margin: var(--space-2xs) 0 0; padding-inline-start: 1.2em; color: var(--color-ink-2); }
    .field { display: grid; gap: var(--space-3xs); font-size: var(--text-sm); color: var(--color-ink-muted); }
    .field textarea { min-height: 88px; padding: var(--space-xs) var(--space-sm); border: var(--rule-hair) solid var(--color-rule); border-radius: 10px; background: var(--color-stock); color: var(--color-ink); font: inherit; font-size: var(--text-md); resize: vertical; }
    .actions { display: flex; flex-wrap: wrap; gap: var(--space-sm); }
    .muted { color: var(--color-ink-muted); }
    .empty { padding-block: var(--space-md); }
    .warn { color: var(--color-error); }
    @media (min-width: 60rem) { .list { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
  `,
})
export default class ModerationPage {
  private readonly api = inject(FeedApi);
  private readonly lang = inject(LanguageService).current;

  protected readonly length = clipLength;
  protected readonly queues: Queue[] = ['PENDING_REVIEW', 'HIDDEN'];
  protected readonly queue = signal<Queue>('PENDING_REVIEW');
  protected readonly posts = signal<OwnPost[]>([]);
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly deciding = signal<{ id: number; decision: Decision } | null>(null);
  protected readonly reason = signal('');
  protected readonly saving = signal(false);
  protected readonly failed = signal<number | null>(null);

  constructor() {
    effect(() => {
      const queue = this.queue();
      this.state.set('loading');
      this.deciding.set(null);
      this.api.queue(queue).subscribe({
        next: (posts) => {
          this.posts.set(posts);
          this.state.set('ready');
        },
        error: () => this.state.set('error'),
      });
    });
  }

  protected when(iso: string): string {
    return dateTime(iso, this.lang());
  }

  protected ask(post: OwnPost, decision: Decision): void {
    this.deciding.set({ id: post.id, decision });
    this.reason.set('');
  }

  protected decide(post: OwnPost, decision: Decision): void {
    this.saving.set(true);
    this.failed.set(null);
    this.api.decide(post.id, decision, decision === 'publish' ? null : this.reason().trim()).subscribe({
      next: () => {
        this.posts.update((all) => all.filter((p) => p.id !== post.id));
        this.deciding.set(null);
        this.saving.set(false);
      },
      error: () => {
        this.failed.set(post.id);
        this.saving.set(false);
      },
    });
  }
}
