# Live routing — talk first, jobs on purpose

Live is a conversation that can use hands. It is not a task runner that happens to speak.

## What went wrong

The Live model has tools, and `run_goal` was written as a catch-all: any question about the phone, anything that might take more than one step. Function-calling models bias toward calling a tool once one exists. Gemini Live also has no reliable `toolConfig` “don’t call tools” switch mid-session.

So “hey how are you?”, “what are you seeing on the screen?”, and “move the buddy up” all became `run_goal`. Live stopped, the agent runner said “On it.”, and it felt like chat mode or a task had hijacked the talk.

## How this is usually solved

Voice products keep a **veto list**, not an allow list, once a tool was chosen:

1. **Greetings and “what do you see” never start a job.** That is the talk-first rule.
2. **One-shots Live can do itself** (open one app, go home, nudge) stay in Live.
3. **Everything else the model already put in `run_goal` is a Goal.** An allow-list of phrases will miss “log me out” vs “log out” and the job dies as Talk.
4. **The orchestrator still decides** — Talk and Act are vetoes / fulfillments; Goal is the default after that.

Google’s Live guidance matches this: put an **invocation condition** on each tool, and keep descriptions from overlapping. `run_goal` is no longer “any question about the phone.”

## What Buddy does

The Live model already called `run_goal`. The router **vetoes** greetings and “what do you see”, and **fulfills** one-shots itself. Anything else is a Goal — we do not require a keyword list, because “open Pinterest and log me out” is a job even when it does not match `log out` as two words. A fact the web can answer should never have been `run_goal`; if it was, the tool response tells Live to search and speak instead (`docs/search.md`).

```
they speak
  → Live may call a tool
  → LiveListen: greeting tools stay blocked
  → if the tool is run_goal, LiveRouter.decide(goal):
        Talk  → stay in Live, tool response says “not a job”
        Act   → one-shot (nudge, fly, open app, back, home) in Live
        Goal  → ack, AgentRunner on this same session (`docs/live-jobs.md`)
```

| They say | Route |
|----------|--------|
| “hey how are you?” | Talk — answer with voice |
| “what are you seeing on the screen?” | Talk — answer from SCREEN |
| “what’s the weather?” | Talk — Live calls `search_web` (not a job) |
| “move the buddy up” | Act — `nudge`, stay in Live |
| “open Calculator” | Act — `open_app`, stay in Live |
| “open Pinterest and log me out” | Goal — runner on this same talk |
| “how much storage do I have left?” | Goal — runner on this same talk |

Typed asks are unchanged: on-device verbs first, everything else is still a goal (`docs/brain.md`). Voice needed the extra gate because there is no transcript to classify before the model speaks — only the `goal` string on the tool call.

## Composition

| File | Role |
|------|------|
| `brain/live/LiveRouter.kt` | Talk / Act / Goal — pattern-first, no network |
| `brain/live/LivePrompt.kt` | Talk is the default; search the web for facts; `run_goal` only for a multi-step job |
| `brain/live/LiveTools.kt` | Narrow `run_goal`; `nudge` for up / down / left / right |
| `brain/live/BuddyLive.kt` | Runs the router; jobs stay on this socket |
| `brain/BuddyMoveIntent.kt` | Shared “move up” / “top left” parse |

See `docs/live.md`, `docs/live-jobs.md`, `docs/brain.md`, and `docs/search.md`.
