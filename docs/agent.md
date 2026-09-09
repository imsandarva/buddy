# Agent runner

Buddy finishes jobs on the phone the way a person does: **look → think a little → act → look again**, until the goal is reached. “How much storage do I have left?” becomes: open Settings, search *storage*, tap the result, read the number, say it out loud.

This replaces the old goal runner. Same eyes and hands underneath — a new mind on top.

## What was wrong before

| Old | Why it could not finish real jobs |
|-----|-----------------------------------|
| Stateless step loop, 6 trail lines | No memory, no plan, no idea whether the last tap *worked* |
| Flat `id \| "label" \| tap` list | No ON/OFF, no rows (title · detail · switch), no scroll containers, no keyboard, no dialogs from other packages |
| Tools: tap / hold / page-swipe / type | No back, home, scroll-inside-a-list, wait, ask, or “read the answer” |
| Stuck = same screen ×3 → give up | The model was never told it was stuck |
| Three doors, three prompts, one mutable plan object | Hard to reason about, hard to extend |

## The loop

```
observe  SceneDescriber.describe(snapshot)          — APP, KEYBOARD, one line per control with state and position
decide   AgentDecider → structured JSON             — thought · progress (memory) · say · action · done
guard    AgentGuard.review                          — limits, risky press → ask first
search   WebSearch.lookup (only for search_web)     — grounded Google Search; result into RECENT STEPS
act      AgentExecutor.perform(action, snapshot)    — eyes / hands / type / global keys / app launch
settle   awaitSettledSnapshot()                     — wait for the accessibility event stream to go quiet
reflect  SceneDiff.describe(before, after)          — “now in Settings”, “Wi‑Fi is now ON”, “nothing changed”
remember AgentMemory.record(...)                    — last 8 steps with outcomes + the model's own progress note
```

Every step is one `generateContent` call. Memory travels in the message, so any model tier can take any step. This is the shape Google's Computer Use loop and the AndroidWorld agents use; AndroidWorld found the **text-only accessibility-tree agent matches screenshot agents on Android**, so we stay pixel-free.

### What the model sees

```
APP: Settings
KEYBOARD: open (a field is ready for typing; back closes it)
SCREEN (top to bottom; [id] kind "label" state @x,y in % of the screen):
[search_action_bar] field "Search settings" focused @50,8
[network] item "Network & internet · Wi‑Fi, mobile, hotspot" @50,22
[switch_widget] switch "Wi‑Fi" ON @91,30
[recycler_view] list (scrollable) "list" @50,60
(12 more lines not shown)
Lists marked scrollable may hold more below or above — scroll to see it.
```

- A tappable row is **one line**: its title and detail joined with ` · `. A switch inside the row is its own line, named after the row.
- Kinds: button, item, switch, checkbox, radio, tab, field, text, heading, image, slider, list (scrollable).
- States: ON/OFF, selected, disabled, focused, password, slider percent.
- `@x,y` lets the model reason about rows and what sits near the bottom. Pixels never leave the app — an action names an **id**, the app resolves it.
- Dialogs, permission prompts, and popup menus stacked above the front app are included, whatever package they belong to.

### What the model returns

```json
{
  "thought": "Storage isn't in view. Settings has a search field at the top.",
  "progress": "In Settings main list. Plan: search 'storage', tap result, read free space.",
  "say": "Let me search for it.",
  "action": { "type": "type", "target": "search_action_bar", "text": "storage", "submit": true },
  "done": null
}
```

`done` is `{ "status": "done" | "cannot", "message": "..." }` when the run is over. If they asked a question, `message` carries the answer read off the screen.

### Actions — what a finger and a phone can do

| Action | Hands |
|--------|-------|
| `tap`, `long_press` | Fly to the control, then `dispatchGesture` |
| `type` (target?, text, submit) | Tap the field, `ACTION_SET_TEXT`, optional IME enter |
| `scroll` (direction, target?) | Measured pan inside the list (40% of it, rests before lift so nothing flings past) |
| `swipe` (direction) | Page pull across the screen — launcher pages, dismiss |
| `drag` (from, to) | Press, hold, slide |
| `back`, `home`, `recents`, `notifications`, `quick_settings` | `performGlobalAction` |
| `open_app` (name) | Launch by label — no drawer hunt |
| `wait` | 1.2 s for a loading screen |
| `point` | Fly to show, don't press — for “where is”, “show me” |
| `move_cursor` | Named place — only when they asked the buddy itself to move |
| `ask` (question) | Pause, speak the question, listen; the reply arrives as THEY SAID |
| `search_web` (query) | Grounded Google Search — not a finger; result comes back in RECENT STEPS (`docs/search.md`) |
| `none` | Only together with `done` |

### Guard

- **Stuck**: “nothing changed” once → a gentle note; twice → the stronger model + “do something different”; five → stop.
- **Repeats**: same action three times → note; four → stop. Failures likewise.
- **Search**: three lookups in one run → a note to use what you already found.
- **Risky press**: tapping *Delete / Uninstall / Reset / Pay / Send / Sign out …* asks first — unless the goal already asked for exactly that (“log me out” → no extra question). A “no” ends the run untouched.
- **Limits**: 30 steps, 4 minutes, 90 s for an answer.

### Two brains

| Tier | Model | When |
|------|-------|------|
| Fast | `gemini-3.5-flash-lite`, thinking `high` | Every step (for now) |
| Strong | `gemini-3.5-flash-lite`, thinking `high` | Same — stall escalation is a no-op until we split tiers again |

Structured output via `generationConfig.responseJsonSchema` (falls back to the OpenAPI `responseSchema` dialect on a 400). Same prompt, same form. Web search is a **separate** `generateContent` call — JSON schema on the decision call would silently drop grounding. See `docs/search.md`.

### Asking the person

`ask` (or a guard confirmation) goes through [AgentDesk]. On a Live talk, Live asks out loud and `answer_job` brings the words back. On a typed ask, the sheet opens. Whatever they say next goes to the run. “Not now” / **End** cancels.

### Talking

The runner does not talk to the person. Milestones go to the notification. The closing `done.message` goes to the desk: Live tells them on the **same** socket; a typed ask uses Android TTS. See `docs/live-jobs.md`. Cancel (double-tap, **End**) stops the job and, if they were talking, the talk.

## Doors

| Door | Path |
|------|------|
| Live voice | One-shot tools run through `AgentExecutor`; `run_goal` starts the silent runner on the **same** Live session (`docs/live-jobs.md`) |
| Typed / STT sheet | Nudge, tap-here, type-here stay on-device; everything else is a goal; the runner speaks through `SpokenDesk` |
| Runner waiting on `ask` | Live: `answer_job`. Typed: the next words on the sheet |

## Composition

| File | Role |
|------|------|
| `brain/agent/AgentRunner.kt` | The loop, lifecycle, ask/answer, wrap-up through [AgentDesk] |
| `brain/agent/AgentDesk.kt` | How a result or a question leaves the runner |
| `brain/agent/SpokenDesk.kt` | Typed path — TTS + ask sheet |
| `brain/live/LiveDesk.kt` | Live path — JOB ASK / JOB DONE on the same socket |
| `brain/agent/AgentDecider.kt` | One structured decision per call; fast / strong tiers |
| `brain/agent/AgentPrompt.kt` | System contract + step message |
| `brain/agent/AgentSchema.kt` | Response JSON Schema (+ OpenAPI dialect) |
| `brain/agent/AgentDecision.kt` | thought · progress · say · action · done |
| `brain/agent/AgentAction.kt` | Sealed action space + JSON parse |
| `brain/agent/AgentExecutor.kt` | Action → eyes / hands / type / keys / launcher → `Outcome` |
| `brain/agent/AgentGuard.kt` | Stuck, repeats, risky press, limits, search cap, tier escalation |
| `brain/agent/WebSearch.kt` | Grounded Google Search for one `search_web` turn |
| `brain/agent/AgentMemory.kt` | Progress note + last steps + answers |
| `brain/agent/SceneDescriber.kt` | Snapshot → SCREEN text |
| `brain/agent/SceneDiff.kt` | Before/after change summary |
| `brain/agent/AppLauncher.kt` | Open an app by label |
| `brain/agent/AgentState.kt` | Idle / Working / Asking / Finishing |
| `brain/GeminiClient.kt` | `generateContent` HTTP + response text |
| `accessibility/BuddyGlobal.kt` | Back / Home / Recents / Notifications / Quick settings |
| `accessibility/ScreenReady.kt` | `awaitSettledSnapshot` — quiet-period settle |

## Try it

1. Start the buddy, turn on **Buddy Assistant**, allow the microphone once.
2. Double-tap the cursor and say: “How much storage do I have left?” — keep talking; watch it open Settings, search, tap; then hear the number from Live, still the same talk.
3. Type: “Turn off Bluetooth.” — it reads the switch state first and only taps if it is on.
4. Say: “Delete the last photo.” — it navigates there, then asks before the delete.
5. Say: “Show me where I change the font size.” — it goes there and points instead of pressing.
6. Type: “What’s the weather today?” — it searches the web and speaks the answer, without walking the phone.
7. Pull down notifications during a run: **I’m on it** shows what Buddy is doing right now; **End** stops it.

Logcat filter `Buddy===TRACE`: `Agent.decide`, `Agent.decision`, `Agent.act`, `Agent.search`, `Eyes.snapshot`, `Hands.stroke`, `Global.press`.

See `docs/brain.md`, `docs/eyes.md`, `docs/hands.md`, `docs/live.md`, `docs/search.md`.
