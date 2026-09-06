# BuddyCursor

BuddyCursor is the on-screen pointer. After Start, it lives in a system overlay so it remains visible on the home screen and in other apps. See `docs/overlay.md`.

## Composition

| File | Role |
|------|------|
| `ui/cursor/BuddyCursor.kt` | Arrow drawing; layout origin is the tip |
| `ui/cursor/BuddyCursorHandle.kt` | Hold, jiggle, live drag deltas |
| `ui/cursor/BuddyDrag.kt` | Finger-down pickup, or a quick double-tap to ask |
| `ui/cursor/GrabMotion.kt` | Hold bounce + short jiggle |
| `overlay/BuddyCursorController.kt` | Hands API — programmatic move |
| `overlay/CursorLanding.kt` | Named spots for `fly_to` |
| `overlay/` | Window, service, permission, session |

## Current behavior

1. Tap **Start your buddy** → grant appear-on-top if asked → cursor shows at the last (or center) position.
2. Press the cursor → it bounces to show it is held.
3. Drag → the overlay window follows your finger.
4. Lift → it stays there.
5. Double-tap the cursor → the ask panel opens on top of whatever you are looking at.
6. Tap **Watch it move** → the cursor flies a short path (same API AI will call later).
7. Tap **Ask buddy** → same panel as the double-tap.
8. Tap **Stop buddy** (or the notification action) → the overlay is removed.

See `docs/cursor-hands.md`.
