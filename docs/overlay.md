# System overlay

BuddyCursor floats above the phone, including the notification shade. Android will not let a normal “appear on top” window sit above that panel — TalkBack and Voice Access use `TYPE_ACCESSIBILITY_OVERLAY` from the accessibility service (z-order above the shade). When **Buddy Assistant** is on, `CursorSurface` hosts the cursor that way. If Assistant is off, it falls back to `TYPE_APPLICATION_OVERLAY` (under the shade). The cursor is not drawn inside the Buddy activity.

## What survives

| User action | Cursor stays |
|-------------|--------------|
| Home, switch apps, stay on the launcher | Yes |
| Leave Buddy with Back | Yes |
| Swipe Buddy away in Recents | Usually yes (foreground service) |
| **Stop buddy** or the notification action | No — this is the intended off switch |
| Force stop Buddy in system Settings | No — Android kills the process |
| Reboot | No (not wired yet) |

Android will not let a normal app keep a window after a force-stop. Minimize / leave the app is the supported path.

## Composition

| File | Role |
|------|------|
| `overlay/OverlayPermission.kt` | Checks and opens “appear on top” |
| `overlay/BuddyOverlayController.kt` | Start/stop + resume after Settings |
| `overlay/BuddyOverlayService.kt` | Foreground service; owns the window lifetime |
| `overlay/CursorSurface.kt` | Picks accessibility overlay vs appear-on-top |
| `overlay/BuddyOverlayWindow.kt` | Small WindowManager view; drag, double-tap, `animateTo`, or pass-through for a stroke |
| `overlay/AskOverlayWindow.kt` | Full-screen type-to-ask panel |
| `overlay/LiveOverlayWindow.kt` | Compact Gemini Live bar |
| `overlay/BuddyCursorController.kt` | Hands API attached while the service runs |
| `overlay/CursorLanding.kt` | Named spots for `fly_to` |
| `overlay/CursorFlightAnimator.kt` | Arc flight between points |
| `overlay/OverlaySession.kt` | Process-wide active flag and rest position |
| `overlay/OverlayNotification.kt` | Quiet ongoing notification with **Stop buddy** |
| `overlay/OverlayComposeOwner.kt` | Lifecycle for Compose without an Activity |
| `ui/cursor/BuddyCursorHandle.kt` | Grab, jiggle, drag deltas |

Touches outside the cursor pass through (`FLAG_NOT_FOCUSABLE` + `FLAG_NOT_TOUCH_MODAL` + `WRAP_CONTENT`). Double-tap the cursor to talk live — a small bar appears; type-to-ask is a separate overlay. Cursor, live bar, and ask sheet hide from accessibility (`hideFromBuddyEyes`) so they are never “what is on screen.” See `docs/live.md` and `docs/ask.md`.

## User flow

1. Tap **Start your buddy**.
2. If needed, allow **Appear on top** / **Display over other apps**, then return.
3. The cursor appears on every screen; hold and drag as before. Double-tap it to speak or type.
4. Tap **Watch it move** to see a programmed flight.
5. Tap **Let me see your screen**, then **Ask buddy** — or double-tap the cursor from any app.
6. Tap **Stop buddy** in the app or in the notification to remove it.

Taps, holds, and drags go through `BuddyHands` — see `docs/hands.md`.
