package com.lextezy.tambiolo.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lextezy.tambiolo.data.Draw
import com.lextezy.tambiolo.data.NumberStats
import com.lextezy.tambiolo.data.StatsPeriod
import com.lextezy.tambiolo.data.gameFor
import com.lextezy.tambiolo.data.numberStats
import com.lextezy.tambiolo.ui.components.FinePrint
import com.lextezy.tambiolo.ui.components.GameChips
import com.lextezy.tambiolo.ui.components.LottoBall
import com.lextezy.tambiolo.ui.components.Mono
import com.lextezy.tambiolo.ui.i18n.S
import com.lextezy.tambiolo.ui.theme.Tambiolo

/** Periods anyone can use. Everything else on this screen needs Tambiolo Plus. */
private val FREE_PERIODS = setOf(StatsPeriod.MONTH)

@Composable
fun StatsScreen(
    draws: List<Draw>,
    hasPlus: Boolean,
    plusPrice: String?,
    onGetPlus: () -> Unit,
    onTryInSimulator: (max: Int, numbers: List<Int>) -> Unit,
) {
    val c = Tambiolo.colors
    val s = S.str
    var max by rememberSaveable { mutableIntStateOf(58) }
    var period by rememberSaveable { mutableStateOf(StatsPeriod.MONTH) }
    val stats = remember(draws, max, period) { numberStats(draws, max, period) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(s.statsTitle, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
        Text(s.statsIntro, fontSize = 14.sp, color = c.muted, lineHeight = 20.sp)

        GameChips(selectedMax = max) { max = it }

        // Period chips
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatsPeriod.entries.forEach { p ->
                val on = p == period
                val locked = !hasPlus && p !in FREE_PERIODS
                Row(
                    Modifier.weight(1f).height(40.dp)
                        .background(if (on) c.selectedBg else c.card, RoundedCornerShape(20.dp))
                        .border(1.5.dp, if (on) c.selectedBg else c.line, RoundedCornerShape(20.dp))
                        .clickable { if (locked) onGetPlus() else period = p },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (locked) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = c.muted, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        s.periodLabel(p), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = if (on) c.selectedInk else if (locked) c.muted else c.ink, maxLines = 1,
                    )
                }
            }
        }

        Text(s.basedOnDraws(stats.drawCount, gameFor(max).code), fontSize = 13.sp, color = c.muted)

        if (stats.drawCount == 0) {
            Text(s.noStatsYet, fontSize = 14.sp, color = c.muted)
        } else if (hasPlus) {
            RankCard(s.mostDrawn, s.hotSub, stats.mostDrawn, stats, shown = 6)
            OutlinedButton(
                onClick = { onTryInSimulator(max, stats.mostDrawn.sorted()) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text(s.tryInSimulator, color = c.ink, fontWeight = FontWeight.SemiBold) }
            RankCard(s.leastDrawn, s.coldSub, stats.leastDrawn, stats, shown = 6)
            NumberChart(stats)
        } else {
            // Free: the top 3 hot numbers are shown, the other 3 are locked.
            RankCard(s.mostDrawn, s.hotSub, stats.mostDrawn, stats, shown = 3, footer = s.hotLocked, onLockedClick = onGetPlus)
            PlusCard(plusPrice, onGetPlus)
        }

        FinePrint(s.statsNote)
    }
}

/**
 * A row of six ranked numbers. Only the first [shown] are revealed;
 * the rest appear as locked balls that open the Plus purchase.
 */
@Composable
private fun RankCard(
    title: String,
    subtitle: String,
    numbers: List<Int>,
    stats: NumberStats,
    shown: Int,
    footer: String? = null,
    onLockedClick: () -> Unit = {},
) {
    val c = Tambiolo.colors
    val s = S.str
    Column(
        Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.ink)
            Text(subtitle, fontSize = 13.sp, color = c.muted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            numbers.forEachIndexed { i, n ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (i < shown) {
                        LottoBall(n, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text(s.timesShort(stats.counts[n]), fontSize = 12.sp, color = c.muted, textAlign = TextAlign.Center, maxLines = 1)
                    } else {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f)
                                .background(c.chip, CircleShape)
                                .border(1.5.dp, c.line, CircleShape)
                                .clickable(onClick = onLockedClick),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = s.plusName, tint = c.muted, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("?", fontSize = 12.sp, color = c.muted, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (footer != null) Text(footer, fontSize = 13.sp, color = c.muted)
    }
}

/** Every number with how often it came up; stronger color = drawn more often. */
@Composable
private fun NumberChart(stats: NumberStats) {
    val c = Tambiolo.colors
    val s = S.str
    val top = stats.topCount.coerceAtLeast(1)
    Column(
        Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(s.allNumbers, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.ink)
        Spacer(Modifier.height(4.dp))
        (1..stats.max).chunked(7).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { n ->
                    val share = stats.counts[n].toFloat() / top
                    Column(
                        Modifier.weight(1f).aspectRatio(0.9f)
                            .background(c.red.copy(alpha = 0.08f + 0.5f * share), RoundedCornerShape(10.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("$n", fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = c.ink)
                        Text("${stats.counts[n]}×", fontSize = 11.sp, color = c.ink)
                    }
                }
                repeat(7 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PlusCard(price: String?, onGetPlus: () -> Unit) {
    val c = Tambiolo.colors
    val s = S.str
    Column(
        Modifier.fillMaxWidth().background(c.navPill, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = c.navPillInk, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.plusName, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = c.navPillInk)
        }
        Text(s.plusPitch, fontSize = 14.sp, color = c.navPillInk, lineHeight = 20.sp)
        Button(
            onClick = onGetPlus,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.selectedBg, contentColor = c.selectedInk),
        ) { Text(s.getPlus(price), fontWeight = FontWeight.Bold) }
    }
}

/** Small bar-chart icon for the bottom bar. */
@Composable
fun StatsIcon(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(Modifier.size(22.dp)) {
        val u = size.width / 24f
        val w = 4f * u
        listOf(5f to 10f, 10.5f to 5f, 16f to 13f).forEach { (x, top) ->
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x * u, top * u),
                size = androidx.compose.ui.geometry.Size(w, (20f - top) * u),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * u),
            )
        }
    }
}
