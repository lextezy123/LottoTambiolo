package com.lextezy.tambiolo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.lextezy.tambiolo.Config
import com.lextezy.tambiolo.data.GAMES
import com.lextezy.tambiolo.ui.theme.BallInk
import com.lextezy.tambiolo.ui.theme.Tambiolo
import com.lextezy.tambiolo.ui.theme.ballColor
import com.lextezy.tambiolo.ui.theme.ballShadeColor

val Mono = FontFamily.Monospace

/** A glossy lotto ball. Size comes from [modifier]; the number scales to fit. */
@Composable
fun LottoBall(n: Int, modifier: Modifier = Modifier, ringColor: Color? = null) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .then(if (ringColor != null) Modifier.border(3.dp, ringColor, CircleShape).padding(5.dp) else Modifier)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to Color.White, 0.4f to ballColor(n), 1f to ballShadeColor(n),
                        center = Offset(size.width * 0.32f, size.height * 0.28f),
                        radius = size.width * 0.8f,
                    )
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val fontSp = with(LocalDensity.current) { (maxWidth * 0.4f).toSp() }
        Text(
            text = n.toString(),
            color = BallInk,
            fontFamily = Mono,
            fontWeight = FontWeight.Bold,
            fontSize = fontSp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Six balls sharing a row evenly, capped at [maxBall] each. */
@Composable
fun BallRow(numbers: List<Int>, maxBall: Dp = 48.dp, highlight: Set<Int> = emptySet()) {
    val c = Tambiolo.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        numbers.forEach { n ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                LottoBall(n, Modifier.widthIn(max = maxBall).fillMaxWidth(), if (n in highlight) c.match else null)
            }
        }
    }
}

/** Pill chip used for game codes like "6/55". */
@Composable
fun CodeChip(text: String) {
    val c = Tambiolo.colors
    Text(
        text = text,
        fontFamily = Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = c.ink,
        modifier = Modifier.background(c.chip, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Row of 6/42 … 6/58 toggles. */
@Composable
fun GameChips(selectedMax: Int, enabled: Boolean = true, onSelect: (Int) -> Unit) {
    val c = Tambiolo.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        GAMES.sortedBy { it.max }.forEach { g ->
            val on = g.max == selectedMax
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (on) c.selectedBg else c.card, RoundedCornerShape(22.dp))
                    .border(1.5.dp, if (on) c.selectedBg else c.line, RoundedCornerShape(22.dp))
                    .clickable(enabled = enabled) { onSelect(g.max) },
                contentAlignment = Alignment.Center,
            ) {
                Text(g.code, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (on) c.selectedInk else c.ink)
            }
        }
    }
}

/** Number picker grid, 7 per row. */
@Composable
fun NumberGrid(max: Int, picks: Set<Int>, enabled: Boolean = true, onToggle: (Int) -> Unit) {
    val c = Tambiolo.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..max).chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { n ->
                    val on = n in picks
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(if (on) c.selectedBg else c.card, CircleShape)
                            .border(1.5.dp, if (on) c.selectedBg else c.line, CircleShape)
                            .clickable(enabled = enabled) { onToggle(n) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            n.toString(),
                            fontFamily = Mono,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                            color = if (on) c.selectedInk else c.ink,
                        )
                    }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** The ring-with-three-balls logo. */
@Composable
fun DrumLogo(size: Dp = 34.dp) {
    val ink = Tambiolo.colors.ink
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 34f
        drawCircle(ink, radius = 15f * s, style = Stroke(width = 2.5f * s))
        drawCircle(Color(0xFFFFC23D), radius = 4.5f * s, center = Offset(12f * s, 20f * s))
        drawCircle(Color(0xFFF2545B), radius = 4.5f * s, center = Offset(21f * s, 21f * s))
        drawCircle(Color(0xFF4FB3E8), radius = 4.5f * s, center = Offset(17f * s, 12.5f * s))
    }
}

/** Outline drum icon for the bottom bar. */
@Composable
fun DrumIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val s = size.width / 24f
        val stroke = Stroke(width = 2f * s)
        drawCircle(color, radius = 9f * s, style = stroke)
        drawCircle(color, radius = 2f * s, center = Offset(9f * s, 14f * s), style = stroke)
        drawCircle(color, radius = 2f * s, center = Offset(15f * s, 14f * s), style = stroke)
        drawCircle(color, radius = 2f * s, center = Offset(12f * s, 9f * s), style = stroke)
    }
}

/** Standard banner ad. Hidden entirely once the user buys "remove ads". */
@Composable
fun BannerAd() {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = Config.BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}

@Composable
fun FinePrint(text: String) {
    Text(text, fontSize = 12.sp, color = Tambiolo.colors.muted, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 4.dp))
}
