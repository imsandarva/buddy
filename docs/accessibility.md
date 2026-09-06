# Accessibility Setup

Buddy appears in **Settings → Accessibility → Installed apps** as **Buddy Assistant** once the service is declared in the manifest.

## Architecture

| File | Role |
|------|------|
| `accessibility/BuddyAccessibilityService.kt` | System service; runs when the user enables access |
| `accessibility/AccessibilityController.kt` | Checks enabled state, opens system settings |
| `res/xml/accessibility_service_config.xml` | Capabilities: read screen content, perform gestures |
| `ui/HomeScreen.kt` | Onboarding UI to guide the user to Settings |
| `MainActivity.kt` | Thin composition layer only |

## User flow

1. Install and open Buddy.
2. Tap **Open Accessibility Settings**.
3. Select **Buddy Assistant** and turn it on.
4. Return to the app — status updates on resume.

## Capabilities enabled

- `canRetrieveWindowContent` — read the on-screen UI tree (required for guidance).
- `canPerformGestures` — tap/swipe on behalf of the user (future feature).
- Window and view events — detect screen changes for step-by-step help.

## Notes

- Accessibility must be granted manually by the user; apps cannot enable it programmatically.
- Rebuild and reinstall after manifest or service changes for the entry to appear in Settings.
