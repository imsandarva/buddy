# Screen eyes

The AI does not walk the accessibility tree. It calls this API. The overlay cursor never reads the screen.

## API

`BuddyScreenEyes` is the process-wide string. The accessibility service attaches the live reader on connect and detaches on unbind.

| Call | Use |
|------|-----|
| `snapshot()` | Visible nodes: `{ id, label, bounds, clickable }` |
| `pointTo(id)` | Fresh snapshot, then fly the tip to that node’s center |
| `pointTo(node)` | Fly to a node we already hold |
| `pointToGuide()` | Debug pick — one teachable control, then `pointTo` |

Pixels stay in the app. Gemini’s `point_to(element_id)` resolves through this API. See `docs/brain.md`.

## Composition

| File | Role |
|------|------|
| `accessibility/BuddyScreenEyes.kt` | Eyes API |
| `accessibility/ScreenSnapshot.kt` | Immutable `ScreenNode` / bounds |
| `accessibility/ScreenTreeWalker.kt` | Walk + recycle live nodes |
| `accessibility/AccessibilityTreeReader.kt` | Bound to the system service |
| `accessibility/GuidePicker.kt` | Chooses one control for the debug tap |
| `accessibility/AccessibilitySession.kt` | Bound / granted / awaiting |
| `accessibility/AccessibilityController.kt` | Reads Settings, opens Buddy Assistant |
| `accessibility/BuddyAccessibilityService.kt` | Composition — attach / detach only |

## What is snapshotted

- Application windows only, active first; System UI is skipped
- Visible nodes with a label (text, description, hint, or a clickable view id)
- The Buddy cursor label is skipped
- Full-screen chrome is skipped
- Same-label children inside a clickable parent are collapsed to the parent

Ids prefer the short view id (`date_time_settings`), then a slug of the label. Duplicates get `_2`.

## Prove it

1. Start the buddy, then tap **Let me see your screen** and turn on **Buddy Assistant**.
2. **Point at something** in the app flies to a control on this screen.
3. Open Settings (leave Buddy). In the notification, tap **Point at something** — after the shade closes, the cursor flies to a row such as Date & time.

See `docs/accessibility.md` and `docs/cursor-hands.md`.
