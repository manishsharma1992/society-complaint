"""Writes Garden City Seva's legal pages into public-seva/ from legal_texts_seva.py.
Run after changing a text:  python3 frontend/landing/build_seva_legal.py
"""
from html import escape as e
from pathlib import Path

from build_landing import md
from legal_texts_seva import LEGAL, UI

OUT = Path(__file__).parent.parent / "public-seva"
NAMES = {"en": "English", "hi": "हिन्दी", "mr": "मराठी"}

PAGE = """<!doctype html>
<html lang="{lang}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
  <title>{title} · Garden City Seva</title>
  <meta name="description" content="{meta}">
  <meta name="robots" content="noindex">
  <link rel="icon" href="/favicon.svg" type="image/svg+xml">
  <link rel="apple-touch-icon" href="/apple-touch-icon.png">
  <style>
    :root {{ color-scheme: light; --ink: #1c2420; --muted: #55605a; --surface: #fbfcfa; --surface-2: #eef2ec; --brand: #14642a; }}
    *, *::before, *::after {{ box-sizing: border-box; }}
    html {{ -webkit-text-size-adjust: 100%; scroll-padding-top: 24px; }}
    body {{ margin: 0; background: var(--surface); color: var(--ink); font: 400 1.0625rem/1.7 system-ui, "Noto Sans Devanagari", sans-serif; }}
    a {{ color: var(--brand); text-underline-offset: 0.18em; }}
    a:focus-visible {{ outline: 2px solid var(--brand); outline-offset: 3px; }}
    header, main {{ max-width: 46rem; margin-inline: auto; padding-inline: 16px; }}
    header {{ display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 4px 16px; padding-block: 12px; }}
    .brand {{ font-weight: 700; text-decoration: none; color: var(--ink); }}
    nav a, .brand {{ display: inline-flex; align-items: center; min-height: 44px; }}
    .tools {{ display: flex; flex-wrap: wrap; gap: 0 14px; }}
    .tools [aria-current] {{ font-weight: 700; text-decoration: none; color: var(--ink); }}
    main {{ padding-block: 24px 72px; }}
    h1 {{ font-size: 2.25rem; line-height: 1.2; margin: 0; }}
    .updated {{ color: var(--muted); margin: 8px 0 0; }}
    .note {{ margin: 20px 0 0; padding: 14px 18px; border-radius: 12px; background: var(--surface-2); }}
    .toc {{ list-style: none; display: flex; flex-wrap: wrap; gap: 8px; margin: 28px 0 0; padding: 0; }}
    .toc a {{ padding: 0 16px; border-radius: 999px; background: var(--surface-2); color: var(--ink); font-weight: 600; font-size: 0.9375rem; text-decoration: none; }}
    section {{ margin-top: 56px; }}
    h2 {{ font-size: 1.625rem; line-height: 1.3; margin: 0 0 14px; }}
    main p, main ul, main ol {{ margin: 0 0 14px; }}
    section ul, section ol {{ padding-inline-start: 1.4em; }}
    section li + li {{ margin-top: 6px; }}
    section li ul {{ margin: 8px 0 0; }}
  </style>
</head>
<body>
  <header>
    <a class="brand" href="{home}">Garden City Seva</a>
    <nav class="tools" aria-label="{title}">{langs}<a href="/login">{signin}</a></nav>
  </header>
  <main>
    <h1>{title}</h1>
    <p class="updated">{updated}</p>
    {note}
    <nav aria-label="{title}"><ul class="toc">{toc}</ul></nav>
    {sections}
  </main>
</body>
</html>
"""


def path(lang):
    return "/legal/" if lang == "en" else f"/{lang}/legal/"


def render(lang):
    ui = UI[lang]
    langs = "".join(f'<a href="{path(l)}" hreflang="{l}" lang="{l}"{" aria-current=page" if l == lang else ""}>{name}</a>'
                    for l, name in NAMES.items())
    note = f'<p class="note">{e(ui["note"])} <a href="/legal/" hreflang="en" lang="en">{e(ui["english"])}</a></p>' if ui["note"] else ""
    toc = "".join(f'<li><a href="#{sid}">{e(title)}</a></li>' for sid, title, _ in LEGAL[lang])
    sections = "\n    ".join(f'<section id="{sid}" aria-labelledby="{sid}-h"><h2 id="{sid}-h">{e(title)}</h2>{md(body)}</section>'
                             for sid, title, body in LEGAL[lang])
    return PAGE.format(lang=lang, home="/" if lang == "en" else f"/{lang}/", title=e(ui["title"]), meta=e(ui["meta"]), signin=e(ui["signin"]), updated=e(ui["updated"]),
                       langs=langs, note=note, toc=toc, sections=sections)


if __name__ == "__main__":
    for lang in LEGAL:
        assert [s[0] for s in LEGAL[lang]] == [s[0] for s in LEGAL["en"]], f"{lang} legal sections differ"
        target = OUT / path(lang).strip("/") / "index.html"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(render(lang), encoding="utf-8")
        print("wrote", target.relative_to(OUT.parent))
