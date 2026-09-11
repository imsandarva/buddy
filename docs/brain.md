# Brain

The model does not move the cursor and does not walk the tree. It names an action; the app executes it and shows the model what changed. One action space serves everything — a live “tap Wi‑Fi”, a typed “turn off Bluetooth”, and a twelve-step “free up some space”.

| Layer | Role |
|-------|------|
| **Senses** — `accessibility/` | Eyes (`BuddyScreenEyes`), hands (`BuddyHands`), type (`BuddyType`), keys (`BuddyGlobal`) |
| **Perception** — `brain/agent/SceneDescriber`, `SceneDiff` | Snapshot → SCREEN text; before/after → “what changed” |
| **Mind** — `brain/agent/AgentDecider`, `AgentPrompt`, `AgentGuard`, `AgentMemory` | One structured decision per step, with memory and judgement |
| **Body** — `brain/agent/AgentExecutor` | `AgentAction` → senses → `Outcome` |
| **Runner** — `brain/agent/AgentRunner` | The see → think → act → see loop; ask and answer; wrap-up |
| **Doors** — `brain/BuddyBrain`, `brain/live/BuddyLive` | Typed words and live voice; Live keeps one session while the runner works |

See `docs/agent.md` for the loop, the action space, and the guard.

## Flow of a request

1. **Buddy itself** (“move up”, “go to the top left”) — on-device `BuddyMoveIntent`, no network.
2. **Here** (“tap”, “hold this”, “swipe left”, “scroll down”, “type hello”, “press enter”) — on-device `BuddyHandIntent` / `BuddyTypeIntent`.
3. **Live one-shot** (“tap Wi‑Fi”, “scroll down”, “move up”, “go back”) — Live's function call → `LiveTools.intent` → `AgentExecutor.perform` on the snapshot the model saw → tool response carries the new SCREEN.
4. **A real multi-step job** — a goal for `AgentRunner`. Typed asks go straight there (`SpokenDesk`). Live may call `run_goal`; `LiveRouter` only starts a job when they asked for phone work. The runner is silent. Live stays on the same socket and tells them when it is done. See `docs/live-jobs.md`.
5. **A question mid-run** — the runner asks through the desk. Live asks out loud and `answer_job` brings the words back. Typed asks still use the sheet.

The ask sheet is closed before eyes or hands run (a voice tap used to land on the still-open sheet).

## Voice

Live talk is Gemini Live (`gemini-3.1-flash-live-preview`): raw mic PCM in, `Orion` (deep male) PCM out, no speech-to-text, no captions. SCREEN goes to Live as `realtimeInput` text whenever their screen changes (including while a job is walking the phone) and again when they start speaking. The first hello plays through on the same socket — the mic stays closed until that line has left the speaker, so Buddy cannot barge into himself and restart the greeting. First meeting (onboarding activation) uses the same Live voice for one hello, then hangs up — never device TTS. Typed asks use the sheet and Android TTS. Mid-run questions on Live stay in the talk. See `docs/live.md` and `docs/live-jobs.md`.

## How we instruct the model

Two prompts, one action vocabulary:

- `brain/agent/AgentPrompt.kt` — the runner. Role, objective, what it receives, **how this phone works** (Settings search, maker names, scrolling, switches, dialogs), actions including `search_web`, how to decide, asking, showing vs doing, finishing, talking, rules. The step message carries GOAL, PROGRESS, RECENT STEPS, THEY SAID, NOTE, SCREEN NOW.
- `brain/live/LivePrompt.kt` — the talk. Greet, wait, talk first, search the web when SCREEN is not enough, one quick tool when asked, `run_goal` on this same session.

Both repeat the matching rule: the name they say is the quoted label; the value you pass is that line's id, copied character for character. Both are told to search the web when a fact or path is missing. See `docs/search.md`.

The API key is the user's own, entered once during onboarding ("give buddy a brain") or replaced from Settings — never bundled at build time. A temporary `data/DevApiKeySeed.kt` only prefills those fields so we can check the funnel; it is not the store. `data/ApiKeyStore.kt` holds it in a private, on-device SharedPreferences file; `GeminiClient` takes it as a supplier (`ApiKeyStore::currentKey`), so a key replaced mid-session is picked up on the very next call, by the runner and by Live's search alike. See `docs/onboarding.md`.

Models: runner `gemini-3.5-flash-lite` with `thinkingLevel: high` (for now, both fast and strong). Live `gemini-3.1-flash-live-preview`, `thinkingLevel: minimal`, 220 ms VAD.

## When the key stops working

A 401/403 from `GeminiClient` (`ApiException.isAuthError`) or an auth-shaped Live close reason (`LiveFail.isAuthError`) both flag `ApiKeyStore.markInvalid()` — never a raw error surfaced to the person. The home screen then shows one quiet line pointing at Settings, and Settings opens its key section straight into edit mode.

## Composition

| File | Role |
|------|------|
| `brain/BuddyBrain.kt` | Typed door — on-device verbs, else the runner; answers routed to a waiting run |
| `brain/agent/` | The runner and its parts — see `docs/agent.md` |
| `brain/live/` | Gemini Live socket, audio, one-shot first-meeting hello, listen gate, talk/job router, same-session jobs — see `docs/live.md` |
| `brain/GeminiClient.kt` | `generateContent` HTTP |
| `brain/agent/WebSearch.kt` | Grounded Google Search for the runner |
| `brain/BuddyMoveIntent.kt` | On-device “move up” / “top left” |
| `brain/BuddyHandIntent.kt` | On-device “tap / hold / swipe left” at the tip |
| `brain/BuddyTypeIntent.kt` | On-device “type hello” / “search for pizza” |
| `brain/Reachability.kt` | Online check + network-error detect |
| `brain/BuddyVoice.kt` | Android STT + TTS |
| `brain/BrainSession.kt` | Phase, note, sheet / live / work flags, progress line |
| `overlay/CursorLanding.kt` | Named spots → 0…1 |
| `ui/home/AskBuddySheet.kt` | Ask panel UI |
| `overlay/AskOverlayWindow.kt` | Hosts that panel over any app |
| `debug/BuddyLog.kt` | `Buddy===TRACE` logcat lines |

## Try it

1. Start the buddy, turn on **Buddy Assistant**, allow the microphone once via **Ask buddy**.
2. Double-tap the cursor from any screen. Say “how are you?” or “what are you seeing?” — Live just talks. Say “what’s the weather?” — Live searches and answers. Say “move up” — the buddy slides, still in Live. Say “open Calculator” — Live taps. Say “how much storage do I have left?” — Live stays with you while the cursor works Settings; then Live tells you the number.
3. Type the same things in the sheet — same runner, spoken result. A typed fact still uses `search_web`.
4. “Move up”, “tap”, “type hello” work with no internet.
5. Say “delete this photo” — Buddy asks before the delete. Say “show me where the font size is” — it points instead of pressing.

See `docs/agent.md`, `docs/eyes.md`, `docs/hands.md`, `docs/type.md`, `docs/live.md`, `docs/search.md`, and `docs/live-jobs.md`.
