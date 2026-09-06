# Cursor hands (taps)

The AI does not inject touches. It calls this API. Finger-like strokes use `AccessibilityService.dispatchGesture` — the same path Voice Access and Switch Access use.

## API

`BuddyHands` is the process-wide string. The accessibility service attaches the live player on connect and detaches on unbind.

| Call | Use |
|------|-----|
| `tapHere()` / `holdHere()` | Stroke at the current tip |
| `tapAt(x, y)` / `holdAt(x, y)` | Fly there, then tap or long-press |
| `swipeHere(dx, dy)` | Page swipe across most of the display (launcher pages, not a short flick) |
| `dragHere(dx, dy)` | Long-press, then slide — rearrange / sliders |
| `swipeTo` / `dragTo` | From the tip to a pixel |
| `swipeFromTo` / `dragFromTo` | Fly to the start, then stroke to the end |
| `isReady()` | Buddy Assistant is on |

A tap is a short dwell. A hold uses `ViewConfiguration.getLongPressTimeout()` plus a little extra. A drag is a continued stroke (`willContinue`) so the finger never lifts.

## Overlay pass-through

Every Buddy overlay sits above the app, so a raw gesture would hit us. Before each stroke `OverlayChrome` sets `FLAG_NOT_TOUCHABLE` on the cursor, the live pill, and the ask sheet (Voice Access does the same). A tap that used to land on “I’m with you” now reaches the button underneath. Flags restore in `finally`.

A directional swipe is a **page pull** — about 76% of the screen, ~460 ms — so an app-drawer page actually turns. A short flick from the tip cannot do that.

The window slides in a straight line with a drag or swipe so the tip stays on the stroke.

## Wiring now

- “Tap” / “hold this” / “swipe left” / “drag right” at the current spot is on-device (`BuddyHandIntent`). No network.
- “Tap Wi‑Fi” / “hold that icon” / “drag this to the trash” goes to Gemini (`tap`, `hold`, `swipe`, `drag`), then `BuddyHands` on **that** snapshot.
- `point_to` still only flies. Use it when they asked *where*, not *do it*.

The ask panel is a full-screen overlay. A stroke while it is still up hits the sheet, not the app (and a scrim tap used to cancel the job). Speak and type both close the panel and wait a beat before hands run.

Needs **Let me see your screen** (Buddy Assistant). `canPerformGestures` is on.

Typing is a separate API — see `docs/type.md`.

See `docs/brain.md` and `docs/accessibility.md`.
