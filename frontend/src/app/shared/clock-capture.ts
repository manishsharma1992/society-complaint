import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, OnInit, computed, effect, input, output, signal, viewChild } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { ClockCapture } from '../core/workforce';
import { preparePhoto } from './photo';

const NOTICE_KEY = 'mycity.shift-notice';
/** How long to wait for a GPS fix before offering to go on without a location. */
const LOCATION_TIMEOUT_MS = 20_000;

type Position = { latitude: number; longitude: number; accuracyMeters: number };
type Step = 'notice' | 'camera' | 'preview' | 'noCamera';

/**
 * Clocking in or out: a live selfie from the front camera (never from the gallery) and, at the same moment, the
 * phone's location. The first time, it explains what is collected and why. Without a camera there is no clock-in;
 * without a location the shift is still recorded, and flagged for the admin.
 */
@Component({
  selector: 'app-clock-capture',
  imports: [TranslocoDirective, HlmButtonImports],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="sheet" role="dialog" aria-modal="true" [attr.aria-labelledby]="'clock-title'" *transloco="let t">
      <h2 id="clock-title">{{ t(mode() === 'start' ? 'clock.startTitle' : 'clock.endTitle') }}</h2>

      @switch (step()) {
        @case ('notice') {
          <div class="notice">
            <p>{{ t('clock.notice.intro') }}</p>
            <ul>
              <li>{{ t('clock.notice.selfie') }}</li>
              <li>{{ t('clock.notice.location') }}</li>
              <li>{{ t('clock.notice.who') }}</li>
              <li>{{ t('clock.notice.kept') }}</li>
            </ul>
          </div>
          <div class="actions">
            <button hlmBtn size="lg" type="button" (click)="acceptNotice()">{{ t('clock.notice.ok') }}</button>
            <button hlmBtn variant="ghost" size="lg" type="button" (click)="cancelled.emit()">{{ t('clock.cancel') }}</button>
          </div>
        }
        @case ('noCamera') {
          <p class="error" role="alert">{{ t('clock.noCamera') }}</p>
          <div class="actions">
            <button hlmBtn size="lg" type="button" (click)="startCamera()">{{ t('clock.tryAgain') }}</button>
            <button hlmBtn variant="ghost" size="lg" type="button" (click)="cancelled.emit()">{{ t('clock.cancel') }}</button>
          </div>
        }
        @default {
          <div class="frame">
            <video #video [class.hidden]="step() !== 'camera'" autoplay playsinline muted [attr.aria-label]="t('clock.cameraLabel')"></video>
            @if (step() === 'preview' && previewUrl()) {
              <img [src]="previewUrl()" [alt]="t('clock.previewAlt')" />
            }
          </div>
          <p class="where" aria-live="polite">
            @switch (locationState()) {
              @case ('locating') { {{ t('clock.locating') }} }
              @case ('found') { {{ t('clock.located', { meters: accuracy() }) }} }
              @case ('none') { <span class="warn">{{ t('clock.noLocation') }}</span> }
            }
          </p>
          <div class="actions">
            @if (step() === 'camera') {
              <button hlmBtn size="xl" type="button" [disabled]="busy()" (click)="take()">{{ t('clock.take') }}</button>
            } @else {
              <button hlmBtn size="xl" type="button" [disabled]="locationState() === 'locating'" (click)="use()">
                {{ t(mode() === 'start' ? 'clock.useStart' : 'clock.useEnd') }}
              </button>
              <button hlmBtn variant="outline" size="lg" type="button" (click)="retake()">{{ t('clock.retake') }}</button>
            }
            <button hlmBtn variant="ghost" size="lg" type="button" (click)="cancelled.emit()">{{ t('clock.cancel') }}</button>
          </div>
        }
      }
    </div>
  `,
  styles: `
    :host { position: fixed; inset: 0; z-index: var(--z-modal); display: grid; place-items: end center; background: color-mix(in oklab, var(--color-ink) 45%, transparent); }
    .sheet {
      width: min(100%, 32rem);
      max-height: 100dvh;
      overflow-y: auto;
      padding: var(--space-md) var(--gutter) calc(var(--space-md) + env(safe-area-inset-bottom));
      background: var(--color-paper);
      border-radius: var(--radius-card) var(--radius-card) 0 0;
      display: grid;
      gap: var(--space-sm);
    }
    h2 { font-size: var(--text-xl); }
    .notice { display: grid; gap: var(--space-xs); color: var(--color-ink-2); }
    .notice ul { margin: 0; padding-inline-start: 1.2em; display: grid; gap: var(--space-2xs); }
    .frame { position: relative; aspect-ratio: 3 / 4; max-height: 55dvh; margin-inline: auto; width: 100%; border-radius: var(--radius-card); overflow: hidden; background: var(--color-ink); }
    video, img { width: 100%; height: 100%; object-fit: cover; display: block; }
    /* a mirror image feels natural while framing; the photo itself is stored the right way round */
    video { transform: scaleX(-1); }
    .hidden { display: none; }
    .where { min-height: 1.5em; color: var(--color-ink-2); font-size: var(--text-sm); }
    .warn, .error { color: var(--color-error); }
    .actions { display: grid; gap: var(--space-xs); }
    @media (min-width: 48rem) { :host { place-items: center; } .sheet { border-radius: var(--radius-card); } }
  `,
})
export class ClockCaptureSheet implements OnInit, OnDestroy {
  readonly mode = input.required<'start' | 'end'>();
  readonly captured = output<ClockCapture>();
  readonly cancelled = output<void>();

  private readonly video = viewChild<ElementRef<HTMLVideoElement>>('video');
  private readonly stream = signal<MediaStream | null>(null);
  private selfie: Blob | null = null;

  protected readonly step = signal<Step>('camera');
  protected readonly busy = signal(false);
  protected readonly previewUrl = signal<string | null>(null);
  private readonly position = signal<Position | null | undefined>(undefined);
  protected readonly locationState = computed(() =>
    this.position() === undefined ? 'locating' : this.position() === null ? 'none' : 'found',
  );
  protected readonly accuracy = computed(() => Math.round(this.position()?.accuracyMeters ?? 0));

  constructor() {
    // the video element appears only once the camera step renders: attach the stream whenever both exist
    effect(() => {
      const el = this.video()?.nativeElement;
      const stream = this.stream();
      if (el && stream && el.srcObject !== stream) {
        el.srcObject = stream;
        void el.play();
      }
    });
  }

  ngOnInit(): void {
    let seen = false;
    try {
      seen = localStorage.getItem(NOTICE_KEY) === '1';
    } catch {
      // blocked storage: show the notice every time
    }
    if (seen) {
      void this.startCamera();
    } else {
      this.step.set('notice');
    }
  }

  ngOnDestroy(): void {
    this.stopCamera();
    const url = this.previewUrl();
    if (url) URL.revokeObjectURL(url);
  }

  protected acceptNotice(): void {
    try {
      localStorage.setItem(NOTICE_KEY, '1');
    } catch {
      // the notice simply shows again next time
    }
    void this.startCamera();
  }

  protected async startCamera(): Promise<void> {
    this.locate();
    try {
      this.stream.set(await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: { ideal: 1280 } }, audio: false }));
      this.step.set('camera');
    } catch {
      this.step.set('noCamera');
    }
  }

  /** The location is looked up once per capture, at the moment of clocking in or out. */
  private locate(): void {
    if (this.position() !== undefined) return;
    if (!('geolocation' in navigator)) {
      this.position.set(null);
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (p) => this.position.set({ latitude: p.coords.latitude, longitude: p.coords.longitude, accuracyMeters: p.coords.accuracy }),
      () => this.position.set(null),
      { enableHighAccuracy: true, timeout: LOCATION_TIMEOUT_MS, maximumAge: 0 },
    );
  }

  protected async take(): Promise<void> {
    const el = this.video()?.nativeElement;
    if (!el || el.videoWidth === 0) return;
    this.busy.set(true);
    try {
      const canvas = document.createElement('canvas');
      canvas.width = el.videoWidth;
      canvas.height = el.videoHeight;
      canvas.getContext('2d')!.drawImage(el, 0, 0);
      const frame = await new Promise<Blob>((resolve, reject) =>
        canvas.toBlob((b) => (b ? resolve(b) : reject(new Error('No frame'))), 'image/jpeg', 0.92),
      );
      // the shared photo preparation sizes and re-encodes it like every other photo in the app
      this.selfie = await preparePhoto(new File([frame], 'selfie.jpg', { type: 'image/jpeg' }));
      this.previewUrl.set(URL.createObjectURL(this.selfie));
      this.step.set('preview');
      this.stopCamera();
    } finally {
      this.busy.set(false);
    }
  }

  protected retake(): void {
    const url = this.previewUrl();
    if (url) URL.revokeObjectURL(url);
    this.previewUrl.set(null);
    this.selfie = null;
    void this.startCamera();
  }

  protected use(): void {
    if (!this.selfie) return;
    this.captured.emit({ selfie: this.selfie, position: this.position() ?? null });
  }

  private stopCamera(): void {
    this.stream()?.getTracks().forEach((track) => track.stop());
    this.stream.set(null);
  }
}
