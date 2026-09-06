# Brain

The model does not move the cursor and does not walk the tree. It returns two tools. The app executes them.

| Tool | App does |
|------|----------|
| `say` | Android TTS — warm words only |
| `point_to(element_id)` | `BuddyScreenEyes.pointTo` on the snapshot used for that turn |

Not Live Mode. Not Computer Use. A later Live adapter should call these same tools.

## Flow

1. Snapshot the active screen (`GuidanceCatalog` — ids and labels, no pixels).
2. Send the user’s words + that list to Gemini Flash (`generateContent` + function calling).
3. Speak `say`. Fly to `element_id` from **that** snapshot.

Voice in is on-device `SpeechRecognizer`. Voice out is on-device TTS. Gemini only sees text.

## Composition

| File | Role |
|------|------|
| `brain/BuddyBrain.kt` | Orchestrator |
| `brain/GeminiClient.kt` | Gemini Developer API (REST) |
| `brain/GuidancePrompt.kt` | System + user prompt |
| `brain/GuidanceCatalog.kt` | Snapshot → compact list |
| `brain/GuidancePlan.kt` | `say` + `element_id` |
| `brain/BuddyVoice.kt` | STT + TTS |
| `brain/BrainSession.kt` | Listening / thinking / ask sheet |
| `ui/home/AskBuddySheet.kt` | Ask surface |

The API key is `gemini.api.key` in `local.properties` (gitignored) → `BuildConfig.GEMINI_API_KEY`. Never commit it.

Model: `gemini-2.5-flash`, then `gemini-2.0-flash` if the first call fails.

## How to try it

1. Start the buddy, turn on **Buddy Assistant**, allow the microphone once via **Ask buddy**.
2. In the app, ask something on this screen — or type it.
3. Open Settings, pull the notification, tap **Ask buddy**. After the shade closes it asks what you need, then points and speaks.

See `docs/eyes.md` and `docs/cursor-hands.md`.
