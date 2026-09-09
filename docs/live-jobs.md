# Live jobs — one talk, a silent worker

Live is the only voice. A long job does not start a new talk, steal the mic, or speak with Android TTS.

## What went wrong

`run_goal` used to **stop Live**, hand the screen to the runner, let the runner talk with TTS, then **open a fresh Live socket**. That new session had no memory of “hey, after this, remind me…” — and it felt like chat mode had hijacked the conversation.

## How this is usually solved

Voice products keep the conversation socket up and treat long work as a **background tool**:

1. **Supervisor + worker.** The voice agent stays in the call. A silent worker owns the hands. Alexa skills, Assistant routines, and Gemini Live “ack then complete” all look like this.
2. **Ack immediately.** Gemini 3.1 Flash Live only does **synchronous** function calling — the model will not speak until it has a tool response. So we reply at once: “started — keep talking.” Then the runner works.
3. **Result as a later turn.** When the worker finishes, we inject `JOB DONE` on the **same** WebSocket (`clientContent`, turn complete). Live tells them, in the same talk.
4. **Hands have one owner.** While the worker is on the screen, Live’s tap / fly / type tools are refused. Talk is still open, and SCREEN still follows so “what do you see?” matches the phone.

Google’s GenAI processors do the same for long tools: silent “running” ack, then a later result. We cannot use `NON_BLOCKING` on this Live model yet; the ack-then-complete pattern is the portable substitute.

## What Buddy does

```
they ask for a real job
  → LiveRouter says Goal
  → tool response: started (talk continues)
  → AgentRunner works the screen (no TTS)
  → Live can still chat; screen tools are blocked; SCREEN still follows
  → they say stop → cancel_job → same talk, SCREEN keeps following
  → runner done → JOB DONE on the same socket
  → Live tells them, then waits
```

If the worker needs a yes/no or a password: `JOB ASK` → Live asks out loud → `answer_job` → the worker continues. They never leave the talk.

| They | Live | Runner |
|------|------|--------|
| “how are you?” | talks | — |
| “how much storage left?” | “on it,” stays on | walks Settings |
| chat while it works | talks (and may search the web) | has the hands |
| job finishes | tells them the result | idle |
| “stop that” | `cancel_job` | stops |

Typed asks (no Live) still use Android TTS and the sheet (`SpokenDesk`). Live jobs use `LiveDesk` — the runner only talks to Live.

## Composition

| File | Role |
|------|------|
| `brain/agent/AgentDesk.kt` | How a result or a question leaves the runner |
| `brain/live/LiveDesk.kt` | Same Live socket — JOB ASK / JOB DONE |
| `brain/agent/SpokenDesk.kt` | Typed path — TTS + ask sheet |
| `brain/live/BuddyLive.kt` | Session stays up; acks `run_goal`; blocks screen tools during a job |
| `brain/agent/AgentRunner.kt` | Silent see → think → act loop |

See `docs/live.md`, `docs/live-routing.md`, `docs/agent.md`, and `docs/search.md`.
