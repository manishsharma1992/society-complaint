"""Writes Garden City Seva's public homepage into public-seva/ from seva/home.html and seva_texts.py.
Run after changing a text:  python3 frontend/landing/build_seva_home.py
Output: public-seva/home.html (served at /), public-seva/hi/index.html, public-seva/mr/index.html, sitemap.xml.
"""
import json
import re
from html import escape as e
from pathlib import Path

from seva_texts import TEXTS

HERE = Path(__file__).parent
OUT = HERE.parent / "public-seva"
SITE = "https://gardencityseva.com"
NAMES = {"en": "EN", "hi": "हिन्दी", "mr": "मराठी"}
FULL = {"en": "English", "hi": "हिन्दी", "mr": "मराठी"}
CHECK = '<svg viewBox="0 0 10 10" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M1.5 5.5 4 8l4.5-6"/></svg>'
STAMP = ("/* Hallmark · v2.0.0 · theme: wayfind (dealt 3/3, seed garden-city-seva-housing-808e·2c8c8255·2026-11-19·r0) · wayfind: platform "
         "· genre: modern-minimal · shape: Narrative Workflow · draw: f93868 · signature: a sample complaint moved along its real statuses */\n")


def logo():
    """The leaf-and-home mark of the app (shared/logo-mark.ts), drawn in the current colour."""
    source = (HERE.parent / "src/app/shared/logo-mark.ts").read_text(encoding="utf-8")
    svg = re.search(r"<svg.*?</svg>", source, re.S).group(0)
    return re.sub(r"\s*\n\s*", " ", svg)


def render(lang, t):
    page = (HERE / "seva/home.html").read_text(encoding="utf-8")
    css = STAMP + "".join((HERE / "seva" / f).read_text(encoding="utf-8") for f in ("tokens.css", "base.css", "page.css"))
    first = t["problems"][0]
    picks = "".join(f'<label class="pick"><input type="radio" name="problem" value="{p["id"]}"{" checked" if i == 0 else ""}><span>{e(p["title"])}</span></label>'
                    for i, p in enumerate(t["problems"]))
    route = "".join(f'<li class="stop"><i>{CHECK}</i>{e(s["status"])}<span class="stop-note">{e(s["note"].replace("{who}", first["who"].lower()))}</span></li>'
                    for s in t["stops"])
    board = "".join(f'<li class="row{" waiting" if r["id"] == "leak" else ""}" data-row="{r["id"]}"><span class="job">{e(r["job"])}</span>'
                    f'<span class="by">{e(r["by"])}</span><span class="state">{e(r["state"])}</span></li>' for r in t["q_rows"])
    leak = next(r for r in t["q_rows"] if r["id"] == "leak")
    demo = {"problems": t["problems"], "stops": t["stops"], "unsent": t["slip_unsent"], "again": t["again"],
            "wait": t["q_wait"], "done": t["q_done"], "waitState": leak["state"], "doneState": t["q_done_state"],
            "finish": t["q_btn"], "reset": t["q_reset"]}
    json_ld = {"@context": "https://schema.org", "@type": "WebApplication", "name": "Garden City Seva", "url": SITE + t["path"],
               "description": t["meta_description"], "inLanguage": lang, "applicationCategory": "UtilitiesApplication",
               "operatingSystem": "Web browser", "areaServed": {"@type": "Place", "name": "Runwal Garden City, Dombivli East, Thane, Maharashtra, India"}}
    values = {**{k: e(v) for k, v in t.items() if isinstance(v, str)},
              "lang": lang, "site": SITE, "css": css, "logo": logo(),
              "json_ld": json.dumps(json_ld, ensure_ascii=False).replace("</", "<\\/"),
              "demo": json.dumps(demo, ensure_ascii=False).replace("</", "<\\/"),
              "legal": "/legal/" if lang == "en" else f"/{lang}/legal/",
              "langs": "".join(f'<a href="{TEXTS[l]["path"]}" hreflang="{l}" lang="{l}" aria-label="{FULL[l]}"{" aria-current=page" if l == lang else ""}>{NAMES[l]}</a>' for l in TEXTS),
              "picks": picks, "route": route, "board": board,
              "first_action": e(t["stops"][0]["next"]), "first_title": e(first["title"]), "first_where": e(first["where"]),
              "first_cat": e(first["cat"]), "first_who": e(first["who"]),
              "plates": "".join(f"<li><h3>{e(a)}</h3><p>{e(b)}</p></li>" for a, b in t["join"]),
              "faq": "".join(f"<details><summary>{e(q)}</summary><p>{e(a)}</p></details>" for q, a in t["faq"])}
    out = re.sub(r"\{\{(\w+)\}\}", lambda m: values[m.group(1)], page)
    assert "{{" not in out, "a placeholder has no text"
    return out


def shape(value):
    """The structure of a text entry, to prove the three languages match."""
    if isinstance(value, dict):
        return {k: shape(v) for k, v in value.items()}
    if isinstance(value, list):
        return [shape(v) for v in value]
    return "text"


if __name__ == "__main__":
    for lang, t in TEXTS.items():
        assert shape(t) == shape(TEXTS["en"]), f"{lang} texts differ from en"
        assert [p["id"] for p in t["problems"]] == [p["id"] for p in TEXTS["en"]["problems"]], f"{lang} problems differ"
        target = OUT / ("home.html" if lang == "en" else f"{lang}/index.html")
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(render(lang, t), encoding="utf-8")
        print("wrote", target.relative_to(HERE.parent))
    urls = "".join(f"  <url><loc>{SITE}{t['path']}</loc></url>\n" for t in TEXTS.values())
    (OUT / "sitemap.xml").write_text(f'<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n{urls}</urlset>\n', encoding="utf-8")
