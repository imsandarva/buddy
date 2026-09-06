# BuddyCursor

BuddyCursor is the on-screen pointer. After Start, it lives in a system overlay so it remains visible on the home screen and in other apps. See `docs/overlay.md`.

## Composition

| File | Role |
|------|------|
| `ui/cursor/BuddyCursor.kt` | Arrow drawing; layout origin is the tip |
| `ui/cursor/BuddyCursorHandle.kt` | Hold, jiggle, live drag deltas |
| `ui/cursor/BuddyDrag.kt` | Finger-down pickup |
| `ui/cursor/GrabMotion.kt` | Hold bounce + short jiggle |
| `overlay/BuddyCursorController.kt` | Hands API — programmatic move |
| `overlay/` | Window, service, permission, session |

## Current behavior

1. Tap **Start your buddy** → grant appear-on-top if asked → cursor shows at the last (or center) position.
2. Press the cursor → it bounces to show it is held.
3. Drag → the overlay window follows your finger.
4. Lift → it stays there.
5. Tap **Watch it move** → the cursor flies a short path (same API AI will call later).
6. Tap **Ask buddy** → Gemini speaks and points at a real control (brain → eyes → hands).
7. Tap **Stop buddy** (or the notification action) → the overlay is removed.

See `docs/cursor-hands.md`.
