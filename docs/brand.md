# Brand

The app logo is BuddyCursor in its **held/drag** presence — Action form, full size, full brightness — sitting on white. It is not Idle (dimmed, smaller), and it is not Voice (the larger orb when Ask buddy or live talk is on).

Idle recedes (`scale 0.82`, `alpha 0.5`, glow `0.1`). Holding and dragging the point brings it to full presence (`scale 1`, `alpha 1`, glow `0.45`). That held body, scaled to sit well on a square plate, is the mark.

## Why this mood

The floating cursor is the product. The logo should be the same being people already know from the screen — caught in the one state that reads as *awake and in the hand*, without borrowing live-mode's mind-orb. A still frame cannot play the drag ribbon, so the glyph is the body only: glow, contact shadow, Action-form fill, and a small glass catch. The live cursor's white contrast rim stays on the overlay (so it reads on dark apps) and is left off the logo — on a white plate that ring only looks like an outline.

## Sizing

On device the Action point is ~24dp. On the logo it is enlarged so it occupies the adaptive-icon safe zone without filling it — a 56dp orb in the 108dp canvas (inside the 66dp never-clipped disc), with the bloom kept inside the circular mask. White is the plate, not a tight crop around the orb.

## What ships

| Asset | Role |
|-------|------|
| `res/mipmap-anydpi-v26/ic_launcher.xml` (+ `_round`) | Adaptive launcher icon — white background, held orb foreground, monochrome disc for themed icons |
| `res/drawable/ic_launcher_foreground.xml` | The orb (vector, same tokens as `BuddyColors` cursor swatches) |
| `res/drawable/ic_buddy_logo.xml` | Full lockup (white + orb) |
| `res/drawable/ic_buddy_status.xml` | Notification silhouette — white disc, system-tinted |
| `res/mipmap-*/ic_launcher.png` | Density rasters for previews and odd launchers |
| `docs/brand/logo.png` | Master lockup (1024) |
| `docs/brand/play_icon_512.png` | Play Store / listing icon |

`BuddyMark` is the same held body without the white plate, used on Home, launch, and onboarding. Live drawing lives in `ui/cursor/CursorMaterial.kt` so the mark and the overlay cannot drift apart.

See `docs/cursor.md` and `docs/ui.md`.
