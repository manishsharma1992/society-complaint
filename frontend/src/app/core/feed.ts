import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, concatMap, from, map, switchMap } from 'rxjs';
import { API, SOCIETY_ID } from './api';
import { Lang } from './language';

const BASE = `${API}/feed/societies/${SOCIETY_ID}`;
const PUBLIC = `${API}/feed/public`;

export interface FeedPhoto {
  /** a short-lived link: every read gives a fresh one */
  url: string;
  widthPx: number;
  heightPx: number;
}

/** A ready video: the server's copy without metadata, and a frame to show before it plays (short-lived links). */
export interface FeedVideo {
  url: string;
  posterUrl: string;
  widthPx: number;
  heightPx: number;
  durationMs: number;
}

/** What the server accepts for a video, checked again there: one per post, counted with the photos. */
export const VIDEO_TYPES = ['video/mp4', 'video/quicktime', 'video/webm'];
export const MAX_VIDEO_BYTES = 50 * 1024 * 1024;
export const MAX_VIDEO_SECONDS = 60;

export type VideoStatus = 'PROCESSING' | 'READY' | 'FAILED';

/** A video as its author and moderators see it: links only once READY, a reason when FAILED. */
export interface OwnVideo {
  status: VideoStatus;
  url: string | null;
  posterUrl: string | null;
  widthPx: number | null;
  heightPx: number | null;
  durationMs: number | null;
  failureReason: string | null;
}

/** The emoji anyone can react with, in the order they are shown. */
export const EMOJI = [
  { name: 'THUMBS_UP', glyph: '👍' },
  { name: 'HEART', glyph: '❤️' },
  { name: 'SURPRISED', glyph: '😮' },
  { name: 'SAD', glyph: '😢' },
  { name: 'ANGRY', glyph: '😡' },
] as const;

export type Emoji = (typeof EMOJI)[number]['name'];

/** Emoji name -> how many reacted with it; emoji nobody chose are left out. */
export type Reactions = Partial<Record<Emoji, number>>;

/** A published post in the feed: "Verified resident" of a society; never who wrote it. */
export interface FeedPost {
  slug: string;
  title: string;
  body: string;
  /** missing on the public feed, which shows one society */
  society?: string;
  tower: string | null;
  publishedAt: string;
  photos: FeedPhoto[];
  videos: FeedVideo[];
  reactions: Reactions;
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
  videos: OwnVideo[];
  moderation: { status: PostStatus; at: string; reason: string | null }[];
  reports: { reason: string; reportedAt: string }[];
}

export type Decision = 'publish' | 'reject' | 'hide' | 'remove';

interface UploadTicket {
  uploadKey: string;
  uploadUrl: string;
  contentType: string;
}

export interface PostTranslation {
  language: Lang;
  title: string;
  body: string;
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

  /** The public feed: anyone, signed in or not. */
  publicFeed(page: number, size = 10): Observable<FeedPost[]> {
    return this.http.get<FeedPost[]>(`${PUBLIC}/societies/${SOCIETY_ID}/posts`, { params: { page, size } });
  }

  publicPost(slug: string): Observable<FeedPost> {
    return this.http.get<FeedPost>(`${PUBLIC}/posts/${encodeURIComponent(slug)}`);
  }

  /** The post machine-translated into the reader's language (anyone; made once on the server, then stored). */
  translation(slug: string, lang: Lang): Observable<PostTranslation> {
    return this.http.get<PostTranslation>(`${PUBLIC}/posts/${encodeURIComponent(slug)}/translations/${lang}`);
  }

  /** Anyone may react, one emoji per post and visitor; reacting again replaces it. Emits the new counts. */
  react(slug: string, emoji: Emoji): Observable<Reactions> {
    return this.http
      .put<{ reactions: Reactions }>(`${PUBLIC}/posts/${encodeURIComponent(slug)}/reaction`, { emoji })
      .pipe(map((r) => r.reactions));
  }

  takeBackReaction(slug: string): Observable<Reactions> {
    return this.http
      .delete<{ reactions: Reactions }>(`${PUBLIC}/posts/${encodeURIComponent(slug)}/reaction`)
      .pipe(map((r) => r.reactions));
  }

  /** @param showTowerId the author's own tower to show on the post, or null to show none */
  submit(title: string, body: string, showTowerId: number | null): Observable<OwnPost> {
    return this.http.post<OwnPost>(`${BASE}/posts`, { title, body, showTowerId });
  }

  /**
   * Uploads each photo or video straight to the bucket and attaches it, one after another, emitting the post after
   * each; the server checks type, size and metadata, and re-encodes a video before anyone can watch it.
   */
  attachEvidence(postId: number, files: Blob[]): Observable<OwnPost> {
    return from(files).pipe(
      concatMap((file) =>
        this.http
          .post<UploadTicket>(`${BASE}/posts/${postId}/evidence/upload-url`, { contentType: file.type, sizeBytes: file.size })
          .pipe(
            switchMap((ticket) =>
              this.http
                .put(ticket.uploadUrl, file, { headers: { 'Content-Type': ticket.contentType }, responseType: 'text' })
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

  /** Anyone, signed in or not, may report a published post; 3 reports hide it until a moderator looks. */
  report(slug: string, reason: string): Observable<unknown> {
    return this.http.post(`${PUBLIC}/posts/${encodeURIComponent(slug)}/reports`, { reason });
  }

  /** Waiting for review (default), or the posts hidden by reports. */
  queue(status: 'PENDING_REVIEW' | 'HIDDEN' = 'PENDING_REVIEW'): Observable<OwnPost[]> {
    return this.http.get<OwnPost[]>(`${BASE}/moderation`, { params: status === 'PENDING_REVIEW' ? { size: 50 } : { status, size: 50 } });
  }

  decide(postId: number, decision: Decision, reason: string | null): Observable<OwnPost> {
    return this.http.post<OwnPost>(`${BASE}/moderation/${postId}/${decision}`, decision === 'publish' ? {} : { reason });
  }
}
