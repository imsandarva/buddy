# Gemini Live

Voice is Gemini Live. Typed asks stay on the turn-based chat API. Same cursor tools.

Until now we used REST `generateContent` (`gemini-3.5-flash-lite`) plus the accessibility list — not a screenshot. Android STT in, Android TTS out. That path still runs when you type.

Live is a **brain adapter**. Eyes and hands do not change.

## What Live is

A stateful WebSocket (`BidiGenerateContent`) to the Gemini Developer API. You talk; Gemini talks back with native audio. When it wants the buddy to move or tap, it calls the same functions: `point_to`, `fly_to`, `tap`, `hold`, `swipe`, `drag`. There is no `say` tool — the model’s own voice is the speech.

| | Chat REST | Live |
|--|-----------|------|
| Model | `gemini-3.5-flash-lite` | `gemini-2.5-flash-native-audio-preview-12-2025` |
| Transport | HTTP `generateContent` | WebSocket `BidiGenerateContent` |
| Voice in | Android `SpeechRecognizer` | 16 kHz PCM mic stream |
| Voice out | Android TTS | 24 kHz PCM from Gemini |
| Screen | Accessibility catalog in the prompt | Catalog after setup + in each tool result |
| Tools | `say` + cursor tools | Cursor tools only |

Not Computer Use. Not a screenshot stream yet. Not ElevenLabs. Not ChatGPT.

## How to talk

1. Start the buddy, turn on **Buddy Assistant**, allow the microphone once.
2. Double-tap the cursor (or **Ask buddy**). A small bar appears at the bottom — the rest of the screen stays open so a tap can reach an app.
3. Talk. Gemini answers out loud. Ask it to open Calculator, move, tap, hold, or drag.
4. **That’s all** or a second double-tap ends the talk. **Type instead** opens the old sheet (REST).

The full ask sheet covers the screen, so Live never uses it. That was the bug when a spoken tap hit the sheet.

## Composition

| File | Role |
|------|------|
| `brain/live/BuddyLive.kt` | Session facade — start / stop / tools |
| `brain/live/LiveSocket.kt` | OkHttp WebSocket |
| `brain/live/LiveMessages.kt` | Setup, audio, catalog, toolResponse JSON |
| `brain/live/LiveMic.kt` | 16 kHz capture |
| `brain/live/LiveSpeaker.kt` | 24 kHz playback + barge-in flush |
| `brain/live/LiveConfig.kt` | Model, rates, voice (`Aoede`) |
| `brain/GuidanceActor.kt` | Shared executor for REST and Live |
| `overlay/LiveOverlayWindow.kt` | Compact bottom bar |
| `ui/home/LiveBuddyBar.kt` | Live chrome |

API key is still `gemini.api.key` in `local.properties`. The socket uses `?key=` on the Gemini Live URL.

See `docs/brain.md` and `docs/hands.md`.
