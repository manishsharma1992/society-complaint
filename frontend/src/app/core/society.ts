import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, shareReplay, tap } from 'rxjs';
import { API, SOCIETY_ID } from './api';

export type AddOnName = 'FEED' | 'COMPLAINTS';

export interface AddOn {
  addOn: AddOnName;
  /** the platform owner lets the society use it */
  available: boolean;
  /** the society has switched it on */
  on: boolean;
}

export interface Society {
  id: number;
  name: string;
  latitude: number | null;
  longitude: number | null;
  addOns: AddOn[];
}

const BASE = `${API}/residency/societies/${SOCIETY_ID}`;

/**
 * The society and its add-ons, loaded once per visit: they decide the home page and which tabs show. The server
 * refuses everything of an add-on that is off, so this only decides what to offer.
 */
@Injectable({ providedIn: 'root' })
export class SocietyStore {
  private readonly http = inject(HttpClient);
  private load$?: Observable<Society | null>;
  readonly society = signal<Society | null>(null);

  readonly feedOn = computed(() => this.isOn('FEED'));
  readonly complaintsOn = computed(() => this.isOn('COMPLAINTS'));

  /** Loads the society once; on failure the app behaves as before add-ons (both on) rather than locking people out. */
  ensure(): Observable<Society | null> {
    this.load$ ??= this.http.get<Society>(BASE).pipe(
      tap((society) => this.society.set(society)),
      catchError(() => of(null)),
      shareReplay(1),
    );
    return this.load$;
  }

  offer(addOn: AddOnName, available: boolean): Observable<Society> {
    return this.http.put<Society>(`${BASE}/add-ons/${addOn}/availability`, { available }).pipe(tap((s) => this.society.set(s)));
  }

  switch(addOn: AddOnName, on: boolean): Observable<Society> {
    return this.http.put<Society>(`${BASE}/add-ons/${addOn}`, { on }).pipe(tap((s) => this.society.set(s)));
  }

  private isOn(name: AddOnName): boolean {
    const society = this.society();
    return society === null ? true : society.addOns.some((a) => a.addOn === name && a.on);
  }
}

/** For route guards: true once the society is known (whether or not the request worked). */
export const societyLoaded = (store: SocietyStore): Observable<true> => store.ensure().pipe(map(() => true as const));
