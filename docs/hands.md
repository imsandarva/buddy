# Cursor hands (taps, scrolls, keys)

The AI does not inject touches. It calls this API. Finger-like strokes use `AccessibilityService.dispatchGesture` — the same path Voice Access and Switch Access use. The phone's own buttons use `performGlobalAction`, like TalkBack.

## Strokes — `BuddyHands`

`BuddyHands` is the process-wide string. The accessibility service attaches the live player on connect and detaches on unbind.

| Call | Use |
|------|-----|
| `tapHere()` / `holdHere()` | Stroke at the current tip |
| `tapAt(x, y)` / `holdAt(x, y)` | Fly there, then tap or long-press |
| `scrollWithin(bounds?, direction, viewId?)` | Reveal more content in a list. `down` = see what is below. No bounds → the middle of the screen. Prefers the list’s own scroll action (TalkBack); otherwise one finger across about half the container |
| `swipePage(direction)` | Page pull across most of the display in the direction the finger moves — launcher pages, dismiss |
| `swipeHere(dx, dy)` / `dragHere(dx, dy)` | Page swipe, or long-press-then-slide, from the tip |
| `swipeTo` / `dragTo` / `swipeFromTo` / `dragFromTo` | Explicit start and end pixels |
| `isReady()` | Buddy Assistant is on |

A tap is a short dwell. A hold uses `ViewConfiguration.getLongPressTimeout()` plus a little extra. A drag is one finger: press, then slide, with the continued segment starting *after* the press so Android never sees two pointers (two strokes with overlapping time look like a pinch — Gallery and most lists ignore that). A scroll asks the list node to move (`ACTION_SCROLL_DOWN` / `UP` / `LEFT` / `RIGHT`, then forward/back), and only then injects a single-finger pan. The overlay goes pass-through *before* any `dispatchGesture`, and the window does not move until the stroke finishes — moving it mid-gesture cancels the finger on many phones.

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

Every Buddy overlay sits above the app, so a raw gesture would hit us. Before each stroke `OverlayChrome` sets `FLAG_NOT_TOUCHABLE` on the cursor and the ask sheet (Voice Access does the same) and applies the flags on the window thread immediately, then waits a beat so WindowManager has them. Flags restore in `finally`. After a moving stroke completes, the tip slides to the lift point — never during the dispatch, which would cancel it.

## Wiring now

- “Tap” / “hold this” / “swipe left” / “drag right” / “scroll down” at the current spot is on-device (`BuddyHandIntent`). No network.
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
| `accessibility/GestureStrokes.kt` | One-finger `GestureDescription`s — tap, hold, swipe, drag, pan |
| `accessibility/NodeScroller.kt` | TalkBack-style `performAction` scroll on the live list node |
| `accessibility/HandReach.kt` | Page-swipe span, scroll-pan span |
| `accessibility/Direction.kt` | Up / Down / Left / Right |
| `overlay/OverlayChrome.kt` | Pass-through for every Buddy overlay during a stroke |

See `docs/agent.md`, `docs/brain.md`, and `docs/accessibility.md`.
