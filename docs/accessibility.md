# Accessibility

Buddy appears in **Settings → Accessibility → Installed apps** as **Buddy Assistant**. The user must turn it on; apps cannot grant this themselves.

**Start your buddy** still only starts the overlay. Seeing the screen is a second, quieter step: **Let me see your screen**.

## What works now

- Prompt to open the Buddy Assistant page (details screen on Android 11+, list otherwise)
- On-demand snapshot of visible controls
- **Point at something** (home or notification) flies the overlay to a real node’s center
- **Ask buddy** sends that snapshot to Gemini, then speaks, points, flies, or taps / holds / drags (see `docs/brain.md`)
- Real finger strokes via `dispatchGesture` (`canPerformGestures`) — see `docs/hands.md`

## Composition

| File | Role |
|------|------|
| `accessibility/BuddyAccessibilityService.kt` | System service; attaches eyes + hands when enabled |
| `accessibility/BuddyHands.kt` | Tap / hold / swipe / drag API |
| `accessibility/GesturePlayer.kt` | `dispatchGesture` + result callback |
| `accessibility/GestureStrokes.kt` | Finger-like `GestureDescription`s |
| `accessibility/WindowRootPicker.kt` | Which windows to walk (skip our overlay) |
| `accessibility/AccessibilityController.kt` | Grant check + settings |
| `accessibility/AccessibilitySession.kt` | Bound / awaiting grant |
| `res/xml/accessibility_service_config.xml` | Read tree, interactive windows, view ids |
| `MainActivity.kt` | Thin composition layer only |

See `docs/eyes.md` for the snapshot API.

## User flow

1. Overlay cursor is already on screen.
2. Tap **Let me see your screen**, turn on **Buddy Assistant**, return.
3. **Point at something** — in the app, or from the notification while another app is open.
4. Ask “tap” or “tap Wi‑Fi” — the buddy presses like a finger. Hold and drag work the same way.

## Capabilities enabled

- `canRetrieveWindowContent` — read the on-screen UI tree.
- `flagRetrieveInteractiveWindows` — read every interactive window, including the launcher under the overlay.
- `flagReportViewIds` — stable ids when the app provides them.
- `canPerformGestures` — tap, hold, swipe, drag (`BuddyHands`).

## Notes

- Rebuild and reinstall after manifest or service changes for the entry to appear in Settings.
- Snapshots run only when asked. Window events are ignored so the service stays cheap.
- The overlay is often the active window after a tap. Eyes skip our chrome and still read the launcher or the app underneath.
