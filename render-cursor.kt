import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.hypot
import kotlin.math.min

/**
 * BuddyCursor preview renderer — tweak here, run `./gradlew renderCursor`, open buddycursor_icon.png.
 * When the design is right, copy the CURSOR_* constants and [arrowPath] into ui/cursor/Cursor.kt.
 */
private const val CURSOR_SIZE_DP = 28f
private const val CURSOR_PAD_DP = 16f
private const val CURSOR_STROKE_DP = 1.6f
private const val EXPORT_SCALE = 8 // px per dp — raise for a sharper PNG
private const val CORNER_RADIUS = 2f // in 24-unit space — slight rounding at joints
private const val FILL_BLUE = 0xFF4A9EFF.toInt()

fun main() {
    val scale = EXPORT_SCALE
    val glyph = CURSOR_SIZE_DP * scale
    val pad = CURSOR_PAD_DP * scale
    val stroke = CURSOR_STROKE_DP * scale
    val canvas = ((CURSOR_SIZE_DP + CURSOR_PAD_DP * 2) * scale).toInt()
    val image = BufferedImage(canvas, canvas, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    g.color = Color(0, 0, 0, 0)
    g.fillRect(0, 0, canvas, canvas)
    val path = arrowPath(glyph - stroke)
    val inset = stroke / 2f
    g.translate((pad + inset).toDouble(), (pad + inset).toDouble())
    g.color = Color(FILL_BLUE, true)
    g.fill(path)
    g.color = Color.BLACK
    g.stroke = BasicStroke(stroke, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)
    g.draw(path)
    g.dispose()
    val out = File("buddycursor_icon.png").absoluteFile
    ImageIO.write(image, "png", out)
    println("Wrote ${out.path} (${canvas}x${canvas})")
}

/** Arrow pointer — left shaft, sloped inner leg, horizontal shelf, right wing. Matches ui/cursor/Cursor.kt. */
private fun arrowPath(size: Float): Path2D.Float = Path2D.Float().apply {
    val u = size / 24f
    val notchX = 5.5f
    val notchY = 13.5f
    // Shelf ends where the outer diagonal (tip → wing) meets y = notchY — no extra wing tip vertex.
    val pts = arrayOf(
        floatArrayOf(0f, 0f),
        floatArrayOf(0f, 20f * u),
        floatArrayOf(notchX * u, notchY * u),
        floatArrayOf(notchY * u, notchY * u)
    )
    roundedPolygon(this, pts, CORNER_RADIUS * u, sharpCorners = setOf(0))
}

private fun roundedPolygon(path: Path2D.Float, pts: Array<FloatArray>, radius: Float, sharpCorners: Set<Int>) {
    val n = pts.size
    for (i in 0 until n) {
        val prev = pts[(i - 1 + n) % n]
        val curr = pts[i]
        val next = pts[(i + 1) % n]
        val inX = curr[0] - prev[0]; val inY = curr[1] - prev[1]
        val outX = next[0] - curr[0]; val outY = next[1] - curr[1]
        val inLen = hypot(inX, inY); val outLen = hypot(outX, outY)
        val trim = if (i in sharpCorners) 0f else min(radius, min(inLen * 0.42f, outLen * 0.42f))
        val entryX = curr[0] - inX / inLen * trim; val entryY = curr[1] - inY / inLen * trim
        val exitX = curr[0] + outX / outLen * trim; val exitY = curr[1] + outY / outLen * trim
        if (i == 0) path.moveTo(entryX, entryY) else path.lineTo(entryX, entryY)
        if (i in sharpCorners) path.lineTo(curr[0], curr[1]) else path.quadTo(curr[0], curr[1], exitX, exitY)
    }
    path.closePath()
}
