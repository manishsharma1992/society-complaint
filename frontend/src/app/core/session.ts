import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API, SOCIETY_ID } from './api';

export type UserStatus = 'VERIFIED' | 'PENDING' | 'ACTIVE' | 'REJECTED';
export type RoleName = 'RESIDENT' | 'STAFF' | 'TOWER_ADMIN' | 'SOCIETY_ADMIN';

export interface Role {
  role: RoleName;
  societyId: number;
  towerId: number | null;
}

export interface User {
  id: number;
  phone: string;
  name: string | null;
  status: UserStatus;
  roles: Role[];
  claim: { societyId: number; flatId: number; occupancy: 'OWNER' | 'TENANT' } | null;
  rejectionReason: string | null;
  /** runs the platform: decides which add-ons each society may use */
  platformOwner?: boolean;
}

interface Session {
  token: string;
  expiresAt: string;
  user: User;
}

export interface OtpSent {
  sentTo: string;
  expiresInSeconds: number;
  resendAfterSeconds: number;
}

const STORAGE_KEY = 'mycity.session';

/** The logged-in user and their login token. The token stays on this device only. */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly http = inject(HttpClient);
  private readonly session = signal<Session | null>(read());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly token = computed(() => this.session()?.token ?? null);
  readonly signedIn = computed(() => this.session() !== null);

  requestCode(phone: string): Observable<OtpSent> {
    return this.http.post<OtpSent>(`${API}/identity/login/otp`, { phone });
  }

  verify(phone: string, code: string): Observable<Session> {
    return this.http
      .post<Session>(`${API}/identity/login/verify`, { phone, code })
      .pipe(tap((session) => this.store(session)));
  }

  /**
   * Swaps the token for a fresh one with the user's current status and roles (after an approval or a new role),
   * without a new code; it still ends when this login ends.
   */
  renew(): Observable<Session> {
    return this.http.post<Session>(`${API}/identity/me/session`, {}).pipe(tap((session) => this.store(session)));
  }

  /** Self sign-up: who they are and which flat they live in, for the society admin to approve. */
  register(registration: { name: string; flatId: number; occupancy: 'OWNER' | 'TENANT' }): Observable<User> {
    return this.http
      .post<User>(`${API}/identity/me/registration`, { ...registration, societyId: SOCIETY_ID })
      .pipe(tap((user) => this.store({ ...this.session()!, user })));
  }

  signOut(): void {
    this.store(null);
  }

  private store(session: Session | null): void {
    this.session.set(session);
    try {
      if (session) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
      } else {
        localStorage.removeItem(STORAGE_KEY);
      }
    } catch {
      // private mode or blocked storage: the session simply lasts for this tab
    }
  }
}

function read(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const session = raw ? (JSON.parse(raw) as Session) : null;
    return session && new Date(session.expiresAt) > new Date() ? session : null;
  } catch {
    return null;
  }
}
