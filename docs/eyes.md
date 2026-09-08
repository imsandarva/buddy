# Screen eyes

The AI does not walk the accessibility tree. It calls this API. The overlay cursor never reads the screen.

## API

`BuddyScreenEyes` is the process-wide string. The accessibility service attaches the live reader on connect and detaches on unbind.

| Call | Use |
|------|-----|
| `snapshot()` | What is on **the user's** screen right now — `ScreenSnapshot` |
| `scenes` | While Live is on, a fresh snapshot when they change apps, windows, or pages (drawer swipe) |
| `lastEventAt` | When the screen last reported a change — lets a run wait for quiet |
| `pointTo(id)` / `pointTo(node)` | Fly the tip to a node's center |
| `pointToGuide()` | Debug pick — one teachable control, then `pointTo` |
| `isReady()` | The service is bound |

`awaitReadableSnapshot()` retries while a new app's tree is still hollow. `awaitSettledSnapshot()` waits for the accessibility event stream to go quiet (≥260 ms, 240 ms of silence, ≤1.5 s) before reading — UI Automator's `waitForIdle`, event-driven, no extra walks while the screen is moving.

## What a snapshot holds

`ScreenSnapshot`: package, **app label** (“Settings”, not `com.android.settings`), **keyboard shown**, screen size, how many nodes were dropped past the cap, and the nodes.

`ScreenNode`: `id`, `label`, `bounds`, **`role`** (button, item, switch, checkbox, radio, tab, field, text, heading, image, slider, list, control), and state — `clickable`, `editable`, `scrollable`, `longClickable`, `checked` (null when not checkable), `selected`, `enabled`, `focused`, `password`, `range` (slider percent), `viewId`.

### Reading like a person

- A **tappable row is one node**: its child texts join the label as `title · detail` (“Wi‑Fi · Connected”). A tappable child inside it (a switch) is its own node, named after the row when it has no text of its own.
- **Standalone text** (headings, values like “45 GB free”) stays as `text` / `heading` nodes — that is how the model reads answers.
- **Scrollable containers** are `list` nodes with an id so `scroll` can name them. Text inside a list but outside a row is standalone.
- Unlabeled clickables keep the view id as their name; a clickable unlabeled sheet covering most of the screen is a scrim and is skipped.
- On-screen nodes (not only `isVisibleToUser` — Compose and some OEM overlays lie there).

### Which windows

- `getWindows` is z-order, top first. The **front covering app window** is the scene; **every readable window above it** — the app's own dialogs, a permission prompt from another package, a popup menu — is part of it. Windows beneath are hidden and dropped.
- **Buddy chrome** is skipped: accessibility overlays, the keyboard, our slim bars, and any Buddy window that hides its descendants (cursor, ask sheet). The **Buddy activity** is kept when it is in front.
- A **pulled-down notification / quick-settings shade** is what they see — we read that System UI window and skip the thin status/nav strips.
- The keyboard window is not read, but its presence sets `keyboardShown`.
- After a tap into another app the first tree is often hollow: uninterruptible prefetch, `refresh()` an empty root, retry until controls appear.

Ids prefer the short view id (`date_time_settings`), then a slug of the label. Duplicates get `_2`.

Live pushes SCREEN again when they leave Buddy, open the launcher, switch apps, or swipe to another page in the same app — the scene key includes labels because launcher pages reuse view ids.

## Composition

| File | Role |
|------|------|
| `accessibility/BuddyScreenEyes.kt` | Eyes API, last-event clock |
| `accessibility/ScreenSnapshot.kt` | Immutable `ScreenNode` / `NodeRole` / bounds |
| `accessibility/ScreenTreeWalker.kt` | Rows, roles, states, lists — Compose-safe visibility |
| `accessibility/WindowRootPicker.kt` | Front app + everything stacked above it; chrome skipped; keyboard flag |
| `accessibility/AccessibilityTreeReader.kt` | Bound to the system service; app labels |
| `accessibility/AccessibilityNodes.kt` | Uninterruptible prefetch, safe recycle (TalkBack / API 33+) |
| `accessibility/ScreenReady.kt` | `awaitReadableSnapshot`, `awaitSettledSnapshot` |
| `accessibility/ScreenSceneTracker.kt` | Debounced follow while Live is watching; retries an empty tree |
| `accessibility/GuidePicker.kt` | Chooses one control for the debug tap |
| `accessibility/AccessibilitySession.kt` | Bound / granted / awaiting |
| `accessibility/AccessibilityController.kt` | Reads Settings, opens Buddy Assistant |
| `accessibility/BuddyAccessibilityService.kt` | Composition — attach / detach eyes + hands + type + keys |
| `brain/agent/SceneDescriber.kt` | Snapshot → the SCREEN text the model reads |

## Prove it

1. Start the buddy, then tap **Let me see your screen** and turn on **Buddy Assistant**.
2. **Point at something** in the app flies to a control on this screen.
3. Open Settings and double-tap the cursor: “is Wi‑Fi on?” — the answer comes from the switch state, no tap.

See `docs/accessibility.md`, `docs/agent.md`, and `docs/cursor-hands.md`.
