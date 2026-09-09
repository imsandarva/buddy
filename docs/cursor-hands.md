# Cursor flight

The AI does not move the window. It calls this API. Finger drag uses the same window. Real taps live in `docs/hands.md`.

## API

`BuddyCursorController` is the process-wide string. The overlay service attaches the live window on start and detaches on stop.

| Call | Use |
|------|-----|
| `animateToPixels(x, y, onLanded?)` | Screen pixels, tip lands there |
| `animateToNormalized(x, y)` | `0…1` of the display |
| `animateToGrid999(x, y)` | Gemini-style `0…999` grid |
| `nudgeNormalized(dx, dy)` | Slide from the current spot (`0…1`) |
| `slideToPixels(x, y, ms, onLanded?)` | Straight follow for a drag stroke |
| `tipPixels()` / `screenPixels()` | Where the tip is now |
| `setPassthrough(on)` | Cursor pass-through; hands use `OverlayChrome` for every Buddy window |
| `playDemo()` | Short hello path — **Watch it move** |
| `cancelFlight()` | Grab or a new command wins |
| `isAttached()` | Overlay window is live |

Flight is a quadratic arc (`cursor/CursorArc.kt`) driven by `ValueAnimator`. Grabbing the cursor cancels the flight. Drag follow is linear so the tip stays on the finger stroke. Every frame of motion reports into `overlay/CursorMoodSignals` (traveling + velocity) so the cursor can draw a trailing comet while it moves — see `docs/cursor.md`.

## Wiring now

- `BuddyScreenEyes.pointTo(node)` maps a snapshot rect to `animateToPixels` at the center (`point_to`).
- `fly_to(place)` maps a name through `CursorLanding` to `animateToNormalized`.
- “Move up / down / left / right” is `BuddyMoveIntent` → `nudgeNormalized` (typed) or Live `nudge` / `LiveRouter` (voice). Do not parse `moveBuddyCursor(x,y)` out of spoken text.
- `BuddyHands` flies, then taps, holds, or drags. See `docs/hands.md`.
- `BuddyType` flies to a field, then fills it. See `docs/type.md`.

See `docs/eyes.md`.
