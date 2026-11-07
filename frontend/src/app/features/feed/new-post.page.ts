import { ChangeDetectionStrategy, Component, OnDestroy, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideCamera, lucideImagePlus, lucideX } from '@ng-icons/lucide';
import { Observable, of, switchMap, tap, toArray } from 'rxjs';
import { FeedApi, OwnPost } from '../../core/feed';
import { Flat } from '../../core/models';
import { ResidencyApi } from '../../core/residency';
import { SessionStore } from '../../core/session';
import { preparePhoto } from '../../shared/photo';

const MAX_PHOTOS = 5;
const MAX_TITLE = 150;
const MAX_BODY = 4000;

interface Picked {
  blob: Blob;
  url: string;
}

/**
 * A resident documents a problem for the feed. The content policy comes first. Photos are resized and re-encoded on
 * the phone (which strips their location); the server checks them again. A moderator reviews before anyone sees it.
 */
@Component({
  selector: 'app-new-post-page',
  imports: [TranslocoDirective, RouterLink, HlmButtonImports, NgIcon],
  providers: [provideIcons({ lucideCamera, lucideImagePlus, lucideX })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      <a routerLink="/feed" class="back">{{ t('post.back') }}</a>
      <h1>{{ t('newPost.title') }}</h1>

      @if (!accepted()) {
        <section class="policy stock" aria-labelledby="policy-title">
          <h2 id="policy-title">{{ t('newPost.policyTitle') }}</h2>
          <p>{{ t('newPost.policyIntro') }}</p>
          <ul>
            <li>{{ t('newPost.policy.facts') }}</li>
            <li>{{ t('newPost.policy.promise') }}</li>
            <li>{{ t('newPost.policy.people') }}</li>
            <li>{{ t('newPost.policy.tone') }}</li>
            <li>{{ t('newPost.policy.review') }}</li>
            <li>{{ t('newPost.policy.anonymous') }}</li>
          </ul>
          <label class="check">
            <input type="checkbox" [checked]="ticked()" (change)="ticked.set($any($event.target).checked)" />
            <span>{{ t('newPost.policyAgree') }}</span>
          </label>
          <button hlmBtn size="lg" type="button" [disabled]="!ticked()" (click)="accepted.set(true)">{{ t('newPost.continue') }}</button>
        </section>
      } @else {
        <form class="form" (submit)="$event.preventDefault(); send()" novalidate>
          <label class="field">
            <span>{{ t('newPost.what') }}</span>
            <input type="text" [attr.maxlength]="maxTitle" [value]="title()" (input)="title.set($any($event.target).value)"
                   [placeholder]="t('newPost.whatPlaceholder')" />
          </label>
          <label class="field">
            <span>{{ t('newPost.details') }}</span>
            <textarea rows="7" [attr.maxlength]="maxBody" [value]="body()" (input)="body.set($any($event.target).value)"
                      [placeholder]="t('newPost.detailsPlaceholder')"></textarea>
            <span class="count num">{{ body().length }} / {{ maxBody }}</span>
          </label>

          <div class="field">
            <span>{{ t('newPost.photos', { max: maxPhotos }) }}</span>
            <div class="thumbs">
              @for (p of photos(); track p.url; let i = $index) {
                <span class="thumb">
                  <img [src]="p.url" [alt]="t('newPost.photoAlt', { n: i + 1 })" />
                  <button type="button" class="remove" (click)="removePhoto(i)" [attr.aria-label]="t('newPost.removePhoto', { n: i + 1 })">
                    <ng-icon name="lucideX" aria-hidden="true" />
                  </button>
                </span>
              }
            </div>
            @if (photos().length < maxPhotos) {
              <div class="pickers">
                <label class="picker press">
                  <ng-icon name="lucideCamera" aria-hidden="true" />
                  <span>{{ t('newPost.camera') }}</span>
                  <input type="file" accept="image/*" capture="environment" (change)="addPhotos($any($event.target))" />
                </label>
                <label class="picker press">
                  <ng-icon name="lucideImagePlus" aria-hidden="true" />
                  <span>{{ t('newPost.gallery') }}</span>
                  <input type="file" accept="image/*" multiple (change)="addPhotos($any($event.target))" />
                </label>
              </div>
            }
            @if (preparing()) { <p class="muted small">{{ t('newPost.preparing') }}</p> }
          </div>

          @if (tower(); as tw) {
            <label class="check">
              <input type="checkbox" [checked]="showTower()" (change)="showTower.set($any($event.target).checked)" />
              <span>{{ t('newPost.showTower', { tower: tw.towerCode }) }}</span>
            </label>
          }

          @if (error()) { <p class="warn" role="alert">{{ error() }}</p> }
          <button hlmBtn size="xl" type="submit" [disabled]="busy() || !title().trim() || !body().trim()">
            {{ busy() ? t('newPost.sending') : t('newPost.send') }}
          </button>
          <p class="muted small">{{ t('newPost.reviewNote') }}</p>
        </form>
      }
    </ng-container>
  `,
  styles: `
    :host { display: block; padding-top: var(--space-xs); max-width: 40rem; }
    .back { min-height: 44px; display: inline-flex; align-items: center; color: var(--color-brand-deep); }
    h1 { font-size: var(--text-2xl); margin-bottom: var(--space-md); }
    .policy { padding: var(--space-md); display: grid; gap: var(--space-sm); color: var(--color-ink-2); }
    .policy h2 { font-size: var(--text-lg); color: var(--color-ink); }
    .policy ul { margin: 0; padding-inline-start: 1.2em; display: grid; gap: var(--space-2xs); }
    .check { display: flex; gap: var(--space-sm); align-items: flex-start; min-height: 44px; color: var(--color-ink); cursor: pointer; }
    .check input { width: 22px; height: 22px; margin-top: 2px; accent-color: var(--color-brand); flex: none; }
    .form { display: grid; gap: var(--space-md); }
    .field { display: grid; gap: var(--space-3xs); font-size: var(--text-sm); color: var(--color-ink-muted); }
    .field input[type='text'], .field textarea {
      min-height: 44px; padding: var(--space-xs) var(--space-sm); border: var(--rule-hair) solid var(--color-rule);
      border-radius: 10px; background: var(--color-stock); color: var(--color-ink); font: inherit; font-size: var(--text-md);
    }
    .field textarea { resize: vertical; }
    .field input:focus-visible, .field textarea:focus-visible { outline: 2px solid var(--color-focus); outline-offset: 1px; }
    .count { justify-self: end; font-size: var(--text-xs); }
    .num { font-variant-numeric: tabular-nums; }
    .thumbs { display: flex; flex-wrap: wrap; gap: var(--space-xs); }
    .thumb { position: relative; width: 88px; height: 88px; border-radius: 10px; overflow: hidden; background: var(--color-paper-2); }
    .thumb img { width: 100%; height: 100%; object-fit: cover; }
    .remove { position: absolute; top: 2px; right: 2px; width: 32px; height: 32px; border-radius: 50%; display: grid; place-items: center; border: 0; background: color-mix(in oklab, var(--color-ink) 65%, transparent); color: var(--color-paper); cursor: pointer; }
    /* the same camera / gallery picker as raising a complaint */
    .pickers { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-xs); }
    .picker {
      position: relative; overflow: hidden; cursor: pointer; min-height: 96px; display: flex; flex-direction: column;
      align-items: center; justify-content: center; gap: var(--space-xs); padding: var(--space-sm); text-align: center;
      border: var(--rule-hair) solid var(--color-ink-muted); border-radius: var(--radius-pill);
      background: var(--color-stock); color: var(--color-ink);
    }
    .picker ng-icon { font-size: 26px; color: var(--color-brand-deep); }
    .picker:focus-within { outline: 2px solid var(--color-focus); outline-offset: 2px; }
    .picker input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
    .muted { color: var(--color-ink-muted); }
    .small { font-size: var(--text-sm); }
    .warn { color: var(--color-error); }
  `,
})
export default class NewPostPage implements OnDestroy {
  private readonly api = inject(FeedApi);
  private readonly router = inject(Router);
  private readonly me = inject(SessionStore).user;
  private readonly transloco = inject(TranslocoService);
  /** Set once the post exists, so a failed photo upload is retried without creating the post twice. */
  private created: OwnPost | null = null;
  private photosSent = 0;

  protected readonly maxPhotos = MAX_PHOTOS;
  protected readonly maxTitle = MAX_TITLE;
  protected readonly maxBody = MAX_BODY;
  protected readonly ticked = signal(false);
  protected readonly accepted = signal(false);
  protected readonly title = signal('');
  protected readonly body = signal('');
  protected readonly photos = signal<Picked[]>([]);
  protected readonly preparing = signal(false);
  protected readonly tower = signal<Flat | null>(null);
  protected readonly showTower = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    const user = this.me();
    if (user) {
      inject(ResidencyApi).flatsOf(user.id).subscribe({ next: (flats) => this.tower.set(flats[0] ?? null), error: () => {} });
    }
  }

  ngOnDestroy(): void {
    this.photos().forEach((p) => URL.revokeObjectURL(p.url));
  }

  protected async addPhotos(input: HTMLInputElement): Promise<void> {
    const files = Array.from(input.files ?? []).slice(0, MAX_PHOTOS - this.photos().length);
    input.value = '';
    if (files.length === 0) return;
    this.preparing.set(true);
    try {
      for (const file of files) {
        const blob = await preparePhoto(file);
        this.photos.update((all) => [...all, { blob, url: URL.createObjectURL(blob) }]);
      }
    } catch {
      this.error.set(this.transloco.translate('newPost.photoFailed'));
    } finally {
      this.preparing.set(false);
    }
  }

  protected removePhoto(index: number): void {
    if (index < this.photosSent) return; // already attached to the post
    const removed = this.photos()[index];
    URL.revokeObjectURL(removed.url);
    this.photos.update((all) => all.filter((_, i) => i !== index));
  }

  protected send(): void {
    this.busy.set(true);
    this.error.set(null);
    const towerId = this.showTower() ? (this.tower()?.towerId ?? null) : null;
    const post$: Observable<OwnPost> = this.created
      ? of(this.created)
      : this.api.submit(this.title().trim(), this.body().trim(), towerId);
    post$
      .pipe(
        switchMap((post) => {
          this.created = post;
          const remaining = this.photos().slice(this.photosSent).map((p) => p.blob);
          return remaining.length
            ? this.api.attachPhotos(post.id, remaining).pipe(tap(() => this.photosSent++), toArray())
            : of([]);
        }),
      )
      .subscribe({
        next: () => void this.router.navigate(['/feed/mine'], { state: { sent: true } }),
        error: (e: HttpErrorResponse) => {
          // the server explains a broken rule (daily limit, a photo with location data left in it)
          this.error.set(e.status === 422 && e.error?.detail ? e.error.detail : this.transloco.translate('newPost.failed'));
          this.busy.set(false);
        },
      });
  }
}
