import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API, SOCIETY_ID } from './api';

export interface Shift {
  id: number;
  staffId: number;
  startedAt: string;
  /** null while the staff member is on duty */
  endedAt: string | null;
  minutesCounted: number;
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

  start(): Observable<Shift> {
    return this.http.post<Shift>(`${BASE}/start`, {});
  }

  end(): Observable<Shift> {
    return this.http.post<Shift>(`${BASE}/end`, {});
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
