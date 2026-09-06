# BuddyCursor

BuddyCursor is the on-screen pointer that will later guide taps. It is a session-owned overlay, not part of the home layout, so it can move independently and eventually live in a system overlay.

## Composition

| File | Role |
|------|------|
| `session/BuddyCursorState.kt` | Immutable placement: visibility + normalized x/y |
| `session/BuddySessionViewModel.kt` | Start Buddy; persist rest position after a drag |
| `ui/cursor/BuddyCursor.kt` | Arrow drawing; layout origin is the tip (hotspot) |
| `ui/cursor/BuddyCursorOverlay.kt` | Layer, placement, grab handle |
| `ui/cursor/BuddyDrag.kt` | Finger-down pickup and live drag |
| `ui/cursor/GrabMotion.kt` | Hold bounce + short jiggle |
| `ui/BuddyApp.kt` | Wires Start → session and hosts the cursor layer |

## Current behavior

1. Tap **Start your buddy** → cursor appears at the center.
2. Press the cursor → it bounces/jiggles to show it is held.
3. Drag → the tip follows your finger in real time (local pixels, not ViewModel).
4. Lift → it stays there; rest position is saved as fractions on the session.

Drag state is read only inside `BuddyCursorLayer`, so the home screen does not recompose while you move.

Moving it onto other apps still comes later. Keep driving position from `BuddyCursorState`.
