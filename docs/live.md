# Gemini Live

Voice is Gemini Live. Typed asks stay on the turn-based chat API. Same cursor tools.

Typed asks go to the agent runner (`docs/agent.md`) over REST `generateContent` — Android STT in, Android TTS out. Voice is Live.

Live is a **brain adapter**. Eyes and hands do not change.

## What Live is

A stateful WebSocket (`BidiGenerateContent`) to the Gemini Developer API. **Mic PCM goes straight to Gemini; Gemini PCM comes straight back.** There is no Android speech-to-text step, no chat REST hop, and no captions. Talk is the default. For one quick thing they asked for on the screen it calls a tool — `tap`, `hold`, `scroll`, `swipe`, `drag`, `type`, `back`, `home`, `open_app`, `point_to`, `fly_to`, `nudge` — which `LiveTools` maps onto the shared `AgentAction` space and `AgentExecutor` performs. A real multi-step job is `run_goal` after `LiveRouter` agrees — Live **acks and stays on the same socket**, the runner works the screen in silence, and Live tells them when `JOB DONE` arrives. Greetings, “what do you see”, and moving the buddy stay in the talk. There is no `say` tool — the model’s own voice is the speech. See `docs/live-jobs.md`.

| | Agent runner (typed) | Live |
|--|-----------|------|
| Model | `gemini-3.5-flash-lite`, thinking `high` | `gemini-3.1-flash-live-preview` |
| Transport | HTTP `generateContent`, structured JSON | WebSocket `BidiGenerateContent` |
| Voice in | Android `SpeechRecognizer` | 16 kHz PCM mic stream (no STT) |
| Voice out | Android TTS | 24 kHz PCM from Gemini (no captions) |
| Screen | SCREEN block in each step message | SCREEN block as `realtimeInput` text after mic is open |
| Tools | Full action space, many steps; silent when Live is talking | One-shot tools; `run_goal` on the same session (`docs/live-jobs.md`) |

Not Computer Use. Not video Live. Not ElevenLabs. Not ChatGPT.

## Audio only — not screen-share video

Gemini Live **can** take video, but it is not a native camera or screen-share codec. The API wants JPEG/PNG frames on `realtimeInput.video` at **at most 1 frame per second**. That is still a screenshot stream, billed as video tokens, and it makes the turn heavier — it does not make replies faster.

We do **not** send those frames. Eyes stay the accessibility SCREEN list (labels and ids, no pixels). Typed ask is the same. If we ever add Live video, it would be a MediaProjection JPEG at ≤1 fps on the same socket, next to the mic — not a separate “video Live” product.

## It must see what the user sees

Live status lives in the **ongoing notification** on a dedicated `buddy_live` channel (“I’m with you”, **End**, **Type instead**) — the screen stays fully tappable with no bottom chrome. `OverlayNotifier.sync()` pushes the live notification whenever talk starts or stops, and `presentCursor()` no longer clobbers it. During a Buddy tap, only the cursor and ask sheet go pass-through via `OverlayChrome`.

Two things used to make the model describe the Buddy app while you were on the home screen:

1. **A frozen first look.** SCREEN was sent once at `setupComplete`. If talk started in Buddy, then you pressed Home, the model still had the Buddy buttons. Industry voice agents (TalkBack-style window follow, Gemini Live `realtimeInput` text) push a new scene when the foreground app changes.
2. **Our chrome in the tree.** Eyes skip overlay chrome (cursor, ask sheet) and still read the **Buddy activity** when it is in front. Skipping the whole package made opening Buddy look like the previous app drawer. Scene follow also listens to our package’s window events, so the SCREEN list updates when they come home to Buddy.

After you install this, toggle **Buddy Assistant** off and on once so the new window events are registered.

The live bar is gone — live talk shows in the notification shade. Eyes read the shade when it covers the screen, and the cursor stays above it (accessibility overlay) so it does not slip behind the panel.

## Why “I talked, then waited forever”

The official Gemini app feels instant because the **server** decides the end of your sentence (VAD) and starts speaking. We were adding seconds on our side:

1. **Wrong model + thinking.** `gemini-2.5-flash-native-audio` thinks dynamically by default. Live now uses `gemini-3.1-flash-live-preview` with `thinkingLevel: minimal` — Google’s low-latency Live model.
2. **Open client turn.** The SCREEN list used to go as `clientContent` with `turnComplete: false`. Mixing that with mic audio makes the server wait on a turn that never closes. SCREEN now goes as `realtimeInput` text — same path as audio, no held turn. Industry clients (Google’s GenAI SDK) use `send_realtime_input` for mid-talk context; `clientContent` is for seeding history only.
3. **Slow VAD.** Default silence before “they finished talking” is long. Setup now sets `endOfSpeechSensitivity: HIGH` and `silenceDurationMs: 220` so a breath is ok, but we do not sit on a full extra second. Google’s own example uses 100 ms; 220 ms is kinder for people who pause while they find the words.
4. **Late first word.** Mic chunks are 20 ms (Live best practice is 20–40 ms). Speaker preroll is ~60 ms and the track buffer ~160 ms, so the first syllable is not sitting in a 400 ms queue.

We still do **not** request transcripts. Those arrive late and make the bar look idle.

Long pauses after “tap Wi‑Fi” can still be a tree walk or a tool, not the voice path. See “Why the cursor felt hung”.

## How to talk

1. Start the buddy, turn on **Buddy Assistant**, allow the microphone once.
2. Double-tap the cursor (or **Ask buddy**). Buddy says a short hello and waits. It must not tap or move until you ask.
3. Talk. You will not see your words or Buddy’s words as text — you only hear each other. Say hello, ask what it sees, or ask it to move — it stays with you. Ask it to open Calculator, tap, hold, scroll, drag, or type. Ask for a real job — “how much storage do I have left?” — Live stays with you while the runner works the screen, then Live tells you when it’s done.

## Not speech-to-text, then chat

Live is **audio in, audio out** on one socket — the same shape as the official Gemini app. We used to also ask Gemini for transcripts and paint them on the bar. That extra job is billed and delivered late, so it *looked* like we waited for STT before thinking. We do not request `inputAudioTranscription` / `outputAudioTranscription`. The mic opens after a short hello (not on `setupComplete`). SCREEN goes as context once the ears are open — it is not an order. Greeting-turn tools stay blocked. After that, tools run when the server has treated the turn as theirs: hangover mic energy (AEC on `VOICE_COMMUNICATION` punches holes in a consecutive-loud streak), model audio after a short hello-grace, barge-in, or a tool that already holds their words (`run_goal`, `open_app`, `type`). A raw energy gate alone used to block every `run_goal` after “On it.” `run_goal` still goes through `LiveRouter` — speaking is not a job. See `docs/live-routing.md`.
4. **End** in the notification, or a second double-tap, ends the talk. **Type instead** opens the old sheet (REST).

The full ask sheet covers the screen, so Live never uses it. That was the bug when a spoken tap hit the sheet.

## Why live used to die right after the socket opened

Gemini Live answers on **binary WebSocket frames that still hold JSON** (`{"setupComplete":{}}`, audio, tool calls). OkHttp only delivers those to `onMessage(WebSocket, ByteString)`. A text-only listener never sees `setupComplete`, the 12s safety timer fires (`Live.stop user=false`), and the type sheet opens. Industry clients (Google’s JS GenAI SDK, Elixir `gemini_ex`) decode both text and binary as UTF-8 JSON. `LiveSocket` now does the same, and a server `error` object fails the session immediately instead of hanging.

Look for `Live.socket setupComplete` then `Live.mic start` in logcat (`Buddy===TRACE`). If live cannot start, the type sheet still opens so they can ask another way.

## Why speech scratched and sped up

Gemini generates PCM **faster than realtime**. The official Gemini app buffers that stream and plays it at 24 kHz wall-clock speed. We used to do three things that the official player never does:

1. **Drop audio** when a 32-chunk queue filled — later words vanished, so a sentence sounded rushed / “2×”.
2. **Start the track empty** with a tiny hardware buffer — underruns sound like old-TV static.
3. **Parse huge JSON on the WebSocket reader** — the next audio frame waited, then arrived in a burst.

Playback is now a jitter buffer: preroll ~60 ms, never drop, blocking `AudioTrack` writes, decode on a worker. Barge-in flush happens only on the speaker thread.

Buddy’s own voice used to leak into the mic (USAGE_ASSISTANT vs VOICE_COMMUNICATION). The server then sent `interrupted` and we flushed mid-sentence. Mic and speaker now share one audio session with echo cancel.

Setup JSON must stay on fields this `v1beta` socket actually knows. Extra keys such as `proactivity` get `1007 Invalid JSON payload` and the talk dies before you can speak.

## Why the cursor felt hung

Live tools ran on the main thread: a full accessibility walk, then fly + tap (up to ~2.4 s), then another walk. The overlay could not move or take taps. Tools now run on a background dispatcher; window moves stay on main. A live SCREEN list is capped (64 lines) so the model can call the next tool sooner.

A spoken tap that then sits still is usually the tree or the tool, not VAD. Voice gap after they stop talking should now be a short silence (about 220 ms) plus model start, not a multi-second think.

After a tool, the tool response carries the new SCREEN taken after the event stream goes quiet (`awaitSettledSnapshot`) and retried while a new app's tree is still hollow — so the model sees the new app, not “(nothing readable)”.

## Composition

| File | Role |
|------|------|
| `brain/live/BuddyLive.kt` | Session facade — start / stop / tools / follow screen / jobs on the same socket |
| `brain/live/LiveListen.kt` | Greeting vs asked — when a tool may run |
| `brain/live/LiveRouter.kt` | Talk / Act / Goal — does not trust `run_goal` blindly |
| `brain/live/LiveDesk.kt` | JOB ASK / JOB DONE back into this talk |
| `brain/live/LiveSpeech.kt` | Hangover energy on mic PCM (AEC-safe) |
| `brain/live/LiveTools.kt` | Function declarations; call → `AgentAction` / `RunGoal` / `AnswerJob` |
| `brain/live/LivePrompt.kt` | The talk contract — talk first, same session for jobs |
| `accessibility/ScreenSceneTracker.kt` | Debounced window follow while Live is on |
| `accessibility/ScreenReady.kt` | Settled / readable snapshot after a tool |
| `brain/live/LiveSocket.kt` | OkHttp WebSocket (text + binary JSON, decode off the reader) |
| `brain/live/LiveMessages.kt` | Setup, audio, catalog, toolResponse JSON |
| `brain/live/LiveAudio.kt` | Shared session + AEC / NS / AGC |
| `brain/live/LiveMic.kt` | 16 kHz capture + send queue |
| `brain/live/LiveSpeaker.kt` | 24 kHz jitter-buffered playback |
| `brain/live/LiveConfig.kt` | Model, rates, voice (`Aoede`) |
| `brain/agent/AgentExecutor.kt` | Shared executor for the runner and Live |
| `overlay/OverlayNotification.kt` | Live talk controls in the notification shade (`buddy_live` channel) |
| `overlay/OverlayNotifier.kt` | Syncs notification when live starts or stops |
| `overlay/OverlayChrome.kt` | Cursor + ask pass through during a stroke |

API key is still `gemini.api.key` in `local.properties`. The socket uses `?key=` on the Gemini Live URL.

See `docs/live-routing.md`, `docs/live-jobs.md`, `docs/agent.md`, `docs/brain.md`, `docs/hands.md`, and `docs/type.md`.
