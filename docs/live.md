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

## Why live used to die right after the socket opened

Gemini Live answers on **binary WebSocket frames that still hold JSON** (`{"setupComplete":{}}`, audio, tool calls). OkHttp only delivers those to `onMessage(WebSocket, ByteString)`. A text-only listener never sees `setupComplete`, the 12s safety timer fires (`Live.stop user=false`), and the type sheet opens. Industry clients (Google’s JS GenAI SDK, Elixir `gemini_ex`) decode both text and binary as UTF-8 JSON. `LiveSocket` now does the same, and a server `error` object fails the session immediately instead of hanging.

Look for `Live.socket setupComplete` then `Live.mic start` in logcat (`Buddy===TRACE`). If live cannot start, the type sheet still opens so they can ask another way.

## Why speech scratched and sped up

Gemini generates PCM **faster than realtime**. The official Gemini app buffers that stream and plays it at 24 kHz wall-clock speed. We used to do three things that the official player never does:

1. **Drop audio** when a 32-chunk queue filled — later words vanished, so a sentence sounded rushed / “2×”.
2. **Start the track empty** with a tiny hardware buffer — underruns sound like old-TV static.
3. **Parse huge JSON on the WebSocket reader** — the next audio frame waited, then arrived in a burst.

Playback is now a jitter buffer: preroll ~120 ms, never drop, blocking `AudioTrack` writes, decode on a worker. Barge-in flush happens only on the speaker thread.

Buddy’s own voice used to leak into the mic (USAGE_ASSISTANT vs VOICE_COMMUNICATION). The server then sent `interrupted` and we flushed mid-sentence. Mic and speaker now share one audio session with echo cancel.

Setup JSON must stay on fields this `v1beta` socket actually knows. Extra keys such as `proactivity` get `1007 Invalid JSON payload` and the talk dies before you can speak.

## Why the cursor felt hung

Live tools ran on the main thread: a full accessibility walk, then fly + tap (up to ~2.4 s), then another walk. The overlay could not move or take taps. Tools now run on a background dispatcher; window moves stay on main. A live SCREEN list is capped so the model can call the next tool sooner.

The API itself is live — long pauses after “tap Wi‑Fi” are usually us waiting on the tree or the player, not Gemini being “slow”.

## Composition

| File | Role |
|------|------|
| `brain/live/BuddyLive.kt` | Session facade — start / stop / tools |
| `brain/live/LiveSocket.kt` | OkHttp WebSocket (text + binary JSON, decode off the reader) |
| `brain/live/LiveMessages.kt` | Setup, audio, catalog, toolResponse JSON |
| `brain/live/LiveAudio.kt` | Shared session + AEC / NS / AGC |
| `brain/live/LiveMic.kt` | 16 kHz capture + send queue |
| `brain/live/LiveSpeaker.kt` | 24 kHz jitter-buffered playback |
| `brain/live/LiveConfig.kt` | Model, rates, voice (`Aoede`) |
| `brain/GuidanceActor.kt` | Shared executor for REST and Live |
| `overlay/LiveOverlayWindow.kt` | Compact bottom bar |
| `ui/home/LiveBuddyBar.kt` | Live chrome |

API key is still `gemini.api.key` in `local.properties`. The socket uses `?key=` on the Gemini Live URL.

See `docs/brain.md` and `docs/hands.md`.
