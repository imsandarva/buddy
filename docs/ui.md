# Home UI

`MainActivity` only enables edge-to-edge drawing and hosts `BuddyApp` — the app's whole screen
surface. `BuddyApp` is a thin router across exactly three destinations: the one-time onboarding
funnel (`docs/onboarding.md`), Settings, and Home. The cursor is not part of any of these; it is a
system overlay (`docs/overlay.md`).

## Composition

| File | Role |
|------|------|
| `MainActivity.kt` | Light system bars + `setContent { BuddyApp() }` |
| `ui/BuddyApp.kt` | Root router — onboarding / Settings / Home, + the overlay/access resume hook |
| `ui/onboarding/` | First-run funnel — see `docs/onboarding.md` |
| `ui/settings/` | API key, permission status, reset — see `docs/onboarding.md` §Composition |
| `ui/home/HomeScreen.kt` | Assembles paper backdrop, hero, Start / Rest, quiet actions, chips, activity |
| `ui/home/HomeTopBar.kt` | The one small icon into Settings |
| `ui/home/PromptChips.kt` | First-run suggested asks — fades out after a few sessions |
| `ui/home/RecentActivityStrip.kt` | Collapsed trail of the last few real requests, tap to repeat |
| `ui/home/AskBuddySheet.kt` | Type-to-ask panel (typed REST path) |
| `ui/components/BuddyMark.kt` | In-app brand mark — same held/drag orb as the launcher, without the white plate |
| `ui/components/BuddyActionButton.kt` | Violet Start and quiet Rest |
| `ui/components/ApiKeyField.kt` | Shared key input — onboarding and Settings both use it |
| `ui/components/UnderlineField.kt` | Thin underline input — language search, no box |
| `ui/components/LanguagePickerList.kt` | Country list + underline search — onboarding and Settings |
| `ui/motion/PressScale.kt` | Shared press spring |
| `session/BuddySessionViewModel.kt` | Facade over hands, eyes, the Gemini brain, and onboarding/key state |
| `data/` | On-device key, onboarding flags, activity log — see `docs/onboarding.md` |
| `ui/theme/` | Color, type, motion, Material theme — light only |

## Actions

- **Start your buddy** — requests appear-on-top if needed, then starts the overlay service.
- **Stop buddy** — shown while the overlay is running; removes the cursor.
- **Watch it move** — flies the overlay along a short demo path (`BuddyCursorController.playDemo()`).
- **Let me see your screen** — opens Buddy Assistant while the overlay is running.
- **Ask buddy** — starts a live talk when the microphone is allowed. Anything longer than one step hands off to the agent runner on your screen. See `docs/live.md` and `docs/agent.md`.
- **Point at something** — snapshots the active screen and flies to one real control.
- **Prompt chips / recent activity** — both call the same typed door (`askWithText`) as the sheet.

See `docs/overlay.md`, `docs/cursor.md`, `docs/brand.md`, `docs/eyes.md`, `docs/hands.md`, `docs/type.md`, `docs/brain.md`, and `docs/onboarding.md`.

## Design tokens

Light only. Paper field (`#F7F6FB`), snow surfaces (`#FFFFFF`), ink (`#1C1730`), iris violet (`#635BFF`) for UI accents. The on-screen pointer is BuddyCursor — one glassy blue-violet being (`CursorFocus` `#5850FF`, `CursorMid` `#6C63F2`). The app logo is that being in its held/drag presence on white; see `docs/brand.md`. No honey, no dusk, no dark theme.

- Eyebrow: wide-tracked sans, violet
- Display: light serif, stacked two-line headlines
- Body: 17 / 27 sans, muted ink
- Motion: local only — ambient bloom, press scale, entrance, grab jiggle, hero crossfade, and the same soft no-overshoot crossfade between every onboarding/Settings/Home destination (`BuddyMotion.crossfade`)
