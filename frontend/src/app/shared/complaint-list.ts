import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { forkJoin } from 'rxjs';
import { ComplaintsApi } from '../core/complaints';
import { Complaint, Tower } from '../core/models';
import { ResidencyApi } from '../core/residency';
import { ComplaintCard } from './complaint-card';

export type ListState = 'loading' | 'error' | 'ready';

/**
 * Complaint cards in a grid, each opening its complaint, with the loading and error states every list shares.
 * What to show when there are none is given by the page, as content.
 */
@Component({
  selector: 'app-complaint-list',
  imports: [TranslocoDirective, RouterLink, HlmButtonImports, ComplaintCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container *transloco="let t">
      @switch (state()) {
        @case ('loading') {
          <div class="grid" aria-busy="true">
            @for (i of [1, 2, 3]; track i) {
              <div class="skeleton stock" aria-hidden="true">
                <span class="bar w30"></span>
                <span class="bar w80 tall"></span>
                <span class="bar w60"></span>
                <span class="bar w50"></span>
              </div>
            }
          </div>
        }
        @case ('error') {
          <div class="message" role="alert">
            <p>{{ t('home.error') }}</p>
            <button hlmBtn variant="outline" size="lg" type="button" (click)="retry.emit()">{{ t('home.retry') }}</button>
          </div>
        }
        @default {
          @if (complaints().length === 0) {
            <ng-content />
          } @else {
            <ul class="grid" role="list">
              @for (complaint of complaints(); track complaint.id) {
                <li>
                  <a class="open press" [routerLink]="['/complaints', complaint.id]">
                    <app-complaint-card
                      [complaint]="complaint"
                      [categoryName]="categories()[complaint.categoryId] ?? null"
                      [towerName]="towerName(complaint.towerId)"
                      [where]="complaint.flatId ? flatLabels()[complaint.flatId] ?? null : null" />
                  </a>
                </li>
              }
            </ul>
          }
        }
      }
    </ng-container>
  `,
  styles: `
    :host { display: block; }
    /* the whole card opens the complaint */
    .open { display: block; height: 100%; color: inherit; text-decoration: none; border-radius: var(--radius-card); }
    .open:focus-visible { outline: 2px solid var(--color-focus); outline-offset: 3px; }
    .grid { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--space-md); }
    /* each card arrives a beat after the one above it: 40ms apart, never blocking a tap */
    .grid > li { animation: arrive var(--dur-short) var(--ease-out) both; }
    .grid > li:nth-child(2) { animation-delay: 40ms; }
    .grid > li:nth-child(3) { animation-delay: 80ms; }
    .grid > li:nth-child(n + 4) { animation-delay: 120ms; }
    @keyframes arrive { from { opacity: 0; transform: translateY(6px); } }
    .skeleton { padding: var(--space-md); display: grid; gap: var(--space-sm); }
    .bar { display: block; height: 0.9em; border-radius: var(--radius-pill); background: var(--color-paper-3); }
    .bar.tall { height: 1.3em; }
    .w30 { width: 30%; } .w50 { width: 50%; } .w60 { width: 60%; } .w80 { width: 80%; }
    .message { padding: var(--space-xl) 0; display: grid; gap: var(--space-md); justify-items: start; color: var(--color-ink-2); }
    @media (prefers-reduced-motion: reduce) { .grid > li { animation: none; } }
    @media (min-width: 48rem) { .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (min-width: 80rem) { .grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
  `,
})
export class ComplaintList {
  readonly complaints = input.required<Complaint[]>();
  readonly state = input.required<ListState>();
  readonly retry = output();

  private readonly residency = inject(ResidencyApi);
  protected readonly flatLabels = this.residency.flatLabels;
  /** English category names by ID, the keys for their translations. */
  protected readonly categories = signal<Record<number, string>>({});
  private readonly towers = signal<Record<number, Tower>>({});

  constructor() {
    forkJoin({ categories: inject(ComplaintsApi).categories(), towers: this.residency.towers() }).subscribe({
      next: ({ categories, towers }) => {
        this.categories.set(Object.fromEntries(categories.map((c) => [c.id, c.name])));
        this.towers.set(Object.fromEntries(towers.map((t) => [t.id, t])));
      },
      error: () => {},
    });
    effect(() => this.residency.ensureFlatLabels(this.complaints().filter((c) => c.flatId).map((c) => c.flatId!)));
  }

  protected towerName(id: number | null): string | null {
    const tower = id === null ? null : this.towers()[id];
    return tower ? `${tower.code} ${tower.name}` : null;
  }
}
