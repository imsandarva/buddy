# Cursor hands (taps, scrolls, keys)

The AI does not inject touches. It calls this API. Finger-like strokes use `AccessibilityService.dispatchGesture` — the same path Voice Access and Switch Access use. The phone's own buttons use `performGlobalAction`, like TalkBack.

## Strokes — `BuddyHands`

`BuddyHands` is the process-wide string. The accessibility service attaches the live player on connect and detaches on unbind.

| Call | Use |
|------|-----|
| `tapHere()` / `holdHere()` | Stroke at the current tip |
| `tapAt(x, y)` / `holdAt(x, y)` | Fly there, then tap or long-press |
| `scrollWithin(bounds?, direction)` | Reveal more content in a list: a measured pan across the middle 40% of the container, resting before lift so nothing flings past. `down` = see what is below. No bounds → the middle of the screen |
| `swipePage(direction)` | Page pull across most of the display in the direction the finger moves — launcher pages, dismiss |
| `swipeHere(dx, dy)` / `dragHere(dx, dy)` | Page swipe, or long-press-then-slide, from the tip |
| `swipeTo` / `dragTo` / `swipeFromTo` / `dragFromTo` | Explicit start and end pixels |
| `isReady()` | Buddy Assistant is on |

A tap is a short dwell. A hold uses `ViewConfiguration.getLongPressTimeout()` plus a little extra. A drag is a continued stroke (`willContinue`) so the finger never lifts. A scroll is a slower stroke plus a rest (`GestureStrokes.pan`) — a fling would skip the very row the model wanted to see.

## Keys — `BuddyGlobal`

| Key | Android |
|-----|---------|
| `Back` | `GLOBAL_ACTION_BACK` — close a menu, dialog, keyboard; previous screen |
| `Home` | `GLOBAL_ACTION_HOME` |
| `Recents` | `GLOBAL_ACTION_RECENTS` |
| `Notifications` | `GLOBAL_ACTION_NOTIFICATIONS` — pull the shade |
| `QuickSettings` | `GLOBAL_ACTION_QUICK_SETTINGS` |

No gesture, no overlay pass-through, no cursor flight.

## Overlay pass-through

Every Buddy overlay sits above the app, so a raw gesture would hit us. Before each stroke `OverlayChrome` sets `FLAG_NOT_TOUCHABLE` on the cursor and the ask sheet (Voice Access does the same). Flags restore in `finally`. The window slides in a straight line with a drag, swipe, or scroll so the tip stays on the stroke.

## Wiring now

- “Tap” / “hold this” / “swipe left” / “drag right” at the current spot is on-device (`BuddyHandIntent`). No network.
- The agent's `tap`, `long_press`, `scroll`, `swipe`, `drag`, `back`, `home`, … (`AgentAction`) run through `AgentExecutor` on the snapshot the model saw. Live's one-shot tools take the same path.
- `point` still only flies. Use it when they asked *where*, not *do it*.

Needs **Let me see your screen** (Buddy Assistant). `canPerformGestures` is on.

Typing is a separate API — see `docs/type.md`.

## Composition

| File | Role |
|------|------|
| `accessibility/BuddyHands.kt` | Tap / hold / scroll / swipe / drag API |
| `accessibility/BuddyGlobal.kt` | Back / Home / Recents / Notifications / Quick settings |
| `accessibility/GesturePlayer.kt` | `dispatchGesture` + result callback |
| `accessibility/GestureStrokes.kt` | Finger-like `GestureDescription`s — tap, hold, swipe, drag, pan |
| `accessibility/HandReach.kt` | Page-swipe span, scroll-pan span |
| `accessibility/Direction.kt` | Up / Down / Left / Right |
| `overlay/OverlayChrome.kt` | Pass-through for every Buddy overlay during a stroke |

See `docs/agent.md`, `docs/brain.md`, and `docs/accessibility.md`.
