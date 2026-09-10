# Onboarding

Buddy asks a stranger for two intimidating things: a personal Gemini key and the Accessibility
permission (Android's own dialog warns it can see everything on the screen). The whole first-run
funnel exists to earn the right to ask for those, in the right order, at the right moment — never
as a wall of asks at launch. This implements the design discussion in `zinfo.md`; nothing here is
a guess at what to build, only at how to build it well.

## The sequence

1. **Launch** — buddy's own mark, alone, breathing. No logo, no "Welcome" splash.
2. **Intro** — 2–3 swipeable cards, one idea each, skip always honestly available.
3. **API key** — "give buddy a brain." One field, live validation, the dormant mark visibly wakes
   on success. Standard form layout: one flex spacer. At rest it lives between the field and
   **Wake buddy up**; with the keyboard up it lives above the field, so the field and the CTA sit
   a tight 12dp apart and hug the keys. The line under the title stays — we do not hide copy when
   they type. The action never hides. Keyboard Done still submits.
4. **Activation** — buddy talks first with Gemini Live's own voice (`Fenrir`) — one short hello,
   then the socket hangs up. No Android TTS, no lingering live talk, no mic. Then it reveals the
   second ability in the same breath: it can act on the screen too.
5. **Overlay permission** — the "easy yes." Granting it is rewarded immediately: the real overlay
   cursor settles onto the screen right there, behind the priming card.
6. **Accessibility permission** — the highest-stakes screen in the app. Plain language for what it
   enables and, just as plainly, what it never does, before Android's own generic dialog appears.
7. **First guided task** — deliberately trivial: buddycursor's demo flight, then buddy opens the
   calculator. Boring but flawless beats impressive but risky.
8. **Home** — steady state.

Any "not now" at steps 4–7 ends the funnel immediately (never a repeated nag) and lands on Home
with full talking functionality; the same permissions stay reachable later from Settings. Steps
1–3 are not skippable in that sense — talking needs the key, so there is nothing to defer to.

## One-way doors

`data/OnboardingPrefs.kt` persists exactly two flags:

- `introSeen` — the wordless cards have been shown once.
- `activationDone` — the post-key funnel (activation → permissions → first task) has been walked
  once, however it ended. Once true, a returning user always lands straight on Home; the funnel
  never runs again. Finer-grained progress inside that funnel (which permission was granted) is
  intentionally not persisted — if the app is killed mid-funnel, it restarts from Activation next
  time, which is short and harmless. This is the "boring but consistent" tradeoff over building a
  resumable state machine for a one-time, few-second flow.

`OnboardingRouter` (in `ui/onboarding/`) is the only place that decides what plays next; every
screen it shows is a self-contained file that receives plain callbacks (`onGranted`, `onSkipped`,
…) and knows nothing about its neighbors.

## The key lives on this device only

`data/ApiKeyStore.kt` holds the user's own Gemini key in a private, app-scoped SharedPreferences
file — never bundled at build time (the old `local.properties` → `BuildConfig.GEMINI_API_KEY`
path is gone), never sent anywhere but straight to Google's API. `GeminiClient` now takes the key
as a supplier (`() -> String`) rather than a fixed value, so every long-lived engine (the agent
runner, Live's search) always uses whatever key is current, even if it was just replaced from
Settings. `data/ApiKeyValidator.kt` turns a pasted key into a plain yes/no with the smallest real
call that proves it works (`gemini-3.5-flash-lite`, 4 output tokens) — never a raw error code.

## Recovery, not silent failure

- **Key stops working** — `GeminiClient.ApiException.isAuthError` (401/403) and
  `LiveFail.isAuthError` both flow into `ApiKeyStore.markInvalid()`. The home screen shows a quiet
  line pointing at Settings; Settings' key section opens straight into edit mode.
- **Accessibility revoked** — unchanged from before onboarding existed: `BuddyBrain`'s
  `HANDS_OFF_NOTE` already says so calmly the next time an action needs it, instead of a raw
  system error.

## Composition

| File | Role |
|------|------|
| `ui/BuddyApp.kt` | Root router — onboarding funnel, Settings, or Home, and nothing else |
| `ui/onboarding/OnboardingStage.kt` | The stage enum, front door to Home |
| `ui/onboarding/OnboardingRouter.kt` | Owns funnel order and "yes"/"not now" transitions only |
| `ui/onboarding/OnboardingScaffold.kt` | Shared paper-canvas. IME shortens the column via `imePadding`; optional sticky action |
| `ui/onboarding/LaunchScreen.kt` | The orb, alone, breathing |
| `ui/onboarding/IntroScreen.kt` | Swipeable cards + dots + skip |
| `ui/onboarding/ApiKeyScreen.kt` | "Give buddy a brain" — field, live check, wake-up beat. Flex spacer so the field + Wake hug the keyboard |
| `ui/onboarding/ActivationScreen.kt` | Live hello (one-shot Fenrir), then the capability reveal |
| `ui/onboarding/OverlayPermissionScreen.kt` | Priming + the real cursor settling on screen |
| `ui/onboarding/AccessibilityPermissionScreen.kt` | Priming, honest about what it is and isn't |
| `ui/onboarding/FirstTaskScreen.kt` | Demo flight, then opens the calculator |
| `ui/components/ApiKeyField.kt` | Shared field — monospace, show/hide — used here and in Settings |
| `ui/components/PermissionGlyph.kt` | Custom line-art for the two priming screens |
| `ui/settings/SettingsScreen.kt` | Composition root — key, permissions, reset |
| `ui/settings/ApiKeySection.kt` | View masked / replace, same live check as first setup |
| `ui/settings/PermissionStatusSection.kt` | Plain on/off + one-tap re-grant |
| `ui/settings/ResetSection.kt` | Stop buddy; destructive full reset behind a confirm |
| `data/ApiKeyStore.kt` | The key, on-device only, plus the "stopped working" flag |
| `data/ApiKeyValidator.kt` | Turns a pasted key into a human yes/no |
| `data/OnboardingPrefs.kt` | `introSeen`, `activationDone`, and the chip-fade session counter |
| `data/ActivityLog.kt` | Last few real requests, for the home screen's recent-activity strip |

## Home screen additions

Still mostly white space (`docs/ui.md`), plus two small, problem-solving additions:

- **Prompt chips** (`ui/home/PromptChips.kt`) — a few tappable example asks, shown only once
  buddy can actually act (`canSeeScreen`) and only for a new user's first several home visits
  (`OnboardingPrefs.CHIP_SESSION_LIMIT`). Solves the blank-assistant-input freeze without a manual.
- **Recent activity** (`ui/home/RecentActivityStrip.kt`) — collapsed by default, a short trail of
  the last few real requests, tap to repeat. An intentional one-line empty state replaces a blank
  hole when there's nothing yet.
- **Settings entry** (`ui/home/HomeTopBar.kt`) — one small, quiet icon, nothing else added to the
  steady-state screen.
