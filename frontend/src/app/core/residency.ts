import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import { API, SOCIETY_ID } from './api';
import { Flat, Tower } from './models';

const BASE = `${API}/residency/societies/${SOCIETY_ID}`;

/** Towers and flats, for labels like "B2-503" and for choosing where a problem is. */
@Injectable({ providedIn: 'root' })
export class ResidencyApi {
  private readonly http = inject(HttpClient);
  private towers$?: Observable<Tower[]>;
  /** Flat labels by flat ID, filled as flats are looked up. */
  readonly flatLabels = signal<Record<number, string>>({});

  towers(): Observable<Tower[]> {
    this.towers$ ??= this.http.get<Tower[]>(`${BASE}/towers`).pipe(shareReplay(1));
    return this.towers$;
  }

  /** Every flat of a tower, for picking one's own when signing up. */
  flatsInTower(towerId: number): Observable<Flat[]> {
    return this.http.get<Flat[]>(`${BASE}/towers/${towerId}/flats`).pipe(tap((flats) => this.remember(flats)));
  }

  flatsOf(userId: number): Observable<Flat[]> {
    return this.http
      .get<Flat[]>(`${BASE}/flats`, { params: { memberId: userId } })
      .pipe(tap((flats) => this.remember(flats)));
  }

  /** Looks up labels for flats not seen yet (an admin's list holds flats of many residents). */
  ensureFlatLabels(ids: number[]): void {
    const known = this.flatLabels();
    for (const id of new Set(ids)) {
      if (!(id in known)) {
        this.http.get<Flat>(`${BASE}/flats/${id}`).subscribe({ next: (flat) => this.remember([flat]), error: () => {} });
      }
    }
  }

  private remember(flats: Flat[]): void {
    this.flatLabels.update((labels) => ({ ...labels, ...Object.fromEntries(flats.map((f) => [f.id, f.label])) }));
  }
}
