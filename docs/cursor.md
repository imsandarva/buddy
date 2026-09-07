# BuddyCursor

BuddyCursor is the on-screen pointer. After Start, it lives in a system overlay so it remains visible on the home screen and in other apps. See `docs/overlay.md`.

## Composition

| File | Role |
|------|------|
| `ui/cursor/CursorGeometry.kt` | Silhouette bounds, tip anchor, path — single source of truth |
| `ui/cursor/BuddyCursor.kt` | Renders the full shape inside the geometry box |
| `ui/cursor/BuddyCursorHandle.kt` | Touch target, transforms pivot on the tip |
| `ui/cursor/CursorGestures.kt` | Tap, summon, hold-lift, drag |
| `ui/cursor/CursorMotion.kt` | Breathing idle, levitation, velocity tilt, summon ripples, landings |
| `overlay/BuddyOverlayWindow.kt` | Maps tip pixels ↔ window origin using `CursorGeometry` |
| `overlay/BuddyCursorController.kt` | Flight API — programmatic move |
| `overlay/CursorLanding.kt` | Named spots for `fly_to` |

## Geometry (why the left side used to vanish)

The shape is symmetric around the tip and extends left *and* right. The old code put the tip 10 dp from the canvas edge and clipped the box — only the right flank survived.

`CursorGeometry` fixes that properly:

1. **Extents** — how far the silhouette reaches left, right, up, and down from the tip.
2. **View size** — `extentLeft + extentRight` by `extentTop + extentBottom`, plus touch padding. Nothing is drawn outside the canvas.
3. **Tip anchor** — the tip sits at `(extentLeft + pad, extentTop + pad)` inside the view. The overlay window is positioned so that point lands on screen coordinates; `tipPixels()` always returns the tip, not the window origin.

The pointer matches the classic arrow: vertical left edge, diagonal right edge, chevron notch, solid blue fill, thick dark outline with rounded joins. See `docs/ui.md`.

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
