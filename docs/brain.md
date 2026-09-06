# Brain

The model does not move the cursor and does not walk the tree. It returns tools. The app executes them.

| Tool | App does |
|------|----------|
| `say` | Android TTS — warm words only |
| `point_to(element_id)` | `BuddyScreenEyes.pointTo` on the snapshot used for that turn |
| `fly_to(place)` | `CursorLanding` → `BuddyCursorController.animateToNormalized` |

`place` is a named spot (`top_left`, `center`, …), not pixels. Not Live Mode. Not Computer Use. A later Live adapter should call these same tools.

Sliding the buddy around is on-device (`BuddyMoveIntent`) — “move up”, “go to the top left”. Gemini is only for “how do I tap this?”. That way a DNS miss (`Unable to resolve host generativelanguage.googleapis.com`) cannot block a cursor nudge.

## Flow

1. If they asked the buddy itself to move, fly or nudge locally and speak. No network.
2. If the ask panel is covering the screen (typed ask), close it and wait a beat so the accessibility tree is the real screen — not the typed field.
3. Snapshot the active screen (`GuidanceCatalog` — ids and labels, no pixels). Strip a node whose label is the question itself (the ask-field echo).
4. If the radio is down, say so — do not wait on DNS. Otherwise send the user’s words + that list to Gemini Flash.
5. Speak `say`. If they asked the buddy to move, fly to `place`. Otherwise fly to `element_id` from **that** snapshot.

Typed send cancels leftover listening so a later STT miss cannot overwrite a real error.

Voice in is on-device `SpeechRecognizer`. Voice out is on-device TTS. Gemini only sees text.

## Why “move to the top-left” used to do nothing useful

Typed Ask snapshotted **while the panel was open**. The text field’s accessibility label was the question, so its id became a 40-character slug of those words (`move_the_cursor_in_the_top_left_corner_o`). Gemini called `say` + `point_to` on that field — `Brain.point found=true ok=true` — and the cursor flew to the sheet, not a corner. There was also no `fly_to` tool, so a spatial ask could not be honored even on a clean catalog.

## Composition

| File | Role |
|------|------|
| `brain/BuddyBrain.kt` | Orchestrator |
| `brain/GeminiClient.kt` | Gemini Developer API (REST) |
| `brain/GeminiTools.kt` | `say` / `point_to` / `fly_to` declarations + parse |
| `brain/GuidancePrompt.kt` | System + user prompt |
| `brain/GuidanceCatalog.kt` | Snapshot → compact list |
| `brain/GuidancePlan.kt` | `say` + `element_id` + `place` |
| `brain/BuddyMoveIntent.kt` | On-device “move up / top left” |
| `brain/Reachability.kt` | Online check + network-error detect |
| `brain/BuddyVoice.kt` | STT + TTS |
| `brain/BrainSession.kt` | Listening / thinking / ask sheet |
| `overlay/CursorLanding.kt` | Named spots → 0…1 |
| `ui/home/AskBuddySheet.kt` | Ask panel (owned overlay — not ModalBottomSheet) |
| `debug/BuddyLog.kt` | `Buddy===TRACE` logcat lines |

The API key is `gemini.api.key` in `local.properties` (gitignored) → `BuildConfig.GEMINI_API_KEY`. Never commit it.

Model: `gemini-3.6-flash` (current Flash for new API keys), then `gemini-3.5-flash-lite` if that call fails. Older 2.x Flash IDs return 404 for new users.

## How to try it

1. Start the buddy, turn on **Buddy Assistant**, allow the microphone once via **Ask buddy**.
2. Ask “move up” or “move to the top-left” — the cursor should fly even with no internet.
3. Ask something on this screen (or type it) — Buddy should speak and point at a real control, not at the ask field.
4. Open Settings, pull the notification, tap **Ask buddy**. After the shade closes it asks what you need, then points and speaks.

See `docs/eyes.md` and `docs/cursor-hands.md`.
