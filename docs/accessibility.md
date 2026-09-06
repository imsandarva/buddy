# Accessibility Setup

Buddy appears in **Settings → Accessibility → Installed apps** as **Buddy Assistant** once the service is declared in the manifest.

The home screen is a brand welcome. Accessibility is not prompted there yet; `Start your buddy` is a reserved hook for that flow.

## Architecture

| File | Role |
|------|------|
| `accessibility/BuddyAccessibilityService.kt` | System service; runs when the user enables access |
| `accessibility/AccessibilityController.kt` | Checks enabled state, opens system settings |
| `res/xml/accessibility_service_config.xml` | Capabilities: read screen content, perform gestures |
| `MainActivity.kt` | Thin composition layer only |

## User flow (next)

1. Tap **Start your buddy**.
2. If access is off, open Accessibility settings and turn on **Buddy Assistant**.
3. Return to the app and begin on-screen guidance.

## Capabilities enabled

- `canRetrieveWindowContent` — read the on-screen UI tree (required for guidance).
- `canPerformGestures` — tap/swipe on behalf of the user (future feature).
- Window and view events — detect screen changes for step-by-step help.

## Notes

- Accessibility must be granted manually by the user; apps cannot enable it programmatically.
- Rebuild and reinstall after manifest or service changes for the entry to appear in Settings.
