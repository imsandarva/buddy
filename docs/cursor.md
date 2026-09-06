# BuddyCursor

BuddyCursor is the on-screen pointer that will later guide taps. It is a session-owned overlay, not part of the home layout, so it can move independently and eventually live in a system overlay.

## Composition

| File | Role |
|------|------|
| `session/BuddyCursorState.kt` | Immutable placement: visibility + normalized x/y |
| `session/BuddySessionViewModel.kt` | When Buddy starts; UI only observes |
| `ui/cursor/BuddyCursor.kt` | Arrow drawing; layout origin is the tip (hotspot) |
| `ui/cursor/BuddyCursorOverlay.kt` | Maps state to screen position and appear/disappear motion |
| `ui/BuddyApp.kt` | Wires Start → `startBuddy()` and draws the overlay above home |

## Current behavior

Tap **Start your buddy** → cursor appears at the center of the screen (`0.5, 0.5`) with a short scale-from-tip animation.

Moving it, pointing at real UI, and drawing over other apps come next. Keep driving those from `BuddyCursorState` rather than from the home screen.
