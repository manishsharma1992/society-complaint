# Logo: the Speaking tower

A residents' tower that is also a speech bubble: the building speaks up. It is the residents' app's own mark,
deliberately unlike the developer's logo (the feed documents the developer's unmet promises, and the app will serve
more societies). The text beside it, "Runwal MyCity / Residents", is the society's name.

## Files

| File | Use |
|---|---|
| `mycity-symbol.svg` | Master symbol, black, 256 grid |
| `mycity-symbol-small.svg` | Small-size cut (four larger windows): below 32 px, favicons |
| `mycity-mono-1b8233.svg` | Symbol in brand green: the default on light backgrounds |
| `mycity-white.svg` | Symbol in white: on brand green or dark backgrounds |
| `mycity-black.svg` | One colour black: fax, stamps, newspaper |
| `mycity-app-icon.svg`, `apple-touch-icon.png`, `icon-192.png`, `icon-512.png`, `maskable-512.png` | White tower on a green tile |
| `favicon.ico`, `favicon.svg`, `favicon-16/32/48.png` | Browser tab (green small cut) |
| `manifest.json`, `head-snippet.html` | Web app install (copied to `frontend/public/` and `src/index.html`) |
| `mycity-horizontal.svg`, `mycity-horizontal-white.svg` | Symbol + wordmark. **Live text**: convert to outlines before print |
| `presentation.html`, `board-spec.json` | Presentation board with mockups |

In the app the mark is the `app-logo-mark` component (`src/app/shared/logo-mark.ts`), drawn in `currentColor`
next to the HTML wordmark in Tiro Devanagari Marathi / Mukta, so it translates with the rest of the page.

## Colour

| | HEX | RGB | Use |
|---|---|---|---|
| Brand green | `#1B8233` | 27 130 51 | Symbol on light, app-icon tile |
| Ink | `#1F1B16` | 31 27 22 | Wordmark "Runwal MyCity" |
| Ink muted | `#5E574D` | 94 87 77 | Wordmark "Residents" |
| White | `#FFFFFF` | 255 255 255 | Symbol on green or dark |

Green on white is 4.9 : 1 (passes WCAG AA). The windows are holes: whatever is behind shows through.

## Clear space and sizes

- **Clear space:** one window width (24 units on the 256 grid, about 1/5 of the tower's width) on every side.
- **Minimum size:** 24 px tall for `mycity-symbol.svg`; below that use `mycity-symbol-small.svg` (works down to 16 px).
- **Header lockup:** the symbol is as tall as the two wordmark lines together (38 px in the app header).

## Do not

- Recolour the windows, fill them in, or add more of them.
- Put the green symbol on a busy photo or on mid-tone backgrounds: use the white version on a solid green tile.
- Stretch, rotate, outline, add shadows or gradients.
- Move the bubble tail to the other side or let it touch the tower's left edge (it then reads as a bookmark).
- Combine it with the developer's (Runwal's) logo as if it were theirs.

## Open items

- The wordmark in the lockup files is live text (Tiro Devanagari Marathi, Mukta; both Open Font License).
  Outline it before sending to a printer.
- No trademark search was done: have the mark checked before printing it widely.
