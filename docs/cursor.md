# BuddyCursor

Buddycursor is not a mouse pointer — it is a physical stand-in for buddy's hand and mind,
floating on the user's real screen. It is one glassy blue-violet being that reshapes itself
continuously between two forms, so its *shape* is the signal for what buddy is doing, with zero
text. The full product-design rationale lives in the design discussion this implements; this file
documents the code that carries it out.

## One entity, two forms

| Form | When | Reads as |
|------|------|----------|
| **Voice** (orb, ~60dp) | Listening, Thinking | buddy "as a mind" — round, ambient, breathing |
| **Action** (point, ~24dp) | Idle, Considering, Traveling, Targeting, Acting, Holding, Dragging, Paused | buddy "as a hand" — small, precise, focused |

Both forms are the *same* material — a shared blue-violet radial gradient, denser and brighter at
the core in Action form, more diffuse and ambient in Voice form — so switching between them is a
continuous reshape (size, density, and a brief liquid squeeze mid-transition), never a cut and
never a cross-fade between two separate assets.

## Composition

| File | Role |
|------|------|
| `ui/cursor/CursorMood.kt` | The sealed `CursorMood` states + `CursorForm` (Voice/Action) + `CursorGestureKind` |
| `overlay/CursorMoodSignals.kt` | Raw facts (gesture in flight, traveling, targeting, user-drag, considering, paused, voice amplitude, velocity, uncertain pulses) — one flow per fact, written by whoever owns that fact |
| `overlay/CursorMoodResolver.kt` | The *only* place priority between overlapping signals is decided; turns the raw facts into the single `CursorMood` to draw |
| `ui/cursor/Cursor.kt` | Composition root — sizes/bounds, and `BuddyCursor()`, which wires every animated number (morph, breathe, sheen, ripple, hold-ring, wobble) and draws the shared material + contrast shadow/rim |
| `ui/cursor/CursorMaterial.kt` | The one glassy body — glow, shadow, radial fill, rim. The launcher logo is this body at **Dragging** presence on white; see `docs/brand.md` |
| `ui/cursor/VoiceForm.kt` / `ActionForm.kt` | Pure, stateless draw functions for each form's extra cues — take plain numbers, draw one cue each |
| `ui/cursor/CursorEffects.kt` | Lowest-level Canvas primitives (glow, motion trail, contact ripple, hold ring, amplitude rings, sheen sweep, wobble) shared by both forms |
| `ui/cursor/BuddyCursorHandle.kt` | Touch target — tap, double-tap, hold, drag |
| `ui/cursor/CursorGestures.kt` | Gesture recognizer |
| `overlay/BuddyOverlayWindow.kt` | Owns the *physical* truths only — flight/drag lifecycle, velocity, the dismiss-zone check — never decides a mood itself |
| `overlay/DismissZone.kt` | Circle hit-test + gentle magnet toward the X (56dp rest, 68dp armed — chat-head scale) |
| `overlay/DismissTargetWindow.kt` | Bottom overlay for the X; not-touchable so the cursor keeps the drag |
| `ui/cursor/DismissTarget.kt` | Drawn X — ink glass at rest, muted rose when armed |
| `overlay/BuddyCursorController.kt` | Flight API — programmatic move |
| `overlay/CursorLanding.kt` | Named spots for `fly_to` |

## The state machine

Exactly one `CursorMood` is true at a time; `CursorMoodResolver` decides the winner when several
raw signals overlap (highest priority first):

1. **Dragging** — the person is repositioning buddycursor, or buddy's own hand is mid swipe/scroll/drag. A trailing ribbon follows the point; a compact circular X rises at the bottom, and dragging onto it arms a dismiss (release there and Buddy stops — the same idea as a Messenger chat-head).
2. **Paused** — a single tap just interrupted a busy buddy. Steady, unmoving, dimmed glow for a deliberate beat before it settles back to idle.
3. **Holding** — a long-press is in flight. A ring fills clockwise so the wait has a visible end.
4. **Acting** — the exact moment of a tap. A quick squash and an outward ripple, paired with one light haptic tick.
5. **Targeting** — landed, brightening, holding a ~180ms anticipation beat before any stroke fires. This is the "you're about to see it tap something" cue.
6. **Traveling** — gliding between two points with a trailing comet of light, eased, never a teleport.
7. **Listening** — Voice form, breathing, glow reacting to real mic/speaker loudness (never a canned loop).
8. **Thinking** — Voice form, a slow sheen turning inside the orb like light in a glass marble — composing a reply.
9. **Considering** — Action form's version of the same sheen, at hand scale — the agent silently deciding its next on-screen move.
10. **Idle** — dimmed, slightly smaller, recedes into peripheral vision.

The **app logo** is Idle's opposite on the Action-form body: **Dragging** presence (full scale, full opacity, held glow) photographed on white — not Voice/live, not Ask buddy. See `docs/brand.md`.

Separately, an **uncertain** wobble (`CursorMoodSignals.pulseUncertain()`) can play on top of any
mood — buddy couldn't find the control it was looking for, so it says so honestly instead of
freezing silently, then moves on. Called from `AgentExecutor.missing()`.

## Gesture vocabulary — exactly three

| Gesture | Effect |
|---------|--------|
| **Single tap** | Always a light haptic tick. If buddy is busy (`BuddyBrain.interrupt()`, gated on `BrainPhase != Idle`), it also stops whatever it's doing — the safety-critical control lives right where the person is already looking, not buried in a sheet. |
| **Double-tap** | Talk to buddy — unchanged. |
| **Long-press + drag** | Reposition. A compact X appears at the bottom; dragging onto it and releasing stops Buddy entirely. |

No fourth gesture. Every extra one is something the user has to remember and Buddy has to teach.

## Geometry

The touch target (`Cursor.touchWidth/Height`, 116dp) is fixed and never resizes as buddy morphs —
only the drawn content inside it scales — so the `WindowManager` window never relayouts
mid-animation. The contact point is always this box's exact center in both forms; there is no
arrow, so there is no asymmetric tip offset to track.

## Contrast on arbitrary host apps

Buddyapp's own screens are light-only, but buddycursor floats over any app, light or dark. Its own
visibility never depends on what's underneath: a soft neutral drop shadow plus a thin light outer
rim are drawn on every frame, in every mood — buddycursor's own blue-violet identity never switches
to a "dark mode."

## Interaction

1. Tap **Start your buddy** → grant appear-on-top if asked → cursor appears, dimmed, at rest.
2. **Single tap** — a light haptic; interrupts if buddy is mid-task.
3. **Double-tap** — live talk starts (or the type sheet if the mic is off); the orb takes over. See `docs/live.md`.
4. **Press and hold** (~110 ms) — then the cursor follows your finger; a compact X rises at the bottom. Drop Buddy onto it to stop.
5. Tap **Watch it move** → the cursor flies a short path (same API AI will call later).
6. Tap **Stop buddy** (or the notification action) → the overlay is removed.

See `docs/cursor-hands.md`, `docs/hands.md`, and `docs/type.md`.
