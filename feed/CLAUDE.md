# Public Feed context (`feed`)

Loaded when working in `feed/`; the root CLAUDE.md keeps the anonymity rules that apply everywhere.

Purpose: residents publish documented evidence (photos, text, later short videos) of construction defects and
commitments the developer has not met. Published posts are public and easy to share.
Same rules as every context: its own module (library jar packaged into `app`), schema `feed`, endpoints under
`/feed`, DDD and ArchUnit. Cross-context needs (e.g. the poster's tower) go through ports implemented in
`com.mycity.runwal.integration`.
Ubiquitous language: Post, Evidence (a media item), Moderation, Report, Reaction, Takedown.

### Anonymous to the public, verified internally
- Only logged-in, approved residents can post (via `CurrentActor`). No unauthenticated posting: it prevents spam
  and fake posts, and keeps the platform accountable.
- The public sees "Verified resident", plus the tower only if the author chooses to show it.
- The author's `UserId` is stored only in the `feed` schema and must never appear in any API response, DTO, log line
  or public page. Only SOCIETY_ADMIN can see it, and every such access is written to the audit log.
- At most 5 posts per user per day.

### Moderation before publishing
- Post lifecycle (Post aggregate): PENDING_REVIEW -> PUBLISHED or REJECTED; PUBLISHED -> HIDDEN -> PUBLISHED or
  REMOVED. Invalid transitions throw a domain exception; every transition raises a domain event.
- Moderators: SOCIETY_ADMIN for now. Add a scoped MODERATOR role in Identity only when the committee asks for it.
- Content policy, shown to authors before they post and checked by moderators:
  - factual and documented: what, where (tower or common area), when, and what was promised (brochure, agreement,
    approved plan) versus what exists
  - no names, faces, phone numbers or other personal details of individual employees or private people
  - no abusive language, no threats, no unverified allegations of crimes
- Anyone, even without logging in, can report a published post; 3 reports hide it until a moderator reviews it.
- Anyone, even without logging in, can react to a published post with one emoji (THUMBS_UP, HEART, SURPRISED, SAD,
  ANGRY); one per visitor and post, keyed like reports. `PUT`/`DELETE /feed/public/posts/{slug}/reaction`.
- Every moderation action goes to an append-only audit log (who, what, when, why).

### Media storage
- Uploads go to the existing private bucket under `feed/{societyId}/{postId}/<uuid>.<ext>` through presigned URLs,
  as for complaint photos.
- Shared links must keep working, so published media can't use short-lived presigned URLs. When a post becomes
  PUBLISHED, its files are copied to a separate **public** Lightsail bucket served through a Lightsail distribution
  (CDN). When it becomes HIDDEN or REMOVED, the public copies are deleted and the CDN cache is invalidated.
  Media of pending or rejected posts never becomes public.

### Media compression and metadata, in two phases
The server has 512 MB today, which is not enough for libvips or ffmpeg next to the JVM. So:

**Phase 1 (now): images only**
- At most 5 photos per post; JPEG/PNG/WebP; at most 5 MB each; check the type by magic bytes, not by extension;
  reject images over 50 megapixels.
- The Angular app compresses before upload with the existing photo resize in `src/app/shared` (it also strips
  EXIF). Reuse it; do not write a second one.
- Server-side guarantee, because the poster's anonymity must not depend on the client alone: when an upload is
  confirmed, the server streams the object and checks it with the `metadata-extractor` library. If it still
  contains GPS, EXIF device data, XMP or IPTC, the upload is rejected with a clear reason.

**Phase 2 (only after the Lightsail plan is upgraded to at least 1 GB): server-side processing and video**
- Evidence lifecycle: UPLOADED -> PROCESSING -> READY or FAILED. A post can be submitted for moderation only when
  all its evidence is READY.
- Job queue: a DB table in `feed`, polled by a `@Scheduled` worker with `SELECT ... FOR UPDATE SKIP LOCKED`.
  One job at a time; at most 3 retries; timeout 30 s per image and 5 min per video; kill the process on timeout.
  No message broker.
- Ports `ImageProcessor` and `VideoTranscoder` in `application.port`; adapters `VipsImageProcessor` and
  `FfmpegVideoTranscoder` in `infrastructure.media` (CODE-04). Call the tools through `ProcessBuilder` with an
  argument list, never through a shell. Temp files in a dedicated directory, deleted in a `finally` block.
- Images (libvips `vips` CLI; now HEIC is also accepted): auto-rotate, strip all metadata, convert to sRGB, resize
  without upscaling. Outputs: `thumb` 320 px WebP q75, `full` 2048 px WebP q82, `og` 1200x630 JPEG q82
  (for link previews). Target: 150-500 KB per photo.
- Videos (FFmpeg): at most 60 s / 50 MB. Validate with `ffprobe` first: MP4, MOV or WebM, one video stream,
  <= 60 s; never pass an unvalidated file to ffmpeg. Encode H.264 + AAC in MP4 with capped CRF:
  ```
  ffmpeg -hide_banner -nostdin -protocol_whitelist file -i <in>
    -map 0:v:0 -map 0:a:0? -map_metadata -1 -map_chapters -1
    -vf "scale='min(1280,iw)':'min(720,ih)':force_original_aspect_ratio=decrease:force_divisible_by=2,fps='min(30,source_fps)'"
    -c:v libx264 -preset medium -crf 26 -maxrate 2M -bufsize 4M -profile:v high -pix_fmt yuv420p
    -c:a aac -b:a 96k -ac 2
    -movflags +faststart -fflags +bitexact <out.mp4>
  ```
  (If the installed ffmpeg doesn't accept the `fps` expression, keep the intent: cap at 30 fps, never increase.)
  Why: H.264 plays everywhere, including WhatsApp, X and all browsers; CRF gives the best quality per byte;
  `maxrate`/`bufsize` cap shaky, high-motion clips; `+faststart` lets playback start early; `-map_metadata -1`
  removes GPS and device data. Target: 5-15 MB per 60 s clip. Also produce a poster frame at 1 s
  (WebP + 1200x630 JPEG).
- Server setup: install `libvips-tools` (with HEIF/WebP) and `ffmpeg`, and keep them updated, since they parse
  untrusted files.

### Sharing
- Each published post has a stable URL `/p/{slug}`.
- WhatsApp and X read Open Graph tags without running JavaScript, so the Angular app alone gives blank previews.
  Nginx routes `/p/{slug}` to a small public endpoint in the feed context, `GET /feed/share/{slug}`, which returns
  minimal HTML with `og:title`, `og:description` and `og:image` (the public CDN URL), then loads the Angular post
  page. No Angular SSR and no Node process: there is no memory for it on 512 MB.
- Public read endpoints (`/feed/public/**`, `/feed/share/**`) need no login and are cacheable; everything else needs
  the token.

### Frontend
- Screens: Feed and Post (public, no login), New post (residents; shows the content policy first), Moderation queue
  (SOCIETY_ADMIN). All texts in en, hi and mr. Post content written by users is not translated.

### Before launch
- Terms of Use, Community Guidelines (the content policy), Privacy Policy and Grievance Redressal are published in
  all three languages (`/legal/`). Still to do: the Grievance Officer's name and contact, and a report link on
  every post.
- Have a lawyer review the content policy and the takedown process.

### Tests
- Unit tests for the Post and Evidence lifecycles and the report threshold.
- Phase 1: a JPEG with GPS EXIF is rejected; a wrong type disguised by its extension is rejected.
- Phase 2: fixtures with GPS EXIF, an iPhone HEIC, and a rotated portrait video with location metadata. Assert
  output size, dimensions and orientation, and **no GPS or device metadata in any output** (`metadata-extractor`
  for images, `ffprobe` for video).
- No feed API response or public page contains the author's ID or name.

