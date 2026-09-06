# Home UI

Buddy opens on a single home surface. `MainActivity` only enables edge-to-edge drawing and hosts `BuddyApp`. The cursor is not part of this screen; it is a system overlay.

## Composition

| File | Role |
|------|------|
| `MainActivity.kt` | Window chrome + `setContent { BuddyApp() }` |
| `ui/BuddyApp.kt` | Theme, session, home, overlay + access resume hook |
| `ui/home/HomeScreen.kt` | Assembles backdrop, hero, Start / Stop, and quiet actions |
| `ui/home/AskBuddySheet.kt` | Listening / thinking / type-to-ask (hosted by the overlay) |
| `ui/components/BuddyActionButton.kt` | Honey Start and quiet Stop |
| `session/BuddySessionViewModel.kt` | Facade over hands, eyes, and the Gemini brain |
| `ui/theme/` | Color, type, motion, Material theme |

## Actions

- **Start your buddy** — requests appear-on-top if needed, then starts the overlay service.
- **Stop buddy** — shown while the overlay is running; removes the cursor.
- **Watch it move** — flies the overlay along a short demo path (`BuddyCursorController.playDemo()`).
- **Let me see your screen** — opens Buddy Assistant while the overlay is running.
- **Ask buddy** — same overlay panel as a double-tap on the cursor. Works from the home screen and other apps. See `docs/ask.md`.
- **Point at something** — snapshots the active screen and flies to one real control.

See `docs/overlay.md`, `docs/cursor.md`, `docs/eyes.md`, and `docs/brain.md`.

## Design tokens

- Dusk navy field (`#10141C`), honey light (`#E4B56A`), quiet sage (`#7A9E96`)
- Serif display + sans body, padding-trimmed type
- Motion stays local: ambient pulse, press scale, entrance, grab jiggle
