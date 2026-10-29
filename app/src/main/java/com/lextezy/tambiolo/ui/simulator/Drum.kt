package com.lextezy.tambiolo.ui.simulator

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.lextezy.tambiolo.ui.theme.BallInk
import com.lextezy.tambiolo.ui.theme.Tambiolo
import com.lextezy.tambiolo.ui.theme.ballColor
import com.lextezy.tambiolo.ui.theme.ballShadeColor
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Simple 2D physics for the tambiolo drum, in a virtual 600 x 640 space:
 * the drum is a circle (center 300,340 radius 270) with a chute at the top.
 */
class Ball(val n: Int, var x: Float, var y: Float, val r: Float) {
    var vx = 0f
    var vy = 0f
    var flying = false
    var scale = 1f
}

class DrumPhysics {
    val balls = mutableListOf<Ball>()
    /** 0 = balls rest at the bottom, 1 = full air jet mixing. */
    var mixing = 0f

    fun reset(max: Int) {
        balls.clear()
        val r = (520f / sqrt(max * 3.2f)).coerceIn(13f, 20f)
        for (n in 1..max) {
            val a = Random.nextFloat() * 6.2832f
            val d = Random.nextFloat() * (RADIUS - r - 20f)
            balls += Ball(n, CX + cos(a) * d, CY + sin(a) * d * 0.5f + 80f, r)
        }
    }

    fun step() {
        for (b in balls) {
            if (b.flying) continue
            b.vy += 0.35f // gravity
            if (mixing > 0f) {
                // Air blower: strong random lift only in the lower part of the drum,
                // so balls shoot up, arc, and fall back down instead of pinning to the top.
                val depth = (b.y - CY) / RADIUS // -1 = top, 1 = bottom
                if (depth > 0.35f) b.vy -= mixing * (1.2f + Random.nextFloat() * 1.6f)
                // Random jitter everywhere
                b.vx += (Random.nextFloat() * 2f - 1f) * 1.2f * mixing
                b.vy += (Random.nextFloat() * 2f - 1f) * 0.8f * mixing
                // Gentle swirl around the center
                val sx = b.x - CX; val sy = b.y - CY
                val sd = hypot(sx, sy).coerceAtLeast(1f)
                b.vx += -sy / sd * 0.25f * mixing
                b.vy += sx / sd * 0.25f * mixing
            }
            val speed = hypot(b.vx, b.vy)
            if (speed > MAX_SPEED) { b.vx *= MAX_SPEED / speed; b.vy *= MAX_SPEED / speed }
            b.vx *= 0.99f; b.vy *= 0.99f
            b.x += b.vx; b.y += b.vy
            val dx = b.x - CX; val dy = b.y - CY
            val d = hypot(dx, dy); val lim = RADIUS - b.r
            if (d > lim) {
                val nx = dx / d; val ny = dy / d
                b.x = CX + nx * lim; b.y = CY + ny * lim
                val dot = b.vx * nx + b.vy * ny
                if (dot > 0) { b.vx -= 1.8f * dot * nx; b.vy -= 1.8f * dot * ny }
            }
        }
        for (i in balls.indices) for (j in i + 1 until balls.size) {
            val a = balls[i]; val b = balls[j]
            if (a.flying || b.flying) continue
            val dx = b.x - a.x; val dy = b.y - a.y
            val d = hypot(dx, dy); val m = a.r + b.r
            if (d > 0f && d < m) {
                val nx = dx / d; val ny = dy / d; val o = (m - d) / 2f
                a.x -= nx * o; a.y -= ny * o; b.x += nx * o; b.y += ny * o
                val rv = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
                if (rv < 0) {
                    val k = -0.9f * rv
                    a.vx -= k * nx; a.vy -= k * ny; b.vx += k * nx; b.vy += k * ny
                }
            }
        }
    }

    companion object {
        const val W = 600f
        const val H = 640f
        const val CX = 300f
        const val CY = 340f
        const val RADIUS = 270f
        const val CHUTE_HALF = 26f
        const val MAX_SPEED = 16f
        /** Where a picked ball flies to (top of the chute). */
        const val EXIT_Y = 24f
    }
}

@Composable
fun DrumView(physics: DrumPhysics, modifier: Modifier = Modifier) {
    val c = Tambiolo.colors
    val frame = remember { mutableLongStateOf(0L) }
    LaunchedEffect(physics) {
        while (true) {
            withFrameNanos { t ->
                physics.step()
                frame.longValue = t
            }
        }
    }
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            color = BallInk.toArgb()
        }
    }
    Canvas(modifier.aspectRatio(DrumPhysics.W / DrumPhysics.H)) {
        frame.longValue // redraw every frame
        val s = size.width / DrumPhysics.W
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        val center = p(DrumPhysics.CX, DrumPhysics.CY)
        val radius = DrumPhysics.RADIUS * s

        // Chute
        val chuteTop = 0f
        val chuteBottom = (DrumPhysics.CY - DrumPhysics.RADIUS + 8f) * s
        val left = (DrumPhysics.CX - DrumPhysics.CHUTE_HALF) * s
        val right = (DrumPhysics.CX + DrumPhysics.CHUTE_HALF) * s
        drawRect(c.bg, topLeft = Offset(left, chuteTop), size = Size(right - left, chuteBottom))

        // Drum body
        drawCircle(
            brush = Brush.radialGradient(
                0f to c.drumCenter, 0.75f to lerp(c.drumCenter, c.drumEdgeFill, 0.5f), 1f to c.drumEdgeFill,
                center = p(DrumPhysics.CX, DrumPhysics.CY - 40f), radius = radius * 1.1f,
            ),
            radius = radius, center = center,
        )

        // Balls
        val sorted = physics.balls.sortedBy { if (it.flying) 1 else 0 }
        drawIntoCanvas { canvas ->
            for (b in sorted) {
                val br = b.r * s * b.scale
                val bc = p(b.x, b.y)
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to Color.White, 0.4f to ballColor(b.n), 1f to ballShadeColor(b.n),
                        center = Offset(bc.x - br * 0.35f, bc.y - br * 0.4f), radius = br * 1.6f,
                    ),
                    radius = br, center = bc,
                )
                paint.textSize = br * 0.95f
                canvas.nativeCanvas.drawText(b.n.toString(), bc.x, bc.y + br * 0.33f, paint)
            }
        }

        // Rim (with a gap for the chute) and chute walls
        val rim = Stroke(width = 6f * s)
        drawArc(
            color = c.drumRim, startAngle = -84f, sweepAngle = 348f, useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2), style = rim,
        )
        drawLine(c.drumRim, Offset(left, chuteTop), Offset(left, chuteBottom), strokeWidth = 6f * s)
        drawLine(c.drumRim, Offset(right, chuteTop), Offset(right, chuteBottom), strokeWidth = 6f * s)

        // Glass shine
        drawArc(
            color = Color.White.copy(alpha = if (c.isDark) 0.16f else 0.4f),
            startAngle = 200f, sweepAngle = 60f, useCenter = false,
            topLeft = p(DrumPhysics.CX - 215f, DrumPhysics.CY - 230f), size = Size(260f * s, 260f * s),
            style = Stroke(width = 10f * s, cap = StrokeCap.Round),
        )
    }
}
