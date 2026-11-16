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

@Composable
fun DrumView(physics: DrumPhysics, modifier: Modifier = Modifier) {
    val c = Tambiolo.colors
    val frame = remember { mutableLongStateOf(0L) }
    LaunchedEffect(physics) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                // Physics runs on real elapsed time, so the drum looks the same on 60, 90 and 120 Hz screens.
                val moved = if (last == 0L) true else physics.advance((t - last) / 1_000_000_000f)
                last = t
                if (moved) frame.longValue = t // redraw only while something is moving
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
