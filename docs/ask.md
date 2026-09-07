# Ask from anywhere

The ask panel lives in the overlay service, not in the Buddy activity. That is the chat-head pattern: invoke it wherever the cursor is.

## How to ask

1. Double-tap the buddy cursor — on the home screen, in Settings, in another app.
2. With the microphone on, that starts a **live talk** — audio in and audio out, no captions. Pull notifications to see **I’m with you** with **End** and **Type instead**; the screen stays fully tappable. If live cannot start, the type sheet opens instead — see `docs/live.md`.
3. Or choose **Type instead** / type in the sheet — that still uses the one-shot chat API.
4. Buddy flies, points, speaks, taps / holds / drags, or types.

**Ask buddy** in the app and **Ask buddy** on the notification use this same panel.

## Why a second window

The cursor window is small and not focusable, so other apps keep getting touches. The ask panel is a full-screen overlay that can take the keyboard. The cursor is raised above it so you can still see the buddy.

## Composition

| File | Role |
|------|------|
| `ui/cursor/CursorGestures.kt` | Tap bloom, double-tap summon, hold-lift, drag |
| `overlay/AskOverlayWindow.kt` | Focusable overlay that hosts `AskBuddySheet` |
| `overlay/BuddyOverlayService.kt` | Owns cursor + ask; watches `BrainSession`; live controls in notification |
| `overlay/OverlayNotification.kt` | Live talk **End** / **Type instead** when talking |
| `brain/BuddyBrain.kt` | Voice → Live; type → REST |
| `ui/home/AskBuddySheet.kt` | Speak / type panel |

Microphone: allow it once in Buddy. After that, double-tap can listen over other apps (the service takes the microphone type only while the panel is open). If the mic is not allowed yet, you can still type.

See `docs/overlay.md` and `docs/brain.md`.
