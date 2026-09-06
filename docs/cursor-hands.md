# Cursor hands

The AI does not move the window. It calls this API. Finger drag uses the same window.

## API

`BuddyCursorController` is the process-wide string. The overlay service attaches the live window on start and detaches on stop.

| Call | Use |
|------|-----|
| `animateToPixels(x, y)` | Screen pixels, tip lands there |
| `animateToNormalized(x, y)` | `0…1` of the display |
| `animateToGrid999(x, y)` | Gemini-style `0…999` grid |
| `nudgeNormalized(dx, dy)` | Slide from the current spot (`0…1`) |
| `playDemo()` | Short hello path — **Watch it move** |
| `cancelFlight()` | Grab or a new command wins |
| `isAttached()` | Overlay window is live |

Flight is a quadratic arc (`cursor/CursorArc.kt`) driven by `ValueAnimator`. Grabbing the cursor cancels the flight.

## Wiring now

- `BuddyScreenEyes.pointTo(node)` maps a snapshot rect to `animateToPixels` at the center (`point_to`).
- `fly_to(place)` maps a name through `CursorLanding` to `animateToNormalized`.
- “Move up / down / left / right” is `BuddyMoveIntent` → `nudgeNormalized`. Do not parse `moveBuddyCursor(x,y)` out of spoken text.

See `docs/eyes.md`.
