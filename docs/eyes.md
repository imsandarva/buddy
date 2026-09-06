# Screen eyes

The AI does not walk the accessibility tree. It calls this API. The overlay cursor never reads the screen.

## API

`BuddyScreenEyes` is the process-wide string. The accessibility service attaches the live reader on connect and detaches on unbind.

| Call | Use |
|------|-----|
| `snapshot()` | Visible nodes on **the user’s** screen: `{ id, label, bounds, clickable, editable }` |
| `scenes` | While Live is on, a fresh snapshot when they change apps, windows, or pages (drawer swipe) |
| `pointTo(id)` | Fresh snapshot, then fly the tip to that node’s center |
| `pointTo(node)` | Fly to a node we already hold |
| `pointToGuide()` | Debug pick — one teachable control, then `pointTo` |

Pixels stay in the app. Gemini’s `point_to(element_id)` resolves through this API. `fly_to` does not use eyes. See `docs/brain.md`.

`pointTo` returns false if the overlay hands are not attached.

## Composition

| File | Role |
|------|------|
| `accessibility/BuddyScreenEyes.kt` | Eyes API |
| `accessibility/ScreenSnapshot.kt` | Immutable `ScreenNode` / bounds |
| `accessibility/AccessibilityNodes.kt` | Uninterruptible prefetch, safe recycle (TalkBack / API 33+) |
| `accessibility/ScreenReady.kt` | Wait until a new app’s tree has controls |
| `accessibility/ScreenTreeWalker.kt` | Walk on-screen controls (Compose-safe visibility) |
| `accessibility/AccessibilityTreeReader.kt` | Bound to the system service |
| `accessibility/WindowRootPicker.kt` | The front content window — Buddy chrome skipped, Buddy activity kept |
| `accessibility/ScreenSceneTracker.kt` | Debounced follow while Live is watching; retries an empty tree |
| `accessibility/GuidePicker.kt` | Chooses one control for the debug tap |
| `accessibility/AccessibilitySession.kt` | Bound / granted / awaiting |
| `accessibility/AccessibilityController.kt` | Reads Settings, opens Buddy Assistant |
| `accessibility/BuddyAccessibilityService.kt` | Composition — attach / detach eyes + hands + type |

## What is snapshotted

- The **topmost covering** app window (`getWindows` is z-order, top first). Focused/active lies while our overlay is up and used to hide other apps
- After a tap into another app the first tree is often hollow (`nodes=0` with the right package). TalkBack / Voice Access / UI Automator wait and prefetch: we take an **uninterruptible** descendant snapshot, `refresh()` an empty root, and **retry** until controls appear before sending SCREEN
- **Buddy chrome** is skipped (cursor, live pill, ask sheet). The **Buddy activity** is kept when it is the covering window on top — leftover launcher under Buddy is ignored, and leftover Buddy under Chrome is ignored too
- Live used to ignore window events from our package, so opening Buddy left the last app-drawer SCREEN frozen. Scene follow now watches those events too
- A **pulled-down notification / quick-settings shade** is what they see — we read that System UI window and skip the thin status/nav strips so the model is not blind there
- Overlay views also set `IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS` so we do not announce ourselves
- System UI is skipped
- On-screen nodes (not only `isVisibleToUser` — Compose and some OEM overlays lie there)
- Visible nodes with a label (text, description, hint, or a clickable view id). Clickable / checkable controls without a label still appear (view id or a short role name)
- Editable fields even when empty (listed as `type` so the model can fill them)
- Unlabeled full-screen wrappers are skipped; a labeled or tappable full-screen control is kept
- Same-label children inside a clickable parent are collapsed to the parent
- Typed ask: the panel is closed first so the catalog is not the ask field
- Live: SCREEN is pushed again when they leave Buddy, open the launcher, switch apps, **or swipe to another page** in the same app (app drawer). Window-only events miss that — TalkBack watches scroll and content, then we wait ~380 ms for the page to settle. The scene key includes labels, because launcher pages reuse the same view ids with different app names.

Ids prefer the short view id (`date_time_settings`), then a slug of the label. Duplicates get `_2`.

## Prove it

1. Start the buddy, then tap **Let me see your screen** and turn on **Buddy Assistant**.
2. **Point at something** in the app flies to a control on this screen.
3. Go to the home screen or app drawer. Double-tap the cursor and ask it to point at an app — it should fly to that icon.

See `docs/accessibility.md` and `docs/cursor-hands.md`.
