# Home UI

Buddy opens on a single home surface. `MainActivity` only enables edge-to-edge drawing and hosts `BuddyApp`. All visual and interaction work lives in focused Compose modules.

## Composition

| File | Role |
|------|------|
| `MainActivity.kt` | Window chrome + `setContent { BuddyApp(...) }` |
| `ui/BuddyApp.kt` | Theme, session, home, and BuddyCursor overlay |
| `ui/home/HomeScreen.kt` | Assembles backdrop, hero, and CTA |
| `ui/cursor/` | Pointer drawing and overlay placement |
| `session/` | Buddy session state (cursor visibility and position) |
| `ui/components/AmbientBackdrop.kt` | Slow-breathing light field |
| `ui/components/BuddyMark.kt` | Cursor-like point of light |
| `ui/components/StartBuddyButton.kt` | Primary action |
| `ui/theme/` | Color, type, motion, Material theme |
| `ui/motion/FadeSlideIn.kt` | Staged entrance |

## Start action

Tap **Start your buddy** to show BuddyCursor at the center of the screen. Session logic lives in `BuddySessionViewModel`; see `docs/cursor.md`.

## Design tokens

- Dusk navy field (`#10141C`), honey light (`#E4B56A`), quiet sage (`#7A9E96`) for secondary type and atmospheric glow
- Serif display + sans body, padding-trimmed type
- Motion stays local: ambient pulse in the canvas, press state in the button, entrance in `FadeSlideIn`
