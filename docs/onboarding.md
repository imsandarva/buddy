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
4. **Language** — "How should I talk?" United States / English is already chosen: it sits in a
   Suggested row at the top, already checked, and the violet **Talk in English** button names it.
   No copy that says “the default is English” — the check and the button *are* the default. A tap
   moves the check; Continue keeps whatever is selected. Search hides Suggested so the alphabet can
   take the page. Same list later from Settings, without the Suggested pin.
5. **Activation** — buddy talks first with Gemini Live's own voice (`Orion`) — one short hello,
   then the socket hangs up. No Android TTS, no lingering live talk, no mic. Then it reveals the
   second ability in the same breath: it can act on the screen too.
6. **Overlay permission** — the "easy yes." One headline, one why, two taps (Allow it → Appear on
   top for Buddy), then the real overlay cursor settles onto the screen right there.
7. **Accessibility permission** — the highest-stakes screen in the app. The why is one breath:
   three concrete acts (tap, type, change settings), why the screen must be seen, and “only when
   you ask” — not a list of examples, not a vague “I can see your screen.” Then a short how-to
   (Installed apps → Buddy Assistant → switch) and a plain line for what it never does, before
   Android's own generic dialog appears. Granting it ends the funnel — buddy does not open an app
   or run a surprise first task.
8. **Home** — steady state.

Any "not now" at steps 5–7 ends the funnel immediately (never a repeated nag) and lands on Home
with full talking functionality; the same permissions stay reachable later from Settings. Steps
1–4 are not skippable in that sense — talking needs the key and a language, so there is nothing to defer to.

## One-way doors

`data/OnboardingPrefs.kt` persists exactly two flags:

- `introSeen` — the wordless cards have been shown once.
- `activationDone` — the post-key funnel (activation → permissions) has been walked
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
path is gone), never sent anywhere but straight to Google's API. For a short check, `data/DevApiKeySeed.kt`
prefills the onboarding and Settings fields only — it does not save into the store; tap **Wake buddy up**
(or Save) as usual. Delete that file and the two `remember` initializers before anyone else has to type
their own key. `GeminiClient` now takes the key
as a supplier (`() -> String`) rather than a fixed value, so every long-lived engine (the agent
runner, Live's search) always uses whatever key is current, even if it was just replaced from
Settings. `data/ApiKeyValidator.kt` turns a pasted key into a plain yes/no with the smallest real
call that proves it works (`gemini-3.5-flash-lite`, 4 output tokens) — never a raw error code.

## Recovery, not silent failure

- **Key stops working** — `GeminiClient.ApiException.isAuthError` (401/403) flags
  `ApiKeyStore.markInvalid()`. A Live close that *says* "leaked" does **not** — Google's Live
  websocket has been returning that line for keys that still work on REST `generateContent`.
  Live confirms on REST first; if REST still accepts the key, typed ask stays up and the sheet
  says live talk is unavailable. Only a real REST reject opens Settings.
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
| `ui/onboarding/LanguageScreen.kt` | "How should I talk?" — US English pre-checked in Suggested, Continue confirms |
| `ui/onboarding/ActivationScreen.kt` | Live hello (one-shot Orion), then the capability reveal |
| `ui/onboarding/OverlayPermissionScreen.kt` | Overlay ask only — copy, grant, skip; layout is `PermissionPrimer` |
| `ui/onboarding/AccessibilityPermissionScreen.kt` | Accessibility ask only — copy, grant, skip; same primer |
| `ui/onboarding/PermissionPrimer.kt` | Shared paper: glyph, headline, how-to, action. Centers when short, scrolls when tall |
| `ui/onboarding/PermissionHowTo.kt` | Why + numbered taps + note, in one column (never loose children into FadeSlideIn's Box) |
| `ui/components/ApiKeyField.kt` | Shared field — monospace, show/hide — used here and in Settings |
| `ui/components/UnderlineField.kt` | Thin underline to type on — no box; language search uses this |
| `ui/components/LanguagePickerList.kt` | Shared country list + underline search; first-run pins [CountryData.DEFAULT] |
| `ui/components/CountryLanguageRow.kt` | One country: flag, name, language, quiet check — no washed box |
| `ui/components/PermissionGlyph.kt` | Custom line-art for the two priming screens |
| `ui/settings/SettingsScreen.kt` | Composition root — key, permissions, reset |
| `ui/settings/LanguageSection.kt` | Current language, opens the same picker |
| `ui/settings/LanguagePickerScreen.kt` | Settings path into the shared country list |
| `ui/settings/ApiKeySection.kt` | View masked / replace, same live check as first setup |
| `ui/settings/PermissionStatusSection.kt` | Plain on/off + one-tap re-grant |
| `ui/settings/ResetSection.kt` | Stop buddy; destructive full reset behind a confirm |
| `data/ApiKeyStore.kt` | The key, on-device only, plus the "stopped working" flag |
| `data/DevApiKeySeed.kt` | Temporary field prefill only — remove before real first-run |
| `data/ApiKeyValidator.kt` | Turns a pasted key into a human yes/no |
| `data/CountryData.kt` | Countries + languages; `DEFAULT` is United States / English |
| `data/LanguagePrefs.kt` | The country they picked, on-device |
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
