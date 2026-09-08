# Home UI

Buddy opens on a single home surface. `MainActivity` only enables edge-to-edge drawing and hosts `BuddyApp`. The cursor is not part of this screen; it is a system overlay.

## Composition

| File | Role |
|------|------|
| `MainActivity.kt` | Light system bars + `setContent { BuddyApp() }` |
| `ui/BuddyApp.kt` | Theme, session, home, overlay + access resume hook |
| `ui/home/HomeScreen.kt` | Assembles paper backdrop, hero, Start / Rest, and quiet actions |
| `ui/home/AskBuddySheet.kt` | Type-to-ask panel (typed REST path) |
| `ui/components/BuddyActionButton.kt` | Violet Start and quiet Rest |
| `ui/motion/PressScale.kt` | Shared press spring |
| `session/BuddySessionViewModel.kt` | Facade over hands, eyes, and the Gemini brain |
| `ui/theme/` | Color, type, motion, Material theme — light only |

## Actions

- **Start your buddy** — requests appear-on-top if needed, then starts the overlay service.
- **Stop buddy** — shown while the overlay is running; removes the cursor.
- **Watch it move** — flies the overlay along a short demo path (`BuddyCursorController.playDemo()`).
- **Let me see your screen** — opens Buddy Assistant while the overlay is running.
- **Ask buddy** — starts a live talk when the microphone is allowed. See `docs/live.md`.
- **Point at something** — snapshots the active screen and flies to one real control.

See `docs/overlay.md`, `docs/cursor.md`, `docs/eyes.md`, `docs/hands.md`, `docs/type.md`, and `docs/brain.md`.

## Design tokens

Light only. Paper field (`#F7F6FB`), snow surfaces (`#FFFFFF`), ink (`#1C1730`), iris violet (`#635BFF`) for UI accents. The on-screen pointer uses `assets/buddycursor_icon.png`. No honey, no dusk, no dark theme.

- Eyebrow: wide-tracked sans, violet
- Display: light serif, stacked two-line headlines
- Body: 17 / 27 sans, muted ink
- Motion: local only — ambient bloom, press scale, entrance, grab jiggle, hero crossfade
