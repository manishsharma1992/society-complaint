# Frontend (`frontend/`)

- Angular 22 (standalone, zoneless, signals), Node 24 from `frontend/.nvmrc` (use nvm; the system Node is too old).
  UI components: Spartan UI (`libs/ui`, shadcn-style, imported as `@spartan-ng/helm/*`), Tailwind 4, Lucide icons.
- Design: Hallmark "Atelier" theme with the Runwal MyCity brand green. All colours and fonts are tokens in
  `src/theme/tokens.css` (renamed to avoid Spartan clashes: `--color-brand`, `--color-stock`, `--color-ink-muted`);
  `src/styles.css` maps Spartan's shadcn variables onto them. Light mode only. Body text 17px, touch targets 44px+.
- Three languages, chosen by the user and remembered on the device: English, Hindi, Marathi (Transloco,
  `public/i18n/{en,hi,mr}.json`). Every new text goes into all three files with the same keys. Category names are
  translated by their English name (`category.<name>`).
- Structure: `src/app/core` (API services, session, roles, guards), `src/app/shared` (complaint card and list,
  language switch, photo resize), `src/app/features/<screen>`. Screens by role: residents see Complaints and Profile;
  staff also Jobs; admins also To assign; the society admin also Sign-ups. Users who are not yet approved land on
  sign-up (`/register`). The UI only decides what to offer: the server checks every action.
- `ng serve` proxies `/api` to the live server (`proxy.conf.json`). Build: `npx ng build` (budgets in angular.json).
- Deploy: `deploy/deploy.sh frontend` (see deploy/README.md); the backend with `deploy/deploy.sh`. It installs every
  built file before the app's own `./index.html` (only that one is held back, so the homepage's `hi/` and `mr/`
  `index.html` files ship too). Nginx changes (e.g. `location = /`) need `deploy/deploy.sh setup`.
- The app's screens carry `<meta name="robots" content="noindex">`: only the public homepage is for search engines.

