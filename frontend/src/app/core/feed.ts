import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, concatMap, from, map, switchMap } from 'rxjs';
import { API, SOCIETY_ID } from './api';

const BASE = `${API}/feed/societies/${SOCIETY_ID}`;

export interface FeedPhoto {
  /** a short-lived link: every read gives a fresh one */
  url: string;
  widthPx: number;
  heightPx: number;
}

/** A published post in the feed: "Verified resident" of a society; never who wrote it. */
export interface FeedPost {
  slug: string;
  title: string;
  body: string;
  society: string;
  tower: string | null;
  publishedAt: string;
  photos: FeedPhoto[];
}

export type PostStatus = 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED' | 'HIDDEN' | 'REMOVED';

/** A post with its moderation state, for its author and for moderators. */
export interface OwnPost {
  id: number;
  slug: string;
  title: string;
  body: string;
  tower: string | null;
  status: PostStatus;
  submittedAt: string;
  publishedAt: string | null;
  evidence: (FeedPhoto & { contentType: string })[];
  moderation: { status: PostStatus; at: string; reason: string | null }[];
  reports: { reason: string; reportedAt: string }[];
}

export type Decision = 'publish' | 'reject' | 'hide' | 'remove';

interface UploadTicket {
  uploadKey: string;
  uploadUrl: string;
  contentType: string;
}

/** Posts, photos, reports and moderation of the residents' feed. */
@Injectable({ providedIn: 'root' })
export class FeedApi {
  private readonly http = inject(HttpClient);

  feed(page: number, size = 10): Observable<FeedPost[]> {
    return this.http.get<FeedPost[]>(`${BASE}/feed`, { params: { page, size } });
  }

  post(slug: string): Observable<FeedPost> {
    return this.http.get<FeedPost>(`${BASE}/feed/${encodeURIComponent(slug)}`);
  }

  /** @param showTowerId the author's own tower to show on the post, or null to show none */
  submit(title: string, body: string, showTowerId: number | null): Observable<OwnPost> {
    return this.http.post<OwnPost>(`${BASE}/posts`, { title, body, showTowerId });
  }

  /**
   * Uploads each photo straight to the bucket and attaches it, one after another, emitting the post after each;
   * the server checks type, size and metadata.
   */
  attachPhotos(postId: number, photos: Blob[]): Observable<OwnPost> {
    return from(photos).pipe(
      concatMap((photo) =>
        this.http
          .post<UploadTicket>(`${BASE}/posts/${postId}/evidence/upload-url`, { contentType: photo.type, sizeBytes: photo.size })
          .pipe(
            switchMap((ticket) =>
              this.http
                .put(ticket.uploadUrl, photo, { headers: { 'Content-Type': ticket.contentType }, responseType: 'text' })
                .pipe(map(() => ticket.uploadKey)),
            ),
            switchMap((uploadKey) => this.http.post<OwnPost>(`${BASE}/posts/${postId}/evidence`, { uploadKey })),
          ),
      ),
    );
  }

  myPosts(): Observable<OwnPost[]> {
    return this.http.get<OwnPost[]>(`${BASE}/my-posts`, { params: { size: 50 } });
  }

  /** Anyone signed in may report a published post; 3 reports hide it until a moderator looks. */
  report(slug: string, reason: string): Observable<unknown> {
    return this.http.post(`${API}/feed/public/posts/${encodeURIComponent(slug)}/reports`, { reason });
  }

  /** Waiting for review (default), or the posts hidden by reports. */
  queue(status: 'PENDING_REVIEW' | 'HIDDEN' = 'PENDING_REVIEW'): Observable<OwnPost[]> {
    return this.http.get<OwnPost[]>(`${BASE}/moderation`, { params: status === 'PENDING_REVIEW' ? { size: 50 } : { status, size: 50 } });
  }

  decide(postId: number, decision: Decision, reason: string | null): Observable<OwnPost> {
    return this.http.post<OwnPost>(`${BASE}/moderation/${postId}/${decision}`, decision === 'publish' ? {} : { reason });
  }
}
