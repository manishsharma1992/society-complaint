# Public homepage and legal pages (`frontend/landing/`)

- Plain HTML, not Angular, so search engines and link previews read it without JavaScript. Nginx serves
  `public/home.html` at `/` and `public/hi/index.html`, `public/mr/index.html` at `/hi/`, `/mr/` (hreflang-linked;
  `robots.txt`, `sitemap.xml` and `og-image.png` sit beside them). Visitors already signed in go to `/start`, which
  sends them on to their home in the app.
- **Feed homepage** (runwalltdfakepromises.com): layout in `landing/template.html`, styles in `landing/feed/`, texts in
  `landing/feed_texts.py` (en, hi and mr must have the same keys). Run `python3 frontend/landing/build_landing.py`; commit its outputs.
  It speaks only of the feed: `scripts/check-sites.mjs` fails if it mentions complaints or staff.
- Voice: the residents' (“You paid for a promise.”). It states no fact about the builder itself: claims come only
  from residents' own moderated posts. The example post on the page is labelled as one.
- **Seva homepage** (gardencityseva.com): layout `landing/seva/home.html`, styles `landing/seva/`, texts `landing/seva_texts.py`.
  Run `python3 frontend/landing/build_seva_home.py`; commit its outputs in `public-seva/`. Its samples are labelled as samples and
  use the app's real status names; never add a figure or a claim that is not true of the app. Only the homepages are open to search engines.
- Font: the feed uses Hedvig Letters Serif and Geist with Noto Serif and Noto Sans Devanagari; Seva uses Chivo Mono with Noto Sans Devanagari.
- Legal pages at `/legal/`, `/hi/legal/`, `/mr/legal/` (Disclaimer, Terms of Use, Privacy Policy, Community
  Guidelines, Grievance Redressal), built by the same script from `landing/legal.html` and `landing/legal_texts.py`
  (a small Markdown subset; English prevails, hi and mr are translations and say so). Courts: Thane. The Privacy
  Policy must list everything the app stores; update it with any new personal data. The Grievance Officer's details
  are "To be announced" until the owner names one. The footer links every section.
- Garden City Seva has its own legal pages (Terms of Use, Privacy Policy, Grievance Redressal) at the same three addresses:
  edit `landing/legal_texts_seva.py`, run `python3 frontend/landing/build_seva_legal.py`, commit its outputs in `public-seva/`.
  Each site's Privacy Policy lists only what that site stores. The sign-in and Help screens link to them (`shared/legal-links.ts`).
- One header and one footer for the feed's homepage and legal pages: `landing/shared.css` plus `header()` and `footer()`
  in `build_landing.py`; change them there, never per page.
- Themes (homepage and legal pages only; the Angular app stays light): automatic (the system's
  `prefers-color-scheme`), light or dark, cycled by the header button and saved as `mycity.theme` in
  localStorage, applied in `<head>` before paint. Colour roles are tokens in `shared.css` (`--surface`,
  `--on-surface`, `--surface-2`, ...); use them, never fixed colours, so every section follows the theme.

