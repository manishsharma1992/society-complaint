import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, concatMap, from, map, of, shareReplay, switchMap, tap, toArray, catchError } from 'rxjs';
import { API, SOCIETY_ID } from './api';
import { Category, Complaint, ComplaintStatus, Scope } from './models';

export interface NewComplaint {
  scope: Scope;
  flatId: number | null;
  towerId: number | null;
  categoryId: number;
  title: string;
  description: string | null;
}

interface UploadTicket {
  storageKey: string;
  uploadUrl: string;
  method: string;
  headers: Record<string, string>;
}

const BASE = `${API}/complaint/societies/${SOCIETY_ID}`;

@Injectable({ providedIn: 'root' })
export class ComplaintsApi {
  private readonly http = inject(HttpClient);
  private categories$?: Observable<Category[]>;

  list(filters: { status?: ComplaintStatus; towerId?: number; assignedTo?: number } = {}): Observable<Complaint[]> {
    return this.http.get<Complaint[]>(`${BASE}/complaints`, { params: { ...filters, size: 50 } });
  }

  assign(id: number, staffId: number): Observable<Complaint> {
    return this.http.post<Complaint>(`${BASE}/complaints/${id}/assign`, { staffId });
  }

  start(id: number): Observable<Complaint> {
    return this.http.post<Complaint>(`${BASE}/complaints/${id}/start`, {});
  }

  resolve(id: number, note: string | null): Observable<Complaint> {
    return this.http.post<Complaint>(`${BASE}/complaints/${id}/resolve`, note ? { note } : {});
  }

  reject(id: number, reason: string): Observable<Complaint> {
    return this.http.post<Complaint>(`${BASE}/complaints/${id}/reject`, { reason });
  }

  get(id: number): Observable<Complaint> {
    return this.http.get<Complaint>(`${BASE}/complaints/${id}`);
  }

  /** The categories change rarely: fetched once per visit. */
  categories(): Observable<Category[]> {
    this.categories$ ??= this.http.get<Category[]>(`${BASE}/categories`).pipe(shareReplay(1));
    return this.categories$;
  }

  comment(id: number, text: string): Observable<Complaint> {
    return this.http.post<Complaint>(`${BASE}/complaints/${id}/comments`, { text });
  }

  /** The person who raised it confirms the fix. */
  close(id: number): Observable<Complaint> {
    return this.http.post<Complaint>(`${BASE}/complaints/${id}/close`, {});
  }

  raise(complaint: NewComplaint): Observable<Complaint> {
    return this.http.post<Complaint>(`${BASE}/complaints`, complaint);
  }

  /**
   * Uploads photos one after another straight to object storage (never through our server), then attaches each.
   * Emits how many of them made it; a failed photo does not undo the complaint.
   */
  attachPhotos(complaintId: number, photos: Blob[], onEach: (attempted: number) => void = () => {}): Observable<number> {
    return from(photos).pipe(
      concatMap((photo, index) =>
        this.uploadOne(complaintId, photo).pipe(
          map(() => 1),
          catchError(() => of(0)),
          tap(() => onEach(index + 1)),
        ),
      ),
      toArray(),
      map((results) => results.reduce((sum, ok) => sum + ok, 0)),
    );
  }

  private uploadOne(complaintId: number, photo: Blob): Observable<unknown> {
    return this.http
      .post<UploadTicket>(`${BASE}/complaints/${complaintId}/photos/upload-url`, {
        contentType: photo.type,
        sizeBytes: photo.size,
      })
      .pipe(
        switchMap((ticket) =>
          this.http
            .put(ticket.uploadUrl, photo, { headers: ticket.headers, responseType: 'text' })
            .pipe(map(() => ticket.storageKey)),
        ),
        switchMap((storageKey) =>
          this.http.post<Complaint>(`${BASE}/complaints/${complaintId}/photos`, { storageKey }),
        ),
      );
  }
}
