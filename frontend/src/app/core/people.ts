import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API, SOCIETY_ID } from './api';

export interface Person {
  id: number;
  name: string;
}

/** Names of the people on complaints and comments (names only; phone numbers are never shared). */
@Injectable({ providedIn: 'root' })
export class People {
  private readonly http = inject(HttpClient);
  private readonly asked = new Set<number>();
  readonly names = signal<Record<number, string>>({});

  /** Whom an admin can assign work to (tower admins: for work they pass on, e.g. lifts). */
  staff(role: 'STAFF' | 'TOWER_ADMIN' = 'STAFF'): Observable<Person[]> {
    return this.http
      .get<Person[]>(`${API}/identity/societies/${SOCIETY_ID}/users`, { params: { role } })
      .pipe(tap((people) => this.remember(people)));
  }

  /** Looks up, in one request, the names not known yet. */
  ensure(ids: number[]): void {
    const missing = [...new Set(ids)].filter((id) => !this.asked.has(id));
    if (missing.length === 0) return;
    missing.forEach((id) => this.asked.add(id));
    this.http
      .get<Person[]>(`${API}/identity/societies/${SOCIETY_ID}/users`, { params: { ids: missing.join(',') } })
      .subscribe({
        next: (people) => this.remember(people),
        error: () => missing.forEach((id) => this.asked.delete(id)),
      });
  }

  private remember(people: Person[]): void {
    this.names.update((names) => ({ ...names, ...Object.fromEntries(people.map((p) => [p.id, p.name])) }));
  }
}
