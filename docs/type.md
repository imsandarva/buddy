# Typing

The AI does not inject key events and does not become the keyboard. It calls this API. Filling a field uses `ACTION_SET_TEXT` — the same path Voice Access, Switch Access, and UI Automator use. On Android 13+ the accessibility IME (`FLAG_INPUT_METHOD_EDITOR`) can `commitText` if set-text fails. Paste is the last fallback. Enter is `ACTION_IME_ENTER`, not a typed newline.

A custom keyboard would make people switch IMEs. Hardware key injection needs a system signature. Tapping Gboard keys is slow and layout-fragile. None of those are the product path.

## API

`BuddyType` is the process-wide string. The accessibility service attaches the live writer on connect and detaches on unbind.

| Call | Use |
|------|-----|
| `typeHere(text, submit?)` | Fill the focused field, or the one nearest the tip |
| `typeAt(text, target, submit?)` | Fly to that field, tap to focus, then fill |
| `submitHere()` | Press Search / Send / Enter on the focused field |
| `isReady()` | Buddy Assistant is on |

`submit` is the IME action (search, send, go). The user’s Gboard stays put.

## Overlay

A named field gets a real tap first so the caret is in that box and the cursor is on it. Typing “here” skips the tap and writes into the focused field.

## Wiring now

- “Type hello” / “search for pizza” / “press enter” is on-device (`BuddyTypeIntent`). No network.
- “Type pizza in the search box” goes to Gemini (`type`), then `BuddyType` on **that** snapshot.
- Empty or unlabeled boxes still appear in the SCREEN list as `type` so the model can see them.

Needs **Let me see your screen** (Buddy Assistant). After install, toggle Assistant off and on once so `flagInputMethodEditor` is live.

See `docs/brain.md` and `docs/accessibility.md`.
