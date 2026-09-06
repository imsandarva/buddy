# System overlay

BuddyCursor is a **system overlay** (`TYPE_APPLICATION_OVERLAY`), the same pattern used by Assistive Touch and chat-head apps. It is not drawn inside the Buddy activity.

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
| `overlay/BuddyOverlayWindow.kt` | Small WindowManager view; drag or `animateTo` moves it |
| `overlay/BuddyCursorController.kt` | Hands API attached while the service runs |
| `overlay/CursorLanding.kt` | Named spots for `fly_to` |
| `overlay/CursorFlightAnimator.kt` | Arc flight between points |
| `overlay/OverlaySession.kt` | Process-wide active flag and rest position |
| `overlay/OverlayNotification.kt` | Quiet ongoing notification with **Stop buddy** |
| `overlay/OverlayComposeOwner.kt` | Lifecycle for Compose without an Activity |
| `ui/cursor/BuddyCursorHandle.kt` | Grab, jiggle, drag deltas |

Touches outside the cursor pass through (`FLAG_NOT_FOCUSABLE` + `FLAG_NOT_TOUCH_MODAL` + `WRAP_CONTENT`).

## User flow

1. Tap **Start your buddy**.
2. If needed, allow **Appear on top** / **Display over other apps**, then return.
3. The cursor appears on every screen; hold and drag as before.
4. Tap **Watch it move** to see a programmed flight.
5. Tap **Let me see your screen**, then **Ask buddy** — or **Ask buddy** on the notification from another app. Ask the buddy to move to a corner, or ask how to tap something.
6. Tap **Stop buddy** in the app or in the notification to remove it.

Taps-for-you are later.
