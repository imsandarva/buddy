# BuddyCursor

BuddyCursor is the on-screen pointer. After Start, it lives in a system overlay so it remains visible on the home screen and in other apps. See `docs/overlay.md`.

## Composition

| File | Role |
|------|------|
| `ui/cursor/Cursor.kt` | Bounds, tip hotspot, and the desktop arrow draw |
| `ui/cursor/BuddyCursorHandle.kt` | Touch target — tap, double-tap, hold, drag |
| `ui/cursor/CursorGestures.kt` | Gesture recognizer |
| `overlay/BuddyOverlayWindow.kt` | Maps tip pixels ↔ window origin using `Cursor` |
| `overlay/BuddyCursorController.kt` | Flight API — programmatic move |
| `overlay/CursorLanding.kt` | Named spots for `fly_to` |

## Geometry

The pointer is drawn on Canvas — blue fill (`#4A9EFF`), black outline, rounded joints. The tip sits at the top-left of the 44 dp glyph; `Cursor.tipOffsetPx` maps that tip to overlay coordinates so window placement stays exact. Touch target = 44 dp glyph + 16 dp pad on each side.

## Interaction

1. Tap **Start your buddy** → grant appear-on-top if asked → cursor appears.
2. **Single tap** — a light haptic; the cursor acknowledges without starting talk.
3. **Double-tap** — live talk starts (or the type sheet if the mic is off). See `docs/live.md`.
4. **Press and hold** (~110 ms) — then the cursor follows your finger.
5. **Drag** — the pointer tracks the finger; release leaves it there.
6. Tap **Watch it move** → the cursor flies a short path (same API AI will call later).
7. Tap **Ask buddy** → same as double-tap.
8. Tap **Stop buddy** (or the notification action) → the overlay is removed.

During a live talk, pull notifications to see **I'm with you** with **End** and **Type instead** — no bottom bar on screen.

See `docs/cursor-hands.md`, `docs/hands.md`, and `docs/type.md`.
