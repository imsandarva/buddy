# Accessibility

Buddy appears in **Settings → Accessibility → Installed apps** as **Buddy Assistant**. The user must turn it on; apps cannot grant this themselves.

**Start your buddy** still only starts the overlay. Seeing the screen is a second, quieter step: **Let me see your screen**.

## What works now

- Prompt to open the Buddy Assistant page (details screen on Android 11+, list otherwise)
- On-demand snapshot of visible controls with roles and states (rows, switches ON/OFF, lists, fields, dialogs on top) — see `docs/eyes.md`
- **Point at something** (home or notification) flies the overlay to a real node’s center
- **Ask buddy** hands the request to the agent runner, which sees, decides, and acts step by step — taps, scrolls, types, presses back, opens apps, asks you when it must (see `docs/agent.md`)
- Real finger strokes via `dispatchGesture` (`canPerformGestures`) — see `docs/hands.md`
- The phone's own keys via `performGlobalAction` — Back, Home, Recents, Notifications, Quick settings
- Real typing via `ACTION_SET_TEXT` (accessibility IME on Android 13+) — see `docs/type.md`

## Composition

| File | Role |
|------|------|
| `accessibility/BuddyAccessibilityService.kt` | System service; attaches eyes + hands + type + keys when enabled |
| `accessibility/BuddyHands.kt` | Tap / hold / scroll / swipe / drag API |
| `accessibility/BuddyGlobal.kt` | Back / Home / Recents / Notifications / Quick settings |
| `accessibility/BuddyType.kt` | Type / submit API |
| `accessibility/FieldWriter.kt` | Live field find + set-text / IME / paste |
| `accessibility/GesturePlayer.kt` | `dispatchGesture` + result callback |
| `accessibility/GestureStrokes.kt` | Finger-like `GestureDescription`s — tap, hold, swipe, drag, pan |
| `accessibility/WindowRootPicker.kt` | Front app + windows stacked above it; skip chrome; keyboard flag |
| `accessibility/ScreenTreeWalker.kt` | Rows, roles, states, scrollable lists |
| `accessibility/AccessibilityNodes.kt` | Prefetch a complete tree; do not recycle on API 33+ |
| `accessibility/ScreenReady.kt` | Settled / readable snapshot after an action |
| `overlay/OverlayChrome.kt` | Pass-through for every Buddy overlay during a stroke |
| `accessibility/ScreenSceneTracker.kt` | Follows the user’s screen while Live is on; retries an empty in-app tree |
| `accessibility/AccessibilityController.kt` | Grant check + settings |
| `accessibility/AccessibilitySession.kt` | Bound / awaiting grant |
| `res/xml/accessibility_service_config.xml` | Read tree, interactive windows, view ids |
| `MainActivity.kt` | Thin composition layer only |

See `docs/eyes.md` for the snapshot API.

## User flow

1. Overlay cursor is already on screen.
2. Tap **Let me see your screen**, turn on **Buddy Assistant**, return.
3. **Point at something** — in the app, or from the notification while another app is open.
4. Ask “tap” or “tap Wi‑Fi” — the buddy presses like a finger. Hold, scroll, drag, and type work the same way.
5. Ask for a whole job — “turn off notifications for this app” — and the runner does every step while you watch.

## Capabilities enabled

- `canRetrieveWindowContent` — read the on-screen UI tree.
- `flagRetrieveInteractiveWindows` — read every interactive window, including the launcher under the overlay.
- `flagReportViewIds` — stable ids when the app provides them.
- `canPerformGestures` — tap, hold, scroll, swipe, drag (`BuddyHands`). Global keys need no extra capability (`BuddyGlobal`).
- `flagInputMethodEditor` — Android 13+ parallel IME so type can `commitText` without replacing Gboard.

## Notes

- Rebuild and reinstall after manifest or service changes for the entry to appear in Settings.
- Snapshots stay on demand unless Live is watching — then window, content, and scroll events (debounced) refresh the SCREEN list so the model follows a new app **and** a new page in the same app (app drawer swipe).
- Opening an app used to send `On screen: (nothing readable)` because the first tree after `WINDOW_STATE_CHANGED` is empty and Compose nodes were dropped. Eyes now prefetch, wait, and keep on-screen controls so Gemini can see inside the app.
- The overlay is often the active window after a tap. Eyes skip **all** of our package and still read the launcher or the app underneath.
- Pulling down the shade is a covering System UI window. Eyes keep that panel (Wi‑Fi, tiles, notifications) and skip the slim status bar. The cursor uses `TYPE_ACCESSIBILITY_OVERLAY` while Buddy Assistant is on so it stays above the shade.
- Rebuild and toggle **Buddy Assistant** after service-config changes so `typeWindowsChanged` is delivered.
