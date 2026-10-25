import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucidePlus } from '@ng-icons/lucide';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { ComplaintsApi } from '../../core/complaints';
import { Complaint } from '../../core/models';
import { SessionStore } from '../../core/session';
import { ComplaintList, ListState } from '../../shared/complaint-list';

/** The complaints this person may see (a resident: their flats', their own and the common areas'), newest first. */
@Component({
  selector: 'app-complaints-page',
  imports: [TranslocoDirective, RouterLink, NgIcon, HlmButtonImports, ComplaintList],
  providers: [provideIcons({ lucidePlus })],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './complaints.page.html',
  styleUrl: './complaints.page.css',
})
export default class ComplaintsPage {
  private readonly complaintsApi = inject(ComplaintsApi);
  protected readonly user = inject(SessionStore).user;

  protected readonly state = signal<ListState>('loading');
  protected readonly complaints = signal<Complaint[]>([]);
  protected readonly canRaise = computed(() => this.user()?.status === 'ACTIVE');

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.complaintsApi.list().subscribe({
      next: (complaints) => {
        this.complaints.set(complaints);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }
}
