import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';
import { Observable, forkJoin, map } from 'rxjs';
import { ComplaintsApi } from '../../core/complaints';
import { Complaint } from '../../core/models';
import { Roles } from '../../core/roles';
import { SessionStore } from '../../core/session';
import { ComplaintList, ListState } from '../../shared/complaint-list';
import { ShiftCard } from '../../shared/shift-card';

/** jobs: a staff member's assigned and started work. assign: open complaints waiting for an admin. */
export type Queue = 'jobs' | 'assign';

/** A work queue, most urgent (earliest due) first. */
@Component({
  selector: 'app-queue-page',
  imports: [TranslocoDirective, ComplaintList, ShiftCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      <div class="head">
        <h1>{{ t('queue.' + queue() + '.title') }}</h1>
        <p class="lead">{{ t('queue.' + queue() + '.lead') }}</p>
      </div>
      @if (queue() === 'jobs') {
        <app-shift-card />
      }
      <app-complaint-list [complaints]="complaints()" [state]="state()" (retry)="load()">
        <div class="empty">
          <h2>{{ t('queue.' + queue() + '.emptyTitle') }}</h2>
          <p>{{ t('queue.' + queue() + '.emptyBody') }}</p>
        </div>
      </app-complaint-list>
    </ng-container>
  `,
  styles: `
    :host { display: block; }
    .head { padding-block: var(--space-sm) var(--space-md); display: grid; gap: var(--space-2xs); }
    h1 { font-size: var(--text-2xl); text-wrap: balance; }
    .lead { color: var(--color-ink-2); }
    .empty { padding: var(--space-xl) 0; display: grid; gap: var(--space-md); color: var(--color-ink-2); }
    .empty h2 { font-size: var(--text-xl); text-wrap: balance; }
  `,
})
export default class QueuePage {
  /** From the route's data. */
  readonly queue = input.required<Queue>();

  private readonly api = inject(ComplaintsApi);
  private readonly roles = inject(Roles);
  private readonly me = inject(SessionStore).user;

  protected readonly state = signal<ListState>('loading');
  protected readonly complaints = signal<Complaint[]>([]);

  constructor() {
    effect(() => this.load(this.queue()));
  }

  protected load(queue: Queue = this.queue()): void {
    this.state.set('loading');
    this.fetch(queue)
      .pipe(map((lists) => lists.flat().sort((a, b) => a.dueAt.localeCompare(b.dueAt))))
      .subscribe({
        next: (complaints) => {
          this.complaints.set(complaints);
          this.state.set('ready');
        },
        error: () => this.state.set('error'),
      });
  }

  private fetch(queue: Queue): Observable<Complaint[][]> {
    if (queue === 'jobs') {
      const me = this.me()!.id;
      return forkJoin([
        this.api.list({ assignedTo: me, status: 'ASSIGNED' }),
        this.api.list({ assignedTo: me, status: 'IN_PROGRESS' }),
      ]);
    }
    // a society admin sees every tower; a tower admin only their own
    return this.roles.isSocietyAdmin()
      ? forkJoin([this.api.list({ status: 'OPEN' })])
      : forkJoin(this.roles.adminTowers().map((towerId) => this.api.list({ status: 'OPEN', towerId })));
  }
}
