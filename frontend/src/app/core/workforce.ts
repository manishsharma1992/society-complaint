import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, switchMap } from 'rxjs';
import { API, SOCIETY_ID } from './api';

export type Placement = 'INSIDE' | 'OUTSIDE' | 'NO_LOCATION';

/** A clock-in or clock-out: where it happened and the selfie (a short-lived link; null after 90 days). */
export interface ClockEvidence {
  placement: Placement;
  distanceMeters: number | null;
  latitude: number | null;
  longitude: number | null;
  accuracyMeters: number | null;
  selfieUrl: string | null;
}

export interface ShiftReview {
  decision: 'CONFIRMED' | 'DISPUTED';
  reviewerId: number;
  reviewedAt: string;
  reason: string | null;
}

export interface Shift {
  id: number;
  staffId: number;
  startedAt: string;
  /** null while the staff member is on duty */
  endedAt: string | null;
  minutesCounted: number;
  /** null for shifts recorded before selfies were required */
  start: ClockEvidence | null;
  end: ClockEvidence | null;
  review: ShiftReview | null;
  needsAttention: boolean;
}

/** What the phone captured to clock in or out: the selfie and, if allowed, where it was. */
export interface ClockCapture {
  selfie: Blob;
  position: { latitude: number; longitude: number; accuracyMeters: number } | null;
}

interface UploadTicket {
  storageKey: string;
  uploadUrl: string;
  method: string;
  headers: Record<string, string>;
}

export interface ShiftHours {
  staffId: number;
  shifts: number;
  minutesWorked: number;
  onDutyNow: boolean;
}

export interface Period {
  from: string;
  to: string;
}

const BASE = `${API}/workforce/societies/${SOCIETY_ID}/shifts`;

/** Maintenance staff clocking in and out, and the hours they worked. */
@Injectable({ providedIn: 'root' })
export class WorkforceApi {
  private readonly http = inject(HttpClient);

  /** Uploads the selfie straight to the bucket, then clocks in with it and the position. */
  start(capture: ClockCapture): Observable<Shift> {
    return this.withSelfie(capture, (body) => this.http.post<Shift>(`${BASE}/start`, body));
  }

  /** Uploads the selfie straight to the bucket, then clocks out with it and the position. */
  end(capture: ClockCapture): Observable<Shift> {
    return this.withSelfie(capture, (body) => this.http.post<Shift>(`${BASE}/end`, body));
  }

  /** Every staff member's shifts in the period, newest first (admins). */
  list(period: Period): Observable<Shift[]> {
    return this.http.get<Shift[]>(BASE, { params: { ...period } });
  }

  review(shiftId: number, decision: 'CONFIRM' | 'DISPUTE', reason: string | null): Observable<Shift> {
    return this.http.post<Shift>(`${BASE}/${shiftId}/review`, { decision, reason });
  }

  private withSelfie(capture: ClockCapture, clock: (body: object) => Observable<Shift>): Observable<Shift> {
    return this.http.post<UploadTicket>(`${BASE}/selfie-upload`, { sizeBytes: capture.selfie.size }).pipe(
      switchMap((ticket) =>
        this.http
          .put(ticket.uploadUrl, capture.selfie, { headers: ticket.headers, responseType: 'text' })
          .pipe(map(() => ticket.storageKey)),
      ),
      switchMap((selfieKey) => clock({ selfieKey, ...(capture.position ?? {}) })),
    );
  }

  mine(period: Period): Observable<Shift[]> {
    return this.http.get<Shift[]>(`${BASE}/mine`, { params: { ...period } });
  }

  onDuty(): Observable<Shift[]> {
    return this.http.get<Shift[]>(`${BASE}/on-duty`);
  }

  /** Every staff member's hours for admins; only one's own for staff. */
  hours(period: Period): Observable<ShiftHours[]> {
    return this.http.get<ShiftHours[]>(`${BASE}/hours`, { params: { ...period } });
  }
}
