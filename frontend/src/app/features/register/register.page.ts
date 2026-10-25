import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmSpinnerImports } from '@spartan-ng/helm/spinner';
import { Flat, Tower } from '../../core/models';
import { ResidencyApi } from '../../core/residency';
import { SessionStore } from '../../core/session';
import { LanguageSwitch } from '../../shared/language-switch';

type Step = 1 | 2 | 3;

/**
 * Self sign-up: a name, the flat, owner or tenant; the society admin then approves. Only what the admin needs to
 * check that someone lives there is asked (India DPDP Act). While waiting, this page shows the request.
 */
@Component({
  selector: 'app-register-page',
  imports: [TranslocoDirective, HlmButtonImports, HlmSpinnerImports, LanguageSwitch],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './register.page.html',
  styleUrl: './register.page.css',
})
export default class RegisterPage {
  private readonly session = inject(SessionStore);
  private readonly residency = inject(ResidencyApi);
  private readonly router = inject(Router);
  protected readonly user = this.session.user;
  protected readonly flatLabels = this.residency.flatLabels;

  protected readonly step = signal<Step>(1);
  protected readonly direction = signal<'forward' | 'back'>('forward');
  protected readonly name = signal(this.user()?.name ?? '');
  protected readonly towers = signal<Tower[]>([]);
  protected readonly towerId = signal<number | null>(null);
  protected readonly flats = signal<Flat[]>([]);
  protected readonly loadingFlats = signal(false);
  protected readonly floor = signal<number | null>(null);
  protected readonly flatId = signal<number | null>(null);
  protected readonly occupancy = signal<'OWNER' | 'TENANT' | null>(null);

  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly checking = signal(false);
  protected readonly stillWaiting = signal(false);

  protected readonly waiting = computed(() => this.user()?.status === 'PENDING');
  protected readonly nameValid = computed(() => this.name().trim().length > 1 && this.name().trim().length <= 100);
  protected readonly floors = computed(() => [...new Set(this.flats().map((f) => f.floor))].sort((a, b) => a - b));
  protected readonly flatsOnFloor = computed(() => this.flats().filter((f) => f.floor === this.floor()).sort((a, b) => a.number - b.number));
  protected readonly chosenFlat = computed(() => this.flats().find((f) => f.id === this.flatId()) ?? null);

  constructor() {
    this.residency.towers().subscribe({ next: (towers) => this.towers.set(towers), error: () => {} });
    const claim = this.user()?.claim;
    if (claim) this.residency.ensureFlatLabels([claim.flatId]);
  }

  protected chooseTower(towerId: number): void {
    if (this.towerId() === towerId) return;
    this.towerId.set(towerId);
    this.floor.set(null);
    this.flatId.set(null);
    this.flats.set([]);
    this.loadingFlats.set(true);
    this.residency.flatsInTower(towerId).subscribe({
      next: (flats) => {
        this.flats.set(flats);
        this.loadingFlats.set(false);
      },
      error: () => {
        this.loadingFlats.set(false);
        this.error.set('register.errorFlats');
      },
    });
  }

  protected chooseFloor(floor: number): void {
    this.floor.set(floor);
    this.flatId.set(null);
  }

  protected next(): void {
    this.error.set(null);
    this.direction.set('forward');
    this.step.update((s) => (s < 3 ? ((s + 1) as Step) : s));
  }

  protected back(): void {
    this.error.set(null);
    this.direction.set('back');
    this.step.update((s) => (s > 1 ? ((s - 1) as Step) : s));
  }

  protected send(): void {
    const flatId = this.flatId();
    const occupancy = this.occupancy();
    if (flatId === null || occupancy === null || !this.nameValid() || this.sending()) return;
    this.sending.set(true);
    this.error.set(null);
    this.session.register({ name: this.name().trim(), flatId, occupancy }).subscribe({
      next: () => this.sending.set(false),
      error: (e: unknown) => {
        this.sending.set(false);
        this.error.set(e instanceof HttpErrorResponse && e.status === 422 ? 'register.errorRefused' : 'register.errorGeneric');
      },
    });
  }

  /** Asks whether the admin has decided; an approved person goes straight in. */
  protected checkAgain(): void {
    this.checking.set(true);
    this.stillWaiting.set(false);
    this.session.renew().subscribe({
      next: (session) => {
        this.checking.set(false);
        if (session.user.status === 'ACTIVE') {
          void this.router.navigate(['/']);
        } else {
          this.stillWaiting.set(session.user.status === 'PENDING');
        }
      },
      error: () => this.checking.set(false),
    });
  }

  protected signOut(): void {
    this.session.signOut();
    void this.router.navigate(['/login']);
  }
}
