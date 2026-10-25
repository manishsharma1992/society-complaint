import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, ElementRef, computed, effect, inject, input, signal, viewChild } from '@angular/core';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideChevronLeft, lucideX } from '@ng-icons/lucide';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmSpinnerImports } from '@spartan-ng/helm/spinner';
import { Observable, forkJoin } from 'rxjs';
import { ComplaintsApi } from '../../core/complaints';
import { dateTime } from '../../core/format';
import { LanguageService } from '../../core/language';
import { Complaint, isOngoing } from '../../core/models';
import { People, Person } from '../../core/people';
import { Roles } from '../../core/roles';
import { ResidencyApi } from '../../core/residency';
import { SessionStore } from '../../core/session';
import { ComplaintCard } from '../../shared/complaint-card';

type Load = 'loading' | 'missing' | 'error' | 'ready';
/** The one move this person can make next, from the complaint's status and their role. */
type NextStep = 'assign' | 'start' | 'resolve' | 'close' | null;

/** One complaint: its card, photos, what happened to it so far, and the conversation about it. */
@Component({
  selector: 'app-complaint-page',
  imports: [TranslocoDirective, NgIcon, HlmButtonImports, HlmSpinnerImports, ComplaintCard],
  providers: [provideIcons({ lucideChevronLeft, lucideX })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './complaint.page.html',
  styleUrl: './complaint.page.css',
})
export default class ComplaintPage {
  /** From the route, /complaints/:id. */
  readonly id = input.required<string>();

  private readonly api = inject(ComplaintsApi);
  private readonly residency = inject(ResidencyApi);
  private readonly people = inject(People);
  private readonly lang = inject(LanguageService).current;
  private readonly me = inject(SessionStore).user;
  private readonly roles = inject(Roles);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  protected readonly state = signal<Load>('loading');
  protected readonly complaint = signal<Complaint | null>(null);
  protected readonly categoryName = signal<string | null>(null);
  protected readonly towerName = signal<string | null>(null);
  protected readonly flatLabels = this.residency.flatLabels;

  protected readonly draft = signal('');
  protected readonly sending = signal(false);
  protected readonly commentError = signal<string | null>(null);
  protected readonly acting = signal(false);
  protected readonly actionError = signal(false);
  protected readonly rejecting = signal(false);
  protected readonly reason = signal('');
  protected readonly resolveNote = signal('');
  protected readonly staff = signal<Person[]>([]);
  protected readonly chosenStaff = signal<number | null>(null);

  protected readonly openPhoto = signal<number | null>(null);
  private readonly viewer = viewChild<ElementRef<HTMLDialogElement>>('viewer');

  protected readonly ongoing = computed(() => {
    const complaint = this.complaint();
    return complaint !== null && isOngoing(complaint.status);
  });
  /** A mirror of the server's rules (ComplaintAccessPolicy), only to decide what to offer. */
  protected readonly manages = computed(() => {
    const complaint = this.complaint();
    return complaint !== null && this.roles.manages(complaint.towerId);
  });
  protected readonly raisedIt = computed(() => this.complaint()?.raisedBy === this.me()?.id);
  protected readonly nextStep = computed<NextStep>(() => {
    const complaint = this.complaint();
    if (!complaint) return null;
    const assignee = complaint.assignedTo !== null && complaint.assignedTo === this.me()?.id;
    switch (complaint.status) {
      case 'OPEN':
        return this.manages() ? 'assign' : null;
      case 'ASSIGNED':
        return assignee || this.manages() ? 'start' : null;
      case 'IN_PROGRESS':
        return assignee || this.manages() ? 'resolve' : null;
      case 'RESOLVED':
        return this.raisedIt() || this.manages() ? 'close' : null;
      default:
        return null;
    }
  });
  /** Open and assigned complaints can still be turned down, by an admin, with a reason. */
  protected readonly mayReject = computed(() => {
    const status = this.complaint()?.status;
    return this.manages() && (status === 'OPEN' || status === 'ASSIGNED');
  });
  protected readonly reasonValid = computed(() => this.reason().trim().length > 0 && this.reason().trim().length <= 500);
  /** Newest first: what happened last is what people come to see. */
  protected readonly history = computed(() => [...(this.complaint()?.history ?? [])].reverse());
  protected readonly draftValid = computed(() => this.draft().trim().length > 0 && this.draft().trim().length <= 1000);

  constructor() {
    effect(() => this.load(Number(this.id())));
    if (this.roles.isAdmin()) {
      this.people.staff().subscribe({ next: (staff) => this.staff.set(staff), error: () => {} });
    }
  }

  protected load(id: number = Number(this.id())): void {
    this.state.set('loading');
    forkJoin({ complaint: this.api.get(id), categories: this.api.categories(), towers: this.residency.towers() }).subscribe({
      next: ({ complaint, categories, towers }) => {
        this.categoryName.set(categories.find((c) => c.id === complaint.categoryId)?.name ?? null);
        const tower = towers.find((t) => t.id === complaint.towerId);
        this.towerName.set(tower ? `${tower.code} ${tower.name}` : null);
        if (complaint.flatId) this.residency.ensureFlatLabels([complaint.flatId]);
        this.show(complaint);
        this.state.set('ready');
      },
      error: (e: unknown) =>
        this.state.set(e instanceof HttpErrorResponse && (e.status === 404 || e.status === 403) ? 'missing' : 'error'),
    });
  }

  protected who(userId: number | null): { key: string; name?: string } {
    if (userId === null) return { key: 'detail.someone' };
    if (userId === this.me()?.id) return { key: 'detail.you' };
    return { key: 'detail.person', name: this.people.names()[userId] ?? '…' };
  }

  protected when(iso: string): string {
    return dateTime(iso, this.lang());
  }

  protected send(): void {
    const complaint = this.complaint();
    if (!complaint || !this.draftValid() || this.sending()) return;
    this.sending.set(true);
    this.commentError.set(null);
    this.api.comment(complaint.id, this.draft().trim()).subscribe({
      next: (updated) => {
        this.sending.set(false);
        this.draft.set('');
        this.show(updated);
      },
      error: (e: unknown) => {
        this.sending.set(false);
        this.commentError.set(e instanceof HttpErrorResponse && e.status === 422 ? 'detail.commentsOff' : 'detail.commentFailed');
      },
    });
  }

  protected assign(): void {
    const staffId = this.chosenStaff();
    if (staffId !== null) this.act((id) => this.api.assign(id, staffId));
  }

  protected start(): void {
    this.act((id) => this.api.start(id));
  }

  protected resolve(): void {
    this.act((id) => this.api.resolve(id, this.resolveNote().trim() || null));
  }

  protected close(): void {
    this.act((id) => this.api.close(id));
  }

  protected reject(): void {
    if (this.reasonValid()) this.act((id) => this.api.reject(id, this.reason().trim()));
  }

  private act(call: (id: number) => Observable<Complaint>): void {
    const complaint = this.complaint();
    if (!complaint || this.acting()) return;
    this.acting.set(true);
    this.actionError.set(false);
    call(complaint.id).subscribe({
      next: (updated) => {
        this.acting.set(false);
        this.rejecting.set(false);
        this.reason.set('');
        this.resolveNote.set('');
        this.chosenStaff.set(null);
        this.show(updated);
      },
      error: () => {
        this.acting.set(false);
        this.actionError.set(true);
      },
    });
  }

  /** Back to the list this came from (jobs, to assign, complaints); a shared link goes to the person's home. */
  protected back(): void {
    if (this.router.lastSuccessfulNavigation()?.previousNavigation) {
      this.location.back();
    } else {
      void this.router.navigate(['/']);
    }
  }

  protected view(index: number): void {
    this.openPhoto.set(index);
    this.viewer()?.nativeElement.showModal();
  }

  protected closeViewer(): void {
    this.viewer()?.nativeElement.close();
  }

  private show(complaint: Complaint): void {
    this.complaint.set(complaint);
    this.people.ensure([
      complaint.raisedBy,
      ...(complaint.assignedTo ? [complaint.assignedTo] : []),
      ...complaint.history.map((h) => h.changedBy),
      ...complaint.comments.map((c) => c.authorId),
    ]);
  }
}
