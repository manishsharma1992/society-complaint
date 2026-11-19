# Public homepage and legal pages (`frontend/landing/`)

- Plain HTML, not Angular, so search engines and link previews read it without JavaScript. Nginx serves
  `public/home.html` at `/` and `public/hi/index.html`, `public/mr/index.html` at `/hi/`, `/mr/` (hreflang-linked;
  `robots.txt`, `sitemap.xml` and `og-image.png` sit beside them). Visitors already signed in go to `/start`, which
  sends them on to their home in the app.
- Edit `landing/template.html` (layout, styles, script) and the texts in `landing/build_landing.py` (en, hi and mr
  must have the same keys), then run `python3 frontend/landing/build_landing.py`; commit its outputs.
- Voice: the residents' (“You paid for a promise.”). It states no fact about the builder itself: claims come only
  from residents' own moderated posts. The example post on the page is labelled as one.
- Motion (motion.dev, a frontend dev dependency) drives the scroll scenes; the build copies it to
  `public/vendor/motion.js`. With reduced motion, or if it fails to load, every section is a plain, visible screen.
- Font: Noto Sans with Noto Sans Devanagari (Google Fonts), so the three languages match. App screenshots in
  `public/images/app/<lang>/` were captured from the built app with a mock API and sample data; recapture them
  when those screens change. Never use the feed's demo posts in them (they contain invented claims).
- Legal pages at `/legal/`, `/hi/legal/`, `/mr/legal/` (Disclaimer, Terms of Use, Privacy Policy, Community
  Guidelines, Grievance Redressal), built by the same script from `landing/legal.html` and `landing/legal_texts.py`
  (a small Markdown subset; English prevails, hi and mr are translations and say so). Courts: Thane. The Privacy
  Policy must list everything the app stores; update it with any new personal data. The Grievance Officer's details
  are "To be announced" until the owner names one. The footer links every section.
- Garden City Seva has its own legal pages (Terms of Use, Privacy Policy, Grievance Redressal) at the same three addresses:
  edit `landing/legal_texts_seva.py`, run `python3 frontend/landing/build_seva_legal.py`, commit its outputs in `public-seva/`.
  Each site's Privacy Policy lists only what that site stores. The sign-in and Help screens link to them (`shared/legal-links.ts`).
- One header and one footer for the homepage and the legal pages: `landing/shared.css` plus `header()` and `footer()`
  in `build_landing.py`; change them there, never per page. The header is fixed (clear over the homepage photo,
  a frosted bar once the page scrolls, solid when transparency is reduced); pages keep `scroll-padding-top` so
  section jumps and keyboard focus land below it. On a phone the header shows the logo without the name.
- Themes (homepage and legal pages only; the Angular app stays light): automatic (the system's
  `prefers-color-scheme`), light or dark, cycled by the header button and saved as `mycity.theme` in
  localStorage, applied in `<head>` before paint. Colour roles are tokens in `shared.css` (`--surface`,
  `--on-surface`, `--surface-2`, ...); use them, never fixed colours, so every section follows the theme. Photo
  sections keep their dark scrim and light text (`--on-photo`) in both themes.

