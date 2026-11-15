import { ChangeDetectionStrategy, Component, computed, inject, input, signal, effect } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { clipLength, dateTime } from '../../core/format';
import { FeedApi, FeedPost, PostTranslation } from '../../core/feed';
import { LanguageService } from '../../core/language';
import { Roles } from '../../core/roles';
import { ReactionBar } from '../../shared/reaction-bar';

const DEVANAGARI = /[ऀ-ॿ]/;

/**
 * One published post: the whole text, every photo, the emoji row and a way to report it (3 reports hide it for
 * review). Signed-in members open it from their feed; everyone else from the public feed (route data openToAll).
 */
@Component({
  selector: 'app-post-page',
  imports: [TranslocoDirective, RouterLink, HlmButtonImports, ReactionBar],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      <a [routerLink]="openToAll() ? '/public' : '/feed'" class="back">{{ t('post.back') }}</a>
      @switch (state()) {
        @case ('loading') { <p class="muted" aria-busy="true">{{ t('feed.loading') }}</p> }
        @case ('missing') {
          <div class="notice"><h1>{{ t('post.missingTitle') }}</h1><p>{{ t('post.missingBody') }}</p></div>
        }
        @case ('error') { <p class="notice" role="alert">{{ t('feed.failed') }}</p> }
        @case ('ready') {
          @if (post(); as p) {
            <article>
              <p class="meta">
                <span class="who">{{ t('feed.verified') }}</span>
                @if (place()) {
                  <span>{{ place() }}</span>
                }
                <span>{{ published() }}</span>
              </p>
              <h1 [attr.lang]="translated()?.language">{{ translated()?.title ?? p.title }}</h1>
              <p class="body" [attr.lang]="translated()?.language">{{ translated()?.body ?? p.body }}</p>
              @if (translated()) {
                <div class="translate">
                  <p class="muted small" role="status">{{ t('post.machineTranslated') }}</p>
                  <div class="translate-actions">
                    <button hlmBtn variant="ghost" size="lg" type="button" class="link-btn" (click)="translation.set(null)">{{ t('post.showOriginal') }}</button>
                    @if (isSocietyAdmin()) {
                      <button hlmBtn variant="ghost" size="lg" type="button" class="link-btn" [disabled]="translating()" [attr.aria-busy]="translating()" (click)="translate(p, true)">{{ t('post.translateAgain') }}</button>
                    }
                  </div>
                  @if (translateFailed()) { <p class="warn" role="alert">{{ t('post.translateFailed') }}</p> }
                </div>
              } @else if (canTranslate()) {
                <div class="translate">
                  <button hlmBtn variant="outline" size="lg" type="button" [disabled]="translating()" [attr.aria-busy]="translating()" (click)="translate(p)">{{ t('post.translate') }}</button>
                  @if (translateFailed()) { <p class="warn" role="alert">{{ t('post.translateFailed') }}</p> }
                </div>
              }
              <div class="photos">
                @for (video of p.videos; track video.url) {
                  <video class="video" controls playsinline preload="none" [poster]="video.posterUrl" [src]="video.url"
                         [width]="video.widthPx" [height]="video.heightPx"
                         [attr.aria-label]="t('post.videoLabel', { length: length(video.durationMs) })"></video>
                }
                @for (photo of p.photos; track photo.url; let i = $index) {
                  <a [href]="photo.url" target="_blank" rel="noopener" class="photo">
                    <img [src]="photo.url" [alt]="t('post.photoAlt', { n: i + 1, total: p.photos.length })"
                         [width]="photo.widthPx" [height]="photo.heightPx" loading="lazy" />
                  </a>
                }
              </div>
              <app-reaction-bar [slug]="p.slug" [reactions]="p.reactions" />
            </article>

            <section class="report" [attr.aria-label]="t('post.report')">
              @if (reported()) {
                <p class="thanks" role="status">{{ t('post.reported') }}</p>
              } @else if (reporting()) {
                <label class="field">
                  <span>{{ t('post.reportReason') }}</span>
                  <textarea rows="3" maxlength="500" [value]="reason()" (input)="reason.set($any($event.target).value)"></textarea>
                </label>
                <div class="actions">
                  <button hlmBtn size="lg" type="button" [disabled]="!reason().trim() || sending()" (click)="sendReport(p)">{{ t('post.sendReport') }}</button>
                  <button hlmBtn variant="ghost" size="lg" type="button" (click)="reporting.set(false)">{{ t('clock.cancel') }}</button>
                </div>
                @if (reportFailed()) { <p class="warn" role="alert">{{ t('post.reportFailed') }}</p> }
              } @else {
                <button hlmBtn variant="ghost" size="lg" type="button" class="report-btn" (click)="reporting.set(true)">{{ t('post.report') }}</button>
                <p class="muted small">{{ t('post.reportHint') }}</p>
              }
            </section>
          }
        }
      }
    </ng-container>
  `,
  styles: `
    :host { display: block; padding-top: var(--space-xs); max-width: 46rem; }
    .back { min-height: 44px; display: inline-flex; align-items: center; color: var(--color-brand-deep); }
    article { display: grid; gap: var(--space-sm); }
    .meta { display: flex; flex-wrap: wrap; gap: var(--space-2xs) var(--space-sm); font-size: var(--text-sm); color: var(--color-ink-muted); }
    .who { color: var(--color-brand-deep); font-weight: 500; }
    h1 { font-size: var(--text-2xl); text-wrap: balance; }
    .body { color: var(--color-ink); white-space: pre-line; max-width: 65ch; }
    .translate { display: grid; gap: var(--space-2xs); justify-items: start; }
    .link-btn { color: var(--color-brand-deep); padding-inline: 0; }
    .translate-actions { display: flex; flex-wrap: wrap; gap: var(--space-md); }
    .photos { display: grid; gap: var(--space-sm); margin-top: var(--space-xs); }
    /* as wide as the video itself (portrait clips get no black bars), never wider than the column or taller than 80vh */
    .video { width: auto; max-width: 100%; height: auto; max-height: 80vh; border-radius: var(--radius-card); background: var(--color-ink); }
    .photo { display: block; border-radius: var(--radius-card); overflow: hidden; background: var(--color-paper-2); }
    .photo img { width: 100%; height: auto; display: block; }
    .photo:focus-visible { outline: 2px solid var(--color-focus); outline-offset: 2px; }
    .report { margin-top: var(--space-xl); padding-top: var(--space-md); border-top: var(--rule-hair) solid var(--color-rule); display: grid; gap: var(--space-xs); justify-items: start; }
    .report-btn { color: var(--color-ink-2); padding-inline: 0; }
    .field { display: grid; gap: var(--space-3xs); width: 100%; font-size: var(--text-sm); color: var(--color-ink-muted); }
    .field textarea { min-height: 88px; padding: var(--space-xs) var(--space-sm); border: var(--rule-hair) solid var(--color-rule); border-radius: 10px; background: var(--color-stock); color: var(--color-ink); font: inherit; font-size: var(--text-md); resize: vertical; }
    .actions { display: flex; flex-wrap: wrap; gap: var(--space-sm); }
    .muted { color: var(--color-ink-muted); }
    .small { font-size: var(--text-sm); }
    .warn { color: var(--color-error); }
    .thanks { color: var(--color-ink-2); }
    .notice { padding: var(--space-lg) 0; color: var(--color-ink-2); display: grid; gap: var(--space-sm); }
    .notice h1 { font-size: var(--text-xl); color: var(--color-ink); }
  `,
})
export default class PostPage {
  /** From the route. */
  readonly slug = input.required<string>();
  /** From the route data: the public page, for everyone, signed in or not. */
  readonly openToAll = input(false);

  private readonly api = inject(FeedApi);
  private readonly lang = inject(LanguageService).current;
  /** The server checks it too; here it only decides whether to offer "Translate again". */
  protected readonly isSocietyAdmin = inject(Roles).isSocietyAdmin;

  protected readonly post = signal<FeedPost | null>(null);
  protected readonly state = signal<'loading' | 'ready' | 'missing' | 'error'>('loading');
  protected readonly published = computed(() => (this.post() ? dateTime(this.post()!.publishedAt, this.lang()) : ''));
  protected readonly place = computed(() =>
    [this.post()?.society, this.post()?.tower].filter((part) => !!part).join(' · '),
  );
  protected readonly length = clipLength;
  protected readonly reporting = signal(false);
  protected readonly reason = signal('');
  protected readonly sending = signal(false);
  protected readonly reported = signal(false);
  protected readonly reportFailed = signal(false);
  protected readonly translation = signal<PostTranslation | null>(null);
  /** Only while it is in the language the reader has chosen; switching language shows the original again. */
  protected readonly translated = computed(() =>
    this.translation()?.language === this.lang() ? this.translation() : null,
  );
  protected readonly translating = signal(false);
  protected readonly translateFailed = signal(false);
  /** An English reader needs no button for a post with no Hindi or Marathi letters in it. */
  protected readonly canTranslate = computed(() => {
    const p = this.post();
    return !!p && (this.lang() !== 'en' || DEVANAGARI.test(p.title + p.body));
  });

  constructor() {
    effect(() => {
      const slug = this.slug();
      this.state.set('loading');
      this.translation.set(null);
      (this.openToAll() ? this.api.publicPost(slug) : this.api.post(slug)).subscribe({
        next: (post) => {
          this.post.set(post);
          this.state.set('ready');
        },
        error: (e) => this.state.set(e?.status === 404 ? 'missing' : 'error'),
      });
    });
  }

  /** @param again the society admin has a bad stored translation made again */
  protected translate(post: FeedPost, again = false): void {
    this.translating.set(true);
    this.translateFailed.set(false);
    (again ? this.api.redoTranslation(post.slug, this.lang()) : this.api.translation(post.slug, this.lang())).subscribe({
      next: (translation) => {
        this.translation.set(translation);
        this.translating.set(false);
      },
      error: () => {
        this.translateFailed.set(true);
        this.translating.set(false);
      },
    });
  }

  protected sendReport(post: FeedPost): void {
    this.sending.set(true);
    this.reportFailed.set(false);
    this.api.report(post.slug, this.reason().trim()).subscribe({
      next: () => {
        this.reported.set(true);
        this.sending.set(false);
      },
      error: () => {
        this.reportFailed.set(true);
        this.sending.set(false);
      },
    });
  }
}
