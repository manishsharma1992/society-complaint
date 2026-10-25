import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmSpinnerImports } from '@spartan-ng/helm/spinner';
import { RegistrationsApi } from '../../core/registrations';
import { ResidencyApi } from '../../core/residency';
import { User } from '../../core/session';
import { ListState } from '../../shared/complaint-list';

/** People who signed up for a flat, waiting for the society admin to approve them (or not, with a reason). */
@Component({
  selector: 'app-signups-page',
  imports: [TranslocoDirective, HlmButtonImports, HlmSpinnerImports],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './signups.page.html',
  styleUrl: './signups.page.css',
})
export default class SignupsPage {
  private readonly api = inject(RegistrationsApi);
  private readonly residency = inject(ResidencyApi);
  protected readonly flatLabels = this.residency.flatLabels;

  protected readonly state = signal<ListState>('loading');
  protected readonly pending = signal<User[]>([]);
  /** Who was just approved or turned down, so the admin sees it happen before the row leaves. */
  protected readonly decided = signal<Record<number, 'ACTIVE' | 'REJECTED'>>({});
  protected readonly busy = signal<number | null>(null);
  protected readonly failed = signal<number | null>(null);
  protected readonly rejecting = signal<number | null>(null);
  protected readonly reason = signal('');
  protected readonly reasonValid = computed(() => this.reason().trim().length > 0 && this.reason().trim().length <= 500);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.api.pending().subscribe({
      next: (users) => {
        this.residency.ensureFlatLabels(users.filter((u) => u.claim).map((u) => u.claim!.flatId));
        this.pending.set(users);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected approve(user: User): void {
    this.decide(user, this.api.approve(user.id));
  }

  protected reject(user: User): void {
    if (this.reasonValid()) this.decide(user, this.api.reject(user.id, this.reason().trim()));
  }

  protected startRejecting(user: User): void {
    this.reason.set('');
    this.rejecting.set(user.id);
  }

  private decide(user: User, call: ReturnType<RegistrationsApi['approve']>): void {
    if (this.busy() !== null) return;
    this.busy.set(user.id);
    this.failed.set(null);
    call.subscribe({
      next: (updated) => {
        this.busy.set(null);
        this.rejecting.set(null);
        this.decided.update((d) => ({ ...d, [user.id]: updated.status === 'ACTIVE' ? 'ACTIVE' : 'REJECTED' }));
      },
      error: () => {
        this.busy.set(null);
        this.failed.set(user.id);
      },
    });
  }
}
