// After `npm run build:sites`: proves each build holds only its own app, and that every text it uses exists.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

const SITES = {
  seva: {
    dir: 'dist/seva/browser',
    forbidden: [/\/feed\/societies/, /\/feed\/public/, /\/moderation\//, /runwalltdfakepromises/, /Runwal MyCity/, /फ़ीड|फीड/],
    required: [/\/complaint\/societies/, /Garden City Seva/],
  },
  feed: {
    dir: 'dist/feed/browser',
    forbidden: [/\/complaint\/societies/, /\/workforce\//, /gardencityseva/, /Garden City Seva/],
    required: [/\/feed\/public/],
    // the feed's legal pages describe the feed only: no staff shifts
    legalForbidden: /selfie|सेल्फ़ी|सेल्फी|शिफ़्ट|शिफ्ट/i,
    // the feed's homepage says nothing about the complaint app
    homeForbidden: /complaint|plumber|selfie|शिकायत|प्लंबर|सेल्फ़ी|तक्रार|सेल्फी|शिफ़्ट|शिफ्ट/i,
  },
};

const files = (dir) =>
  readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    return statSync(path).isDirectory() ? files(path) : [path];
  });
const leaves = (o, prefix = '') =>
  Object.entries(o).flatMap(([k, v]) => (typeof v === 'string' ? [prefix + k] : leaves(v, `${prefix}${k}.`)));

let failed = 0;
const fail = (site, message) => {
  failed++;
  console.error(`FAIL ${site}: ${message}`);
};

for (const [site, { dir, forbidden, required, legalForbidden, homeForbidden }] of Object.entries(SITES)) {
  const all = files(dir);
  const code = all.filter((f) => f.endsWith('.js')).map((f) => readFileSync(f, 'utf8')).join('\n');
  const i18n = Object.fromEntries(['en', 'hi', 'mr'].map((l) => [l, JSON.parse(readFileSync(join(dir, 'i18n', `${l}.json`), 'utf8'))]));
  const pages = all.filter((f) => f.endsWith('.html')).map((f) => readFileSync(f, 'utf8')).join('\n');
  const text = code + pages + Object.values(i18n).map((j) => JSON.stringify(j)).join('\n');

  if (!all.some((f) => f === join(dir, 'index.html'))) fail(site, 'has no index.html');
  // index.html is the app's own page (login, posts): its description must not speak of the other app either
  for (const page of ['home.html', 'hi/index.html', 'mr/index.html', 'index.html']) {
    const file = join(dir, page);
    if (!all.includes(file)) fail(site, `has no ${page}`);
    else if (homeForbidden?.test(readFileSync(file, 'utf8').replace(/शिकायत निवारण|तक्रार निवारण/g, ''))) fail(site, `${page} mentions ${homeForbidden}`);
  }
  for (const page of ['legal', 'hi/legal', 'mr/legal']) {
    const file = join(dir, page, 'index.html');
    if (!all.includes(file)) fail(site, `has no ${page}/index.html`);
    else if (legalForbidden?.test(readFileSync(file, 'utf8'))) fail(site, `${page} mentions ${legalForbidden}`);
  }
  for (const word of forbidden) if (word.test(text)) fail(site, `contains ${word}`);
  for (const word of required) if (!word.test(text)) fail(site, `is missing ${word}`);

  const keys = new Set(leaves(i18n.en));
  for (const l of ['hi', 'mr']) {
    const other = new Set(leaves(i18n[l]));
    for (const k of keys) if (!other.has(k)) fail(site, `${l}.json lacks ${k}`);
    for (const k of other) if (!keys.has(k)) fail(site, `${l}.json has extra ${k}`);
  }

  // texts the code asks for by their full name, e.g. t("detail.translate")
  const namespaces = Object.keys(i18n.en).join('|');
  const used = new Set([...code.matchAll(new RegExp(`["'\`]((?:${namespaces})\\.[A-Za-z0-9_.]+)["'\`]`, 'g'))].map((m) => m[1]));
  const prefixes = new Set(leaves(i18n.en).flatMap((k) => k.split('.').map((_, i, p) => p.slice(0, i + 1).join('.'))));
  // a name ending in a dot is completed at run time, e.g. "shifts.ranges." + range
  for (const k of used) if (!prefixes.has(k.replace(/\.$/, ''))) fail(site, `code uses missing text ${k}`);

  console.log(`${site}: ${all.length} files, ${used.size} text keys checked`);
}
process.exit(failed ? 1 : 0);
