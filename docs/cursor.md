# BuddyCursor

BuddyCursor is the on-screen pointer. After Start, it lives in a system overlay so it remains visible on the home screen and in other apps. See `docs/overlay.md`.

## Composition

| File | Role |
|------|------|
| `ui/cursor/CursorGeometry.kt` | Asset bounds, tip anchor — single source of truth |
| `ui/cursor/CursorAsset.kt` | Loads `assets/buddycursor_icon.png` once |
| `ui/cursor/BuddyCursor.kt` | Renders the PNG + summon ripples |
| `ui/cursor/BuddyCursorHandle.kt` | Touch target, transforms pivot on the tip |
| `ui/cursor/CursorGestures.kt` | Tap, summon, hold-lift, drag |
| `ui/cursor/CursorMotion.kt` | Breathing idle, levitation, velocity tilt, summon ripples, landings |
| `overlay/BuddyOverlayWindow.kt` | Maps tip pixels ↔ window origin using `CursorGeometry` |
| `overlay/BuddyCursorController.kt` | Flight API — programmatic move |
| `overlay/CursorLanding.kt` | Named spots for `fly_to` |

## Geometry

The pointer is `assets/buddycursor_icon.png` (500×500, tip at pixel 155,50). `CursorGeometry` maps that tip to overlay coordinates so window placement and transforms pivot stay exact. Touch target = 56 dp icon + 8 dp pad on each side.

## Interaction

1. Tap **Start your buddy** → grant appear-on-top if asked → cursor eases in with a gentle entrance.
2. **Single tap** — a soft bloom pulse; the cursor acknowledges without starting talk.
3. **Double-tap** — summon ripples outward, then live talk starts (or the type sheet if the mic is off). See `docs/live.md`.
4. **Press and hold** (~110 ms) — the cursor lifts, then follows your finger.
5. **Drag** — the pointer drifts with a slight velocity tilt; release lands with a soft spring.
6. Tap **Watch it move** → the cursor flies a short path (same API AI will call later).
7. Tap **Ask buddy** → same as double-tap.
8. Tap **Stop buddy** (or the notification action) → the overlay is removed.

During a live talk, pull notifications to see **I'm with you** with **End** and **Type instead** — no bottom bar on screen.

See `docs/cursor-hands.md`, `docs/hands.md`, and `docs/type.md`.
