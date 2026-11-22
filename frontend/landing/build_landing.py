"""Writes the public homepage in English, Hindi and Marathi from template.html.

Run after changing a text:  python3 frontend/landing/build_landing.py
Output: public/home.html (served at /), public/hi/index.html, public/mr/index.html.
"""
import html
import json
import re
from pathlib import Path

from feed_texts import TEXTS
from legal_texts import LEGAL, UI, UPDATED

HERE = Path(__file__).parent
PUBLIC = HERE.parent / "public"
SITE = "https://runwalltdfakepromises.com"
FONTS = ("https://fonts.googleapis.com/css2?family=Newsreader:opsz,wght@6..72,400;6..72,500&family=Inter:wght@400;500;600"
         "&family=Noto+Serif+Devanagari:wght@400;500&family=Noto+Sans+Devanagari:wght@400;500;600&display=swap")
STAMP = ("/* tasteskill v2 · direction: editorial-report (2026-11-22) · type and colour kept: DESIGN.md editorial system "
         "in the brand green · idea: what was promised sits in the margin, what is there today runs in the column */\n")

LOGO = ('<svg viewBox="72 24 112 216" aria-hidden="true" focusable="false"><path fill="currentColor" fill-rule="evenodd" '
        'd="M92 24 H164 A20 20 0 0 1 184 44 V188 A20 20 0 0 1 164 208 H126 L92 238 V208 A20 20 0 0 1 72 188 V44 '
        'A20 20 0 0 1 92 24 Z M96 56 h24 v24 h-24 Z M136 56 h24 v24 h-24 Z M96 100 h24 v24 h-24 Z M136 100 h24 v24 '
        'h-24 Z M96 144 h24 v24 h-24 Z M136 144 h24 v24 h-24 Z"/></svg>')


e = html.escape


def json_ld(t):
    """WebSite + WebApplication: what the site is, for search engines."""
    return json.dumps({
        "@context": "https://schema.org",
        "@graph": [
            {"@type": "WebSite", "@id": SITE + "/#website", "url": SITE + "/", "name": "Runwal MyCity Residents",
             "inLanguage": ["en", "hi", "mr"]},
            {"@type": "WebApplication", "name": "Runwal MyCity Residents", "url": SITE + t["path"],
             "description": t["meta_description"], "inLanguage": t["path"].strip("/") or "en",
             "applicationCategory": "LifestyleApplication", "operatingSystem": "Web browser",
             "areaServed": {"@type": "Place", "name": "Runwal MyCity, Dombivli East, Thane, Maharashtra, India"}},
        ],
    }, ensure_ascii=False)


def md(text):
    """The legal texts' Markdown subset: paragraphs (line breaks kept), "- " lists, "1. " lists with "   - " sub-items, **bold**."""
    inline = lambda x: re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", e(x))
    out = []
    for block in text.strip().split("\n\n"):
        lines = block.split("\n")
        if all(l.startswith("- ") for l in lines):
            out.append("<ul>" + "".join(f"<li>{inline(l[2:])}</li>" for l in lines) + "</ul>")
        elif re.match(r"\d+\. ", lines[0]):
            items = []
            for l in lines:
                if re.match(r"\d+\. ", l):
                    items.append([inline(l.split(". ", 1)[1]), []])
                else:
                    items[-1][1].append(f"<li>{inline(l.strip()[2:])}</li>")
            out.append("<ol>" + "".join(f"<li>{t}" + (f"<ul>{''.join(sub)}</ul>" if sub else "") + "</li>" for t, sub in items) + "</ol>")
        else:
            out.append("<p>" + "<br>".join(inline(l) for l in lines) + "</p>")
    return "\n".join(out)


assert md("A **b** & c\n\n- x\n- y\n\n1. one\n   - sub\n2. two") == (
    "<p>A <strong>b</strong> &amp; c</p>\n<ul><li>x</li><li>y</li></ul>\n"
    "<ol><li>one<ul><li>sub</li></ul></li><li>two</li></ol>")


# ---- shared by the homepage and the legal pages: theme, header, footer ----
THEME = {"en": ("Theme: automatic", "Theme: light", "Theme: dark"),
         "hi": ("थीम: अपने आप", "थीम: हल्की", "थीम: गहरी"),
         "mr": ("थीम: आपोआप", "थीम: फिकट", "थीम: गडद")}

ICONS = ('<svg class="i-auto" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="12" cy="12" r="9"/>'
         '<path d="M12 3a9 9 0 0 1 0 18z" fill="currentColor"/></svg>'
         '<svg class="i-light" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">'
         '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>'
         '<svg class="i-dark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" aria-hidden="true">'
         '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>')

# runs in <head> before anything paints: a saved choice wins, otherwise the system's (CSS prefers-color-scheme)
THEME_BOOT = ("try { var m = localStorage.getItem('mycity.theme'); "
              "if (m === 'light' || m === 'dark') document.documentElement.dataset.theme = m; } catch (e) {}")

SHARED_JS = """
    (function () {
      // the header turns into a solid bar once the page scrolls
      var top = document.querySelector('.top');
      var mark = function () { top.classList.toggle('scrolled', scrollY > 8); };
      addEventListener('scroll', mark, { passive: true }); mark();
      // the theme button: automatic (the system's setting) -> light -> dark
      var root = document.documentElement, button = document.querySelector('.theme'), modes = ['auto', 'light', 'dark'];
      var current = function () { var t = root.dataset.theme; return t === 'light' || t === 'dark' ? t : 'auto'; };
      var show = function (m) { var label = button.getAttribute('data-label-' + m); button.dataset.mode = m; button.setAttribute('aria-label', label); button.title = label; };
      show(current());
      button.addEventListener('click', function () {
        var m = modes[(modes.indexOf(current()) + 1) % modes.length];
        if (m === 'auto') delete root.dataset.theme; else root.dataset.theme = m;
        try { if (m === 'auto') localStorage.removeItem('mycity.theme'); else localStorage.setItem('mycity.theme', m); } catch (e) {}
        show(m);
      });
    })();
"""


def lang_paths(kind):
    return {"en": "/", "hi": "/hi/", "mr": "/mr/"} if kind == "home" else {"en": "/legal/", "hi": "/hi/legal/", "mr": "/mr/legal/"}


def header(lang, t, kind):
    names = {"en": "English", "hi": "हिन्दी", "mr": "मराठी"}
    current = ' aria-current="page"'
    langs = "".join(f'<a href="{href}" hreflang="{l}" lang="{l}"{current if l == lang else ""}>{names[l]}</a>'
                    for l, href in lang_paths(kind).items())
    auto, light, dark = (e(x) for x in THEME[lang])
    return (f'<a class="skip" href="#main">{e(t["skip"])}</a>\n'
            f'  <header class="top"><div class="shell">'
            f'<a class="brand" href="{t["path"]}" aria-label="Runwal MyCity">{LOGO}<span translate="no">Runwal MyCity</span></a>'
            f'<nav class="tools" aria-label="{e(t["language"])}">{langs}'
            f'<button type="button" class="theme" data-mode="auto" data-label-auto="{auto}" data-label-light="{light}" data-label-dark="{dark}" '
            f'aria-label="{auto}" title="{auto}">{ICONS}</button>'
            f'<a class="btn" href="/login">{e(t["nav_signin"])}</a></nav></div></header>')


def footer(lang, t, kind):
    names = {"en": "English", "hi": "हिन्दी", "mr": "मराठी"}
    li = lambda href, text, extra="": f'<li><a href="{href}"{extra}>{e(text)}</a></li>'
    legal_base = "" if kind == "legal" else legal_path(lang)
    legal = "".join(li(f"{legal_base}#{sid}", title) for sid, title, _ in LEGAL[lang])
    langs = "".join(li(href, names[l], f' hreflang="{l}" lang="{l}"') for l, href in lang_paths(kind).items())
    if kind == "home":
        site = "".join(li(h, t[k]) for h, k in (("/public", "nav_posts"), ("#why", "nav_why"), ("#faq", "nav_faq"), ("/login", "nav_signin")))
    else:
        site = li(t["path"], UI[lang]["back_home"]) + li("/public", t["nav_posts"]) + li("/login", t["nav_signin"])
    updated = f"<span>{e(UPDATED[lang])}</span>" if kind == "legal" else ""
    return (f'<footer class="site-foot"><div class="shell foot">'
            f'<div class="about"><a class="brand" href="{t["path"]}">{LOGO}<span translate="no">Runwal MyCity</span></a><p>{e(t["footer_independent"])}</p></div>'
            f'<nav aria-labelledby="f-legal"><h2 class="foot-h" id="f-legal">{e(UI[lang]["legal_title"])}</h2><ul>{legal}</ul></nav>'
            f'<nav aria-labelledby="f-lang"><h2 class="foot-h" id="f-lang">{e(t["foot_lang"])}</h2><ul>{langs}</ul></nav>'
            f'<nav aria-labelledby="f-site"><h2 class="foot-h" id="f-site">Runwal MyCity</h2><ul>{site}</ul></nav>'
            f'</div><div class="base"><div class="shell"><span>{e(t["footer_place"])}</span>{updated}</div></div></footer>')


def shared_values(lang, t, kind):
    return {"shared_css": (HERE / "shared.css").read_text(encoding="utf-8"), "shared_js": SHARED_JS, "theme_boot": THEME_BOOT,
            "header": header(lang, t, kind), "footer": footer(lang, t, kind)}


def render_legal(lang, t):
    page = (HERE / "legal.html").read_text(encoding="utf-8")
    ui = UI[lang]
    note = (f'<p class="note">{e(ui["legal_note"])} <a href="/legal/" hreflang="en" lang="en">{e(ui["legal_note_link"])}</a></p>'
            if ui["legal_note"] else "")
    values = {**{k: e(v) for k, v in ui.items()}, "lang": lang, "logo": LOGO, "path": t["path"], "language": e(t["language"]), "fonts": e(FONTS),
              "legal_path": legal_path(lang), "updated": e(UPDATED[lang]), "note": note,
              "footer_independent": e(t["footer_independent"]),
              "footer_place": e(t["footer_place"]), "nav_signin": e(t["nav_signin"]), "skip": e(t["skip"]),
              "toc": "".join(f'<li><a href="#{sid}">{e(title)}</a></li>' for sid, title, _ in LEGAL[lang]),
              "sections": "\n    ".join(f'<section id="{sid}" aria-labelledby="{sid}-h"><h2 id="{sid}-h">{e(title)}</h2>{md(body)}</section>'
                                          for sid, title, body in LEGAL[lang]),
              "cur_en": ' aria-current="page"' if lang == "en" else "",
              "cur_hi": ' aria-current="page"' if lang == "hi" else "",
              "cur_mr": ' aria-current="page"' if lang == "mr" else ""}
    values.update(shared_values(lang, t, "legal"))
    out = re.sub(r"\{\{(\w+)\}\}", lambda m: values[m.group(1)], page)
    assert "{{" not in out, "a placeholder has no text"
    return out


def legal_path(lang):
    return "/legal/" if lang == "en" else f"/{lang}/legal/"


def rules(lang):
    """The two lists of the Community Guidelines: what a post must do, and what is removed."""
    _, title, body = next(section for section in LEGAL[lang] if section[0] == "guidelines")
    keep, keep_items, drop, drop_items = body.strip().split("\n\n")[:4]
    items = lambda block: "".join(f"<li>{e(line[2:])}</li>" for line in block.split("\n"))
    return {"rules_title": e(title), "rules_keep": e(keep.rstrip(":：")), "rules_keep_items": items(keep_items),
            "rules_drop": e(drop.rstrip(":：")), "rules_drop_items": items(drop_items)}


def render(lang, t):
    page = (HERE / "template.html").read_text(encoding="utf-8")
    css = STAMP + "".join((HERE / f).read_text(encoding="utf-8") for f in ("feed/tokens.css", "feed/base.css", "shared.css", "feed/page.css"))
    values = {**{k: e(v) for k, v in t.items() if isinstance(v, str)}, "lang": lang, "json_ld": json_ld(t), "fonts": e(FONTS), "css": css, **rules(lang),
              "legal_guidelines": legal_path(lang) + "#guidelines"}
    values.update(shared_values(lang, t, "home"))
    out = re.sub(r"\{\{(\w+)\}\}", lambda m: values[m.group(1)], page)
    assert "{{" not in out, "a placeholder has no text"
    return out


if __name__ == "__main__":
    targets = {"en": PUBLIC / "home.html", "hi": PUBLIC / "hi" / "index.html", "mr": PUBLIC / "mr" / "index.html"}
    keys = set(TEXTS["en"])
    for lang, t in TEXTS.items():
        assert set(t) == keys, f"{lang} texts differ: {keys ^ set(t)}"
        targets[lang].parent.mkdir(parents=True, exist_ok=True)
        targets[lang].write_text(render(lang, t), encoding="utf-8")
        print("wrote", targets[lang].relative_to(HERE.parent))
        legal = PUBLIC / legal_path(lang).strip("/") / "index.html"
        assert [sid for sid, _, _ in LEGAL[lang]] == [sid for sid, _, _ in LEGAL["en"]], f"{lang} legal sections differ"
        legal.parent.mkdir(parents=True, exist_ok=True)
        legal.write_text(render_legal(lang, t), encoding="utf-8")
        print("wrote", legal.relative_to(HERE.parent))
