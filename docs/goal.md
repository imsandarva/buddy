# Goal runner

Buddy can finish a job that takes many phone steps — log out of an app, open Settings and type a Wi‑Fi password — without asking in the middle.

It uses **the screen in front of you**. The cursor opens apps and taps there. It does not work in a hidden window.

## Door vs hands

| | Role |
|--|------|
| **Live** (or a typed ask) | Hears the goal. Speaks a short “on it.” Calls `run_goal`. |
| **Goal runner** | Chat REST loop. Snapshot → one tool → wait for the new screen → repeat. |

Live is a conversation. After one tap it waits for more speech. The runner does not wait. Same eyes, hands, and type as a single tap. When the runner finishes, it speaks one line, then **Live starts again** so they can keep talking — they should not have to double-tap.

## Loop

1. Close talk. Show **I’m on it** in the notification.
2. Snapshot SCREEN (accessibility list, no pixels).
3. `generateContent` (`gemini-3.5-flash-lite`) with GOAL + LAST STEPS + SCREEN.
4. One tool: `open_app`, `tap`, `type`, `swipe`, … or `done`.
5. Wait for the tree to settle. Repeat until `done`, the same screen three times, 20 steps, or they stop.
6. Speak a short wrap-up, then reopen Live (mic on, wait for them — no extra hello).

`open_app` launches by the icon name so it does not have to hunt the drawer first.

## Stop

Double-tap the cursor, or **End** / **Stop buddy** in the notification. That cancels the run and does **not** reopen Live.

A finished job always comes back to Live. A cancel does not.

## Composition

| File | Role |
|------|------|
| `brain/goal/GoalRunner.kt` | Loop, stuck / max-step stop, TTS at start and end |
| `brain/goal/GoalPrompt.kt` | One-step-toward-goal contract |
| `brain/goal/AppLauncher.kt` | Open an installed app by launcher label |
| `brain/GeminiClient.kt` | `goalStep` — same HTTP client as typed ask |
| `brain/GeminiTools.kt` | `run_goal` on Live/typed; `open_app` + `done` on the runner |

See `docs/brain.md`, `docs/live.md`, `docs/eyes.md`, and `docs/hands.md`.
