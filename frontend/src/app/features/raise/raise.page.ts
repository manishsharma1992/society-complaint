import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideCamera, lucideImagePlus, lucideX } from '@ng-icons/lucide';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmSpinnerImports } from '@spartan-ng/helm/spinner';
import { forkJoin, switchMap, of } from 'rxjs';
import { ComplaintsApi } from '../../core/complaints';
import { Category, Complaint, Flat, Tower } from '../../core/models';
import { SocietyStore } from '../../core/society';
import { ResidencyApi } from '../../core/residency';
import { SessionStore } from '../../core/session';
import { ComplaintCard } from '../../shared/complaint-card';
import { preparePhoto } from '../../shared/photo';

type Step = 1 | 2 | 3;
type Where = { kind: 'flat'; flatId: number } | { kind: 'common'; towerId: number | null };
interface PickedPhoto { blob: Blob; url: string }

const MAX_PHOTOS = 5;

/**
 * Raising a complaint, in three short steps: photos (optional), what and where, a short title.
 * The complaint is created first; photos then go straight to object storage. A full-screen task: it covers
 * the tab bar and always has a way out.
 */
@Component({
  selector: 'app-raise-page',
  imports: [TranslocoDirective, RouterLink, NgIcon, HlmButtonImports, HlmSpinnerImports, ComplaintCard],
  providers: [provideIcons({ lucideCamera, lucideImagePlus, lucideX })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './raise.page.html',
  styleUrl: './raise.page.css',
})
export default class RaisePage {
  private readonly api = inject(ComplaintsApi);
  private readonly residency = inject(ResidencyApi);
  private readonly society = inject(SocietyStore);
  private readonly router = inject(Router);
  private readonly user = inject(SessionStore).user;

  protected readonly step = signal<Step>(1);
  protected readonly direction = signal<'forward' | 'back'>('forward');
  protected readonly photos = signal<PickedPhoto[]>([]);
  protected readonly preparing = signal(0);
  protected readonly photoError = signal(false);

  protected readonly categories = signal<Category[]>([]);
  protected readonly towers = signal<Tower[]>([]);
  protected readonly myFlats = signal<Flat[]>([]);
  /** towers and own flats where complaints are on (a tower may handle complaints its own way) */
  protected readonly openTowers = computed(() => this.towers().filter((t) => this.society.complaintsOnIn(t.id)));
  protected readonly openFlats = computed(() => this.myFlats().filter((f) => this.society.complaintsOnIn(f.towerId)));
  protected readonly closedFlats = computed(() => this.myFlats().filter((f) => !this.society.complaintsOnIn(f.towerId)));
  protected readonly categoryId = signal<number | null>(null);
  protected readonly where = signal<Where | null>(null);

  protected readonly title = signal('');
  protected readonly details = signal('');
  protected readonly submitting = signal(false);
  protected readonly uploaded = signal(0);
  protected readonly error = signal<string | null>(null);
  protected readonly confirmingCancel = signal(false);

  protected readonly raised = signal<Complaint | null>(null);
  protected readonly failedPhotos = signal(0);

  protected readonly maxPhotos = MAX_PHOTOS;
  protected readonly canAddPhoto = computed(() => this.photos().length + this.preparing() < MAX_PHOTOS);
  protected readonly whereChosen = computed(() => this.categoryId() !== null && this.where() !== null);
  protected readonly titleValid = computed(() => this.title().trim().length > 0 && this.title().trim().length <= 150);
  protected readonly started = computed(
    () => this.photos().length > 0 || this.categoryId() !== null || this.title().trim().length > 0,
  );

  constructor() {
    const userId = this.user()?.id;
    forkJoin({
      categories: this.api.categories(),
      towers: this.residency.towers(),
      flats: userId ? this.residency.flatsOf(userId) : of([] as Flat[]),
    }).subscribe(({ categories, towers, flats }) => {
      this.categories.set(categories);
      this.towers.set(towers);
      this.myFlats.set(flats);
      // one flat: that is almost always where the problem is
      const open = flats.filter((f) => this.society.complaintsOnIn(f.towerId));
      if (open.length === 1) {
        this.where.set({ kind: 'flat', flatId: open[0].id });
      }
    });
    inject(DestroyRef).onDestroy(() => this.photos().forEach((p) => URL.revokeObjectURL(p.url)));
  }

  // step 1: photos

  protected async addPhotos(input: HTMLInputElement): Promise<void> {
    const files = Array.from(input.files ?? []).slice(0, MAX_PHOTOS - this.photos().length - this.preparing());
    input.value = '';
    this.photoError.set(false);
    for (const file of files) {
      this.preparing.update((n) => n + 1);
      try {
        const blob = await preparePhoto(file);
        this.photos.update((list) => [...list, { blob, url: URL.createObjectURL(blob) }]);
      } catch {
        this.photoError.set(true);
      } finally {
        this.preparing.update((n) => n - 1);
      }
    }
  }

  protected removePhoto(index: number): void {
    const photo = this.photos()[index];
    URL.revokeObjectURL(photo.url);
    this.photos.update((list) => list.filter((_, i) => i !== index));
  }

  // step 2: what and where

  protected chooseFlat(flatId: number): void {
    this.where.set({ kind: 'flat', flatId });
  }

  protected chooseCommon(towerId: number | null): void {
    this.where.set({ kind: 'common', towerId });
  }

  protected isFlat(flatId: number): boolean {
    const where = this.where();
    return where?.kind === 'flat' && where.flatId === flatId;
  }

  protected isCommon(towerId?: number | null): boolean {
    const where = this.where();
    return where?.kind === 'common' && (towerId === undefined || where.towerId === towerId);
  }

  protected sla(category: Category): { key: string; n: number } {
    return category.slaHours >= 48
      ? { key: 'sla.days', n: Math.round(category.slaHours / 24) }
      : { key: 'sla.hours', n: category.slaHours };
  }

  // navigation between steps

  protected next(): void {
    this.direction.set('forward');
    this.step.update((s) => (s < 3 ? ((s + 1) as Step) : s));
  }

  protected back(): void {
    this.direction.set('back');
    this.step.update((s) => (s > 1 ? ((s - 1) as Step) : s));
  }

  protected cancel(): void {
    if (this.started() && !this.confirmingCancel()) {
      this.confirmingCancel.set(true);
      return;
    }
    void this.router.navigate(['/']);
  }

  // step 3: send

  protected submit(): void {
    const where = this.where();
    const categoryId = this.categoryId();
    if (!where || categoryId === null || !this.titleValid() || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.uploaded.set(0);
    const photos = this.photos().map((p) => p.blob);
    this.api
      .raise({
        scope: where.kind === 'flat' ? 'FLAT' : 'COMMON_AREA',
        flatId: where.kind === 'flat' ? where.flatId : null,
        towerId: where.kind === 'common' ? where.towerId : null,
        categoryId,
        title: this.title().trim(),
        description: this.details().trim() || null,
      })
      .pipe(
        switchMap((complaint) =>
          (photos.length ? this.api.attachPhotos(complaint.id, photos, (n) => this.uploaded.set(n)) : of(0)).pipe(
            switchMap((ok) => {
              this.failedPhotos.set(photos.length - ok);
              return ok > 0 ? this.api.get(complaint.id) : of(complaint);
            }),
          ),
        ),
      )
      .subscribe({
        next: (complaint) => {
          this.submitting.set(false);
          this.raised.set(complaint);
        },
        error: (e: unknown) => {
          this.submitting.set(false);
          this.error.set(e instanceof HttpErrorResponse && e.status === 403 ? 'raise.errorNotYours' : 'raise.errorGeneric');
        },
      });
  }

  protected categoryNameOf(id: number): string | null {
    return this.categories().find((c) => c.id === id)?.name ?? null;
  }

  protected towerNameOf(id: number | null): string | null {
    const tower = this.towers().find((t) => t.id === id);
    return tower ? `${tower.code} ${tower.name}` : null;
  }

  protected flatLabelOf(id: number | null): string | null {
    return this.myFlats().find((f) => f.id === id)?.label ?? null;
  }
}
