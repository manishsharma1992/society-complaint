import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, SOCIETY_ID } from './api';
import { User } from './session';

const BASE = `${API}/identity/societies/${SOCIETY_ID}/registrations`;

/** People who signed up and claimed a flat, waiting for the society admin. */
@Injectable({ providedIn: 'root' })
export class RegistrationsApi {
  private readonly http = inject(HttpClient);

  pending(): Observable<User[]> {
    return this.http.get<User[]>(BASE);
  }

  approve(userId: number): Observable<User> {
    return this.http.post<User>(`${BASE}/${userId}/approve`, {});
  }

  reject(userId: number, reason: string): Observable<User> {
    return this.http.post<User>(`${BASE}/${userId}/reject`, { reason });
  }
}
