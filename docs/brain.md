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

`place` is a named spot (`top_left`, `center`, …), not pixels. Not Live Mode. Not Computer Use. A later Live adapter should call these same tools.

Sliding the buddy around is on-device (`BuddyMoveIntent`) — “move up”, “go to the top left”. Tap / hold / swipe / drag at the current tip is also on-device (`BuddyHandIntent`) — “tap”, “hold this”, “swipe left”. Gemini is for named controls (“tap Wi‑Fi”). A DNS miss cannot block a nudge or a tap-here.

## Flow

1. If they asked the buddy itself to move, fly or nudge locally and speak. No network.
2. If they asked to tap / hold / swipe / drag *here*, do that locally. No network.
3. If the ask panel is covering the screen (typed ask), close it and wait a beat so the accessibility tree is the real screen — not the typed field.
4. Snapshot the screen they are looking at (`GuidanceCatalog` — ids and labels, no pixels), including the launcher under the overlay. Strip a node whose label is the question itself.
5. If the radio is down, say so — do not wait on DNS. Otherwise send the user’s words + that list to Gemini Flash.
6. Speak `say`. Then fly, point, or perform the hand stroke on **that** snapshot.

Typed send cancels leftover listening so a later STT miss cannot overwrite a real error.

Voice in is on-device `SpeechRecognizer`. Voice out is on-device TTS. Gemini only sees text.

## Why “move to the top-left” used to do nothing useful

Typed Ask snapshotted **while the panel was open**. The text field’s accessibility label was the question, so its id became a 40-character slug of those words (`move_the_cursor_in_the_top_left_corner_o`). Gemini called `say` + `point_to` on that field — `Brain.point found=true ok=true` — and the cursor flew to the sheet, not a corner. There was also no `fly_to` tool, so a spatial ask could not be honored even on a clean catalog.

## Composition

| File | Role |
|------|------|
| `brain/BuddyBrain.kt` | Orchestrator |
| `brain/GeminiClient.kt` | Gemini Developer API (REST) |
| `brain/GeminiTools.kt` | `say` / `point_to` / `fly_to` / `tap` / `hold` / `swipe` / `drag` |
| `brain/GuidancePrompt.kt` | System + user prompt |
| `brain/GuidanceCatalog.kt` | Snapshot → compact list |
| `brain/GuidancePlan.kt` | `say` + `element_id` + `place` + `hand` |
| `brain/HandPlan.kt` | Tap / hold / stroke from the model |
| `brain/BuddyMoveIntent.kt` | On-device “move up / top left” |
| `brain/BuddyHandIntent.kt` | On-device “tap / hold / swipe left” |
| `brain/Reachability.kt` | Online check + network-error detect |
| `brain/BuddyVoice.kt` | STT + TTS |
| `brain/BrainSession.kt` | Listening / thinking / ask sheet |
| `overlay/CursorLanding.kt` | Named spots → 0…1 |
| `ui/home/AskBuddySheet.kt` | Ask panel UI |
| `overlay/AskOverlayWindow.kt` | Hosts that panel over any app |
| `debug/BuddyLog.kt` | `Buddy===TRACE` logcat lines |

The API key is `gemini.api.key` in `local.properties` (gitignored) → `BuildConfig.GEMINI_API_KEY`. Never commit it.

Model: `gemini-3.5-flash-lite` only.

## How to try it

1. Start the buddy, turn on **Buddy Assistant**, allow the microphone once via **Ask buddy**.
2. Leave the app. Double-tap the cursor — the panel should open on the launcher or whatever you opened.
3. Ask “move up” or “move to the top-left” — the cursor should fly even with no internet.
4. Ask “tap” or “hold this” — it should press where it is. Ask “tap Wi‑Fi” on a list — it should fly there and tap.
5. Ask something on this screen (or type it) — Buddy should speak and point or tap a real control, not the ask field.
6. Open Settings, pull the notification, tap **Ask buddy**. After the shade closes it asks what you need, then points, taps, or speaks.

See `docs/eyes.md`, `docs/cursor-hands.md`, and `docs/hands.md`.
