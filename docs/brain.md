# Brain

The model does not move the cursor and does not walk the tree. It returns tools. The app executes them.

| Tool | App does |
|------|----------|
| `say` | Android TTS — warm words only |
| `point_to(element_id)` | `BuddyScreenEyes.pointTo` on the snapshot used for that turn |
| `fly_to(place)` | `CursorLanding` → `BuddyCursorController.animateToNormalized` |
| `tap(element_id?)` | Fly to that control (or the tip), then a real tap |
| `hold(element_id?)` | Fly, then long-press |
| `swipe` / `drag` | Quick slide, or hold-then-slide |
| `type(text, element_id?, submit?)` | Fill a text field, optionally press Search / Send |
| `run_goal(goal)` | Hand a multi-step job to the chat runner — Live / typed door only |
| `open_app(name)` / `done` | Runner only — launch by label, or stop the loop |

`place` is a named spot (`top_left`, `center`, …), not pixels. Not Computer Use. **Live** is a second brain adapter on these same tools (except `say` — Live speaks with native audio). See `docs/live.md`.

Sliding the buddy around is on-device (`BuddyMoveIntent`) — “move up”, “go to the top left”. Tap / hold / swipe / drag at the current tip is also on-device (`BuddyHandIntent`) — “tap”, “hold this”, “swipe left”. “Type hello” / “search for pizza” / “press enter” is on-device (`BuddyTypeIntent`). Gemini is for named controls (“tap Wi‑Fi”, “type pizza in the search box”). A DNS miss cannot block a nudge, a tap-here, or a type-here.

## Flow

1. If they asked the buddy itself to move, fly or nudge locally and speak. No network.
2. If they asked to type, search, or press enter *here*, do that locally. No network.
3. If they asked to tap / hold / swipe / drag *here*, do that locally. No network.
4. Spoken or typed: close the ask panel first and wait a beat. A voice tap used to land on the still-open sheet (scrim dismiss → job cancel). The sheet must be gone before eyes, hands, or type run.
5. Snapshot the screen they are looking at (`GuidanceCatalog` — ids and labels, no pixels), including the launcher under the overlay. Strip a node whose label is the question itself. Text fields are marked `type`.
6. If the radio is down, say so — do not wait on DNS. Otherwise send the user’s words + that list to Gemini Flash.
7. Speak `say`. Then fly, point, type, or perform the hand stroke on **that** snapshot.

Typed send cancels leftover listening so a later STT miss cannot overwrite a real error.

Voice **talk** is Gemini Live: raw mic PCM in, raw voice PCM out — no speech-to-text API, no captions, no screen-share video. Gemini’s “video Live” is JPEG frames at ≤1 fps, not a native share; we send the accessibility SCREEN list as realtime text instead, and we send it again when they change screens so the model sees the home screen, the notification shade, or the app in front of them. Voice **type** is still REST + Android TTS.

Live replies arrive as binary JSON frames on the WebSocket. If those frames are ignored, `setupComplete` never lands and the session falls back to the type sheet. Speech is played through a jitter buffer (never drop, clocked at 24 kHz); cursor tools run off the main thread. See `docs/live.md`.

## Typed vs live

Typed send still closes the sheet, snapshots, and calls `generateContent`. Double-tap with a microphone starts Live instead of STT.

## Why “move to the top-left” used to do nothing useful

Typed Ask snapshotted **while the panel was open**. The text field’s accessibility label was the question, so its id became a 40-character slug of those words (`move_the_cursor_in_the_top_left_corner_o`). Gemini called `say` + `point_to` on that field — `Brain.point found=true ok=true` — and the cursor flew to the sheet, not a corner. There was also no `fly_to` tool, so a spatial ask could not be honored even on a clean catalog.

## Composition

| File | Role |
|------|------|
| `brain/BuddyBrain.kt` | Orchestrator |
| `brain/GeminiClient.kt` | Gemini Developer API (REST) |
| `brain/GeminiTools.kt` | `say` / `point_to` / `fly_to` / `tap` / `hold` / `swipe` / `drag` / `type` |
| `brain/GuidancePrompt.kt` | System + user prompt — sectioned contract for the model |
| `brain/GuidanceCatalog.kt` | Snapshot → compact list |
| `brain/GuidancePlan.kt` | `say` + `element_id` + `place` + `hand` + `type` |
| `brain/HandPlan.kt` | Tap / hold / stroke from the model |
| `brain/TypePlan.kt` | Text + field id + submit from the model |
| `brain/GuidanceActor.kt` | Shared tap / fly / point / type executor |
| `brain/goal/` | Multi-step runner — Live/typed hands off; chat loop owns continue |
| `brain/live/` | Gemini Live WebSocket + jitter-buffered speaker + AEC mic + screen follow |
| `brain/BuddyHandIntent.kt` | On-device “tap / hold / swipe left” |
| `brain/BuddyTypeIntent.kt` | On-device “type hello” / “search for pizza” |
| `brain/Reachability.kt` | Online check + network-error detect |
| `brain/BuddyVoice.kt` | STT + TTS |
| `brain/BrainSession.kt` | Listening / thinking / ask sheet |
| `overlay/CursorLanding.kt` | Named spots → 0…1 |
| `ui/home/AskBuddySheet.kt` | Ask panel UI |
| `overlay/AskOverlayWindow.kt` | Hosts that panel over any app |
| `debug/BuddyLog.kt` | `Buddy===TRACE` logcat lines |

## How we instruct the model

`GuidancePrompt` is the first message. It is written like a production system prompt, not a paragraph of vibes: **role, objective, input contract, tool policy, matching rule, speech, hard rules, goal** — then the same matching rule again on the user turn.

The important contract, repeated on purpose:

- Latest SCREEN is the only truth.
- The name they say is the quoted **label**. The tool argument is that line’s **element_id**, copied exactly (`Pinterest` on screen may be `apps_icon_4`, never an invented `pinterest`).
- One spoken sentence. One phone step — or `run_goal` when the job takes many steps. Point is not tap. Flying the buddy is not pointing at a control.
- If SCREEN is empty, say so. Do not guess.

REST (`SYSTEM`) must call `say`. Live (`LIVE`) speaks with native audio and has no `say` tool. Tool declarations in `GeminiTools` repeat the same id rule so the schema and the prompt agree.

The API key is `gemini.api.key` in `local.properties` (gitignored) → `BuildConfig.GEMINI_API_KEY`. Never commit it.

Model (REST): `gemini-3.5-flash-lite` only. Model (Live): `gemini-3.1-flash-live-preview` with `thinkingLevel: minimal` and a short server VAD (220 ms silence).

## How to try it

1. Start the buddy, turn on **Buddy Assistant**, allow the microphone once via **Ask buddy**.
2. Leave the app. Double-tap the cursor — a live talk starts (or the type sheet if the mic is off).
3. Speak naturally. Ask “open Calculator” — Gemini should talk, then tap. Ask “log me out of Pinterest” — Live hands off to the goal runner; watch the cursor do the steps. Type the same kind of job and typed ask will hand off too.
4. Ask “move up” or “move to the top-left” — the cursor should fly even with no internet.
5. Ask “tap” or “hold this” — it should press where it is. Ask “tap Wi‑Fi” on a list — it should fly there and tap.
6. Open a search box and ask “type hello” — it should fill the field. Ask “search for pizza” — it should type and press search.
7. Ask something on this screen (or type it) — Buddy should speak and point or tap a real control, not the ask field.
8. Open Settings, pull the notification, tap **Ask buddy**. After the shade closes it asks what you need, then points, taps, types, or speaks.

See `docs/eyes.md`, `docs/cursor-hands.md`, `docs/hands.md`, `docs/type.md`, and `docs/goal.md`.
