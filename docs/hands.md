# Cursor hands (taps)

The AI does not inject touches. It calls this API. Finger-like strokes use `AccessibilityService.dispatchGesture` — the same path Voice Access and Switch Access use.

## API

`BuddyHands` is the process-wide string. The accessibility service attaches the live player on connect and detaches on unbind.

| Call | Use |
|------|-----|
| `tapHere()` / `holdHere()` | Stroke at the current tip |
| `tapAt(x, y)` / `holdAt(x, y)` | Fly there, then tap or long-press |
| `swipeHere(dx, dy)` | Quick slide from the tip (`0…1` of the display) |
| `dragHere(dx, dy)` | Long-press, then slide — rearrange / sliders |
| `swipeTo` / `dragTo` | From the tip to a pixel |
| `swipeFromTo` / `dragFromTo` | Fly to the start, then stroke to the end |
| `isReady()` | Buddy Assistant is on |

A tap is a short dwell. A hold uses `ViewConfiguration.getLongPressTimeout()` plus a little extra. A drag is a continued stroke (`willContinue`) so the finger never lifts.

## Overlay pass-through

The buddy window sits on the tip, so a raw gesture would hit the cursor. Before each stroke the overlay sets `FLAG_NOT_TOUCHABLE` and fades to `0.79` alpha (Android 12+ will drop touches through an opaque system-alert window). The cursor stays visible and presses. Flags restore in `finally`.

The window slides in a straight line with a drag or swipe so the tip stays on the stroke.

## Wiring now

- “Tap” / “hold this” / “swipe left” / “drag right” at the current spot is on-device (`BuddyHandIntent`). No network.
- “Tap Wi‑Fi” / “hold that icon” / “drag this to the trash” goes to Gemini (`tap`, `hold`, `swipe`, `drag`), then `BuddyHands` on **that** snapshot.
- `point_to` still only flies. Use it when they asked *where*, not *do it*.

The ask panel is a full-screen overlay. A stroke while it is still up hits the sheet, not the app (and a scrim tap used to cancel the job). Speak and type both close the panel and wait a beat before hands run.

Needs **Let me see your screen** (Buddy Assistant). `canPerformGestures` is on.

See `docs/brain.md` and `docs/accessibility.md`.
