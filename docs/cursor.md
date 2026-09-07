# BuddyCursor

BuddyCursor is the on-screen pointer. After Start, it lives in a system overlay so it remains visible on the home screen and in other apps. See `docs/overlay.md`.

## Composition

| File | Role |
|------|------|
| `ui/cursor/BuddyCursor.kt` | Soft rounded pointer; blue gradient + glow; layout origin is the tip |
| `ui/cursor/BuddyCursorHandle.kt` | Lift, drift, tap bloom, double-tap summon |
| `ui/cursor/CursorGestures.kt` | Tap, summon, hold-lift, drag |
| `ui/cursor/CursorMotion.kt` | Breathing idle, levitation, velocity tilt, summon ripples, landings |
| `overlay/BuddyCursorController.kt` | Flight API — programmatic move |
| `overlay/CursorLanding.kt` | Named spots for `fly_to` |
| `overlay/` | Window, service, permission, session |

The pointer is sky-blue with a soft glow and white rim so it reads on light and dark screens. See `docs/ui.md`.

## Interaction

1. Tap **Start your buddy** → grant appear-on-top if asked → cursor eases in with a gentle entrance.
2. **Single tap** — a soft bloom pulse; the cursor acknowledges without starting talk.
3. **Double-tap** — summon ripples outward, then live talk starts (or the type sheet if the mic is off). See `docs/live.md`.
4. **Press and hold** (~110 ms) — the cursor lifts with a brighter glow, then follows your finger.
5. **Drag** — the pointer drifts with a slight velocity tilt; release lands with a soft spring.
6. Tap **Watch it move** → the cursor flies a short path (same API AI will call later).
7. Tap **Ask buddy** → same as double-tap.
8. Tap **Stop buddy** (or the notification action) → the overlay is removed.

During a live talk, pull notifications to see **I'm with you** with **End** and **Type instead** — no bottom bar on screen.

See `docs/cursor-hands.md`, `docs/hands.md`, and `docs/type.md`.
