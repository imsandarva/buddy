You’re aiming at the right problem. The important split is this: **the AI should not move the cursor. The app should. The AI should only say where.**

That’s how [Clicky](https://github.com/farzaa/clicky) works, how [Claude Computer Use](https://platform.claude.com/docs/en/agents-and-tools/tool-use/computer-use-tool.md) works, and how [Gemini Computer Use](https://ai.google.dev/gemini-api/docs/computer-use) works. Eyes, brain, and hands stay separate.

## The forest

Buddy already has a hand: the overlay window. What’s missing is a **single move command** anything can call:

- `animateTo(xPx, yPx)` — screen pixels, tip lands there  
- `animateToNormalized(x, y)` — `0…1` or Gemini’s `0…999` grid, then we convert  

Finger-drag already updates that window. AI later just becomes another caller of the same function. Don’t wait for Gemini Live to design this. If we can’t move it from a test button in code, the model won’t be able to either.

Three jobs, three modules:

| Layer | Job | Buddy today |
|---|---|---|
| Eyes | What’s on screen? | Not built (accessibility is stubbed) |
| Brain | Where should we point, and what should we say? | Not built |
| Hands | Fly the cursor there | Overlay exists; **no programmatic move API yet** |

Live Mode, ChatGPT, MCP, `[POINT:…]` tags — those are only Brain adapters. The Hands stay the same.

## How Clicky does it (Mac)

From [farzaa/clicky](https://github.com/farzaa/clicky): screenshot + voice go to Claude. The prompt tells Claude to append a tag in the **spoken text**:

```text
click that and you'll get the color wheels. [POINT:1100,42:color inspector]
```

The app regex-parses `[POINT:x,y:label]`, strips it before TTS, maps screenshot pixels → screen pixels, then the overlay **flies** there on a curve.

That’s clever for a weekend demo. It is **not** the best protocol:

- Models forget the format, put it mid-sentence, or invent coordinates  
- Speech and control are jammed into one string  
- You have to sanitize TTS so the user never hears “point one thousand one hundred”  
- Clicky even needs `[POINT:none]` as a special case  

They used tags because they wanted pointing mixed into one streaming paragraph. We don’t have that constraint.

## What the big APIs actually do

Industry practice is **tool calls**, not “write `moveBuddyCursor(x,y)` in the reply.”

- **Claude:** `mouse_move` / `left_click` as JSON `tool_use` blocks. Your app executes them.  
- **Gemini:** `function_call` like `click` with **`x,y` on a 0–999 grid**, plus `ENVIRONMENT_MOBILE` for Android. You scale to the real screen. Google’s [Android computer-use quickstart](https://github.com/google-gemini/gemini-android-computer-use-quickstart) does exactly that.  
- **MCP:** same idea, different socket. Useful if a desktop agent drives the phone. **Not needed** between Gemini’s API and this app.

So: `movebuddycursor(x,y)` in the chat text is the workaround. Native function calling is the product version.

Also: **never let the model speak pixel numbers to the user.** Two outputs:

1. `say` — warm words: “Tap Date & time, right here.”  
2. `point` — structured: where the cursor goes  

## Precision on Android (this is the real win)

Raw “AI looks at a screenshot and guesses pixels” is okay. For **Settings → Date & time**, it’s the weaker path.

Android already has a map of the screen: the **accessibility tree**. Each control has a label and `getBoundsInScreen()`. Operon-style Android agents send **screenshot + UI tree**, then act on a node, not a guess.

For Buddy’s teaching use case, prefer this:

1. Snapshot visible nodes: `{ id, text, bounds }`  
2. Give the model that list (plus a screenshot if useful)  
3. Tool: `point_to(element_id = "date_and_time")`  
4. **We** move the cursor to the **center of that rect**

That’s more precise than “look at the photo and pick x=812.” Pixels stay as a fallback when a screen has no useful tree (games, custom drawing).

Normalized coordinates still matter as the wire format (Gemini’s 0–999, or 0–1). Screenshot pixels ≠ phone pixels (status bar, density, crop). One mapper. Clicky had to do this too.

## Live Mode

Gemini/ChatGPT Live is **eyes + voice**. It does not replace the Hands.

When we add it: same `point_to` / `say` tools. If a live session can’t do tools well, *then* parse a tag as a fallback — don’t start there.

Live also does not require the model to drive the system mouse. BuddyCursor is a **virtual** pointer. The real finger stays with the user. That’s the teacher product, not a full computer-use agent. Clicking *for* them is a later accessibility gesture.

## What I would do

**Best path for this app**

1. **Now (no AI):** `BuddyOverlayWindow.animateTo(x, y)` — spring/arc, same jiggle-on-arrive if we want. Prove it with a hidden debug target.  
2. **Next:** accessibility snapshot → list of on-screen things.  
3. **Then:** Gemini (or Claude) with tools `say` + `point_to(element_id)` and fallback `point_to_normalized(x, y)`.  
4. **Later:** Live audio uses those same tools.  
5. **Later still:** `tap` via accessibility on that same node.

**Don’t** make the model write `moveBuddyCursor(120, 400)` in the sentence.  
**Don’t** wait on MCP.  
**Don’t** couple “which AI” to “how the window moves.”

The forest sentence: **BuddyCursor is a puppet. Build a clean string. Then any brain — Gemini Live, ChatGPT, a test button — can pull it.**

If you want a next step in code, the smallest one is that programmatic `animateTo` — still no API keys, but the AI will have somewhere to plug in.