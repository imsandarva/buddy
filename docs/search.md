# Web search

Buddy looks things up on the web when the screen and its own knowledge are not enough — a current fact, how this phone maker names a menu, a path it is not sure of.

One lookup module: `WebSearch` calls Gemini `generateContent` with `google_search` and **no** JSON schema. Both doors use it.

## Why not Live’s built-in search

Google’s Live API can attach `googleSearch` next to function calls. In practice that is a bad fit here:

- Declaring it on setup has closed the socket at once with **1011 quota** on keys that otherwise talk fine.
- Invoking it on `gemini-3.1-flash-live-preview` has also killed sessions with **1007 invalid argument**.
- The runner already cannot mix `google_search` with structured JSON on the same `generateContent` call.

So Live declares a normal `search_web` function. We run [WebSearch], send the note back as the tool response, and Live speaks it. The socket stays up.

```
Live talk
  → they ask a fact not on SCREEN
  → search_web(query)
  → WebSearch.lookup (REST + google_search)
  → tool response with the note
  → Live speaks the answer

Runner (typed ask, or Live's run_goal)
  → decide (structured JSON, no search on this call)
  → action search_web → same WebSearch.lookup
  → result lands in RECENT STEPS
  → next turn: act on the phone, or finish with the answer
```

The prompts name the tool. Live: call `search_web`; do not start a job for weather. Runner: `search_web` whenever SCREEN is not enough; do not search the list you already have; do not search in a loop.

## What it is not

Not opening Chrome. Not Live’s built-in `googleSearch`. Not a third-party search key.

## Composition

| File | Role |
|------|------|
| `brain/agent/WebSearch.kt` | One grounded `generateContent` call; no JSON schema |
| `brain/agent/AgentAction.SearchWeb` | The runner’s search turn — query in `text` |
| `brain/agent/AgentRunner.kt` | Runs search before hands; same SCREEN afterwards |
| `brain/live/LiveTools.kt` | Live `search_web` function |
| `brain/live/BuddyLive.kt` | Executes it through [WebSearch]; allowed while a job has the screen |
| `brain/live/LivePrompt.kt` | Call `search_web`; not a job |

See `docs/agent.md`, `docs/live.md`, `docs/live-routing.md`, and `docs/brain.md`.
