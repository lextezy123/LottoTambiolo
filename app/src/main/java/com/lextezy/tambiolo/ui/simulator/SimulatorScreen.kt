package com.lextezy.tambiolo.ui.simulator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lextezy.tambiolo.data.Combo
import com.lextezy.tambiolo.data.combinations
import com.lextezy.tambiolo.data.gameFor
import com.lextezy.tambiolo.ui.components.FinePrint
import com.lextezy.tambiolo.ui.components.GameChips
import com.lextezy.tambiolo.ui.components.LottoBall
import com.lextezy.tambiolo.ui.components.NumberGrid
import com.lextezy.tambiolo.ui.i18n.S
import com.lextezy.tambiolo.ui.theme.Tambiolo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

private enum class Phase { IDLE, MIXING, DRAWING, DONE }

private class Stats(val max: Int, val tally: IntArray, val runs: Int)

private val random = SecureRandom()

private fun quickPick(max: Int): List<Int> {
    val pool = (1..max).toMutableList()
    return List(6) { pool.removeAt(random.nextInt(pool.size)) }.sorted()
}

private fun simulate(max: Int, picks: Set<Int>, runs: Int): Stats {
    val tally = IntArray(7)
    val pool = IntArray(max)
    repeat(runs) {
        for (i in 0 until max) pool[i] = i + 1
        var hits = 0
        for (k in 0 until 6) {          // partial Fisher-Yates: first 6 slots become the draw
            val j = k + random.nextInt(max - k)
            val t = pool[k]; pool[k] = pool[j]; pool[j] = t
            if (pool[k] in picks) hits++
        }
        tally[hits]++
    }
    return Stats(max, tally, runs)
}

@Composable
fun SimulatorScreen(
    pending: Combo?,
    onPendingUsed: () -> Unit,
    showTip: Boolean,
    onTipSeen: () -> Unit,
) {
    val c = Tambiolo.colors
    val s = S.str
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    var max by rememberSaveable { mutableIntStateOf(42) }
    var picks by remember { mutableStateOf(listOf(3, 11, 19, 24, 33, 40)) }
    val drawn = remember { mutableStateListOf<Int>() }
    var phase by remember { mutableStateOf(Phase.IDLE) }
    var stats by remember { mutableStateOf<Stats?>(null) }
    var statsTop by remember { mutableIntStateOf(0) }
    var showRunInfo by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val physics = remember { DrumPhysics().apply { reset(42) } }

    fun resetDrum(newMax: Int) {
        job?.cancel()
        drawn.clear()
        phase = Phase.IDLE
        physics.mixing = 0f
        physics.reset(newMax)
    }

    LaunchedEffect(pending) {
        val combo = pending ?: return@LaunchedEffect
        max = combo.max
        picks = combo.numbers.sorted()
        stats = null
        resetDrum(combo.max)
        onPendingUsed()
    }

    // After "Run 1,000 draws", scroll so the results card is in view.
    LaunchedEffect(stats) {
        if (stats == null) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        scroll.animateScrollTo((statsTop - 200).coerceAtLeast(0))
    }

    val busy = phase == Phase.MIXING || phase == Phase.DRAWING
    val pickSet = picks.toSet()
    val hits = drawn.count { it in pickSet }

    fun startDraw() {
        if (showTip) onTipSeen()
        stats = null
        resetDrum(max)
        job = scope.launch {
            phase = Phase.MIXING
            physics.mixing = 1f
            delay(1800)
            phase = Phase.DRAWING
            repeat(6) { k ->
                val inDrum = physics.balls.filter { !it.flying }
                val ball = inDrum[random.nextInt(inDrum.size)]
                ball.flying = true
                val sx = ball.x; val sy = ball.y
                val start = withFrameNanos { it }
                var p = 0f
                while (p < 1f) {
                    withFrameNanos { now ->
                        p = ((now - start) / 1_000_000f / 700f).coerceAtMost(1f)
                        val e = p * p * (3 - 2 * p)
                        ball.x = sx + (DrumPhysics.CX - sx) * e
                        ball.y = sy + (DrumPhysics.EXIT_Y - sy) * e
                        ball.scale = 1f + 0.5f * sin(p * PI).toFloat()
                    }
                }
                physics.balls.remove(ball)
                drawn.add(ball.n)
                if (k < 5) delay(900)
            }
            physics.mixing = 0f
            phase = Phase.DONE
        }
    }

    fun skipToResult() {
        job?.cancel()
        val remaining = (1..max).filter { it !in drawn }.toMutableList()
        while (drawn.size < 6) drawn.add(remaining.removeAt(random.nextInt(remaining.size)))
        physics.balls.removeAll { it.n in drawn || it.flying }
        physics.mixing = 0f
        phase = Phase.DONE
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(s.simTitle, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = c.ink, modifier = Modifier.weight(1f))
            Text(
                s.justForFun, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.muted,
                modifier = Modifier.background(c.card, RoundedCornerShape(50)).border(1.dp, c.line, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        if (showTip) {
            Row(
                Modifier.fillMaxWidth().background(c.navPill, RoundedCornerShape(14.dp)).padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(s.simTip, fontSize = 14.sp, color = c.navPillInk, lineHeight = 19.sp, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                TextButton(onClick = onTipSeen) { Text(s.gotIt, color = c.navPillInk, fontWeight = FontWeight.Bold) }
            }
        }

        GameChips(selectedMax = max, enabled = !busy) { m ->
            if (m != max) {
                max = m
                picks = picks.filter { it <= m }
                stats = null
                resetDrum(m)
            }
        }
        Text(gameFor(max).name, fontSize = 13.sp, color = c.muted)

        DrumView(physics, Modifier.fillMaxWidth(0.86f).align(Alignment.CenterHorizontally))

        // Tray of drawn balls
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 6) {
                val n = drawn.getOrNull(i)
                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                    if (n != null) {
                        LottoBall(n, Modifier.fillMaxSize(), if (n in pickSet) c.match else null)
                    } else {
                        Box(Modifier.fillMaxSize().border(2.dp, c.line, CircleShape))
                    }
                }
            }
        }

        val (status, statusColor) = when (phase) {
            Phase.IDLE -> (if (picks.size == 6) s.ready else s.pickMore(6 - picks.size)) to c.ink
            Phase.MIXING -> s.mixing to c.ink
            Phase.DRAWING -> (if (hits > 0) "${s.ballOf(drawn.size)} · ${s.matchedSoFar(hits)}" else s.ballOf(drawn.size)) to c.ink
            Phase.DONE -> (if (hits == 6) s.allMatched else s.youMatched(hits)) to (if (hits > 0) c.match else c.ink)
        }
        Text(status, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = statusColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { if (busy) skipToResult() else startDraw() },
                enabled = busy || picks.size == 6,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.red, contentColor = Color.White),
            ) {
                Text(
                    when { busy -> s.skipToResult; phase == Phase.DONE -> s.drawAgain; else -> s.draw },
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                )
            }
            OutlinedButton(
                onClick = { stats = simulate(max, pickSet, 1000) },
                enabled = !busy && picks.size == 6,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(s.run1000, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.ink, maxLines = 1)
            }
            IconButton(onClick = { showRunInfo = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Info, contentDescription = s.run1000Help, tint = c.muted)
            }
        }

        stats?.let { st ->
            Box(Modifier.onGloballyPositioned { statsTop = it.positionInParent().y.toInt() }) {
                StatsCard(st)
            }
        }

        // Number picker
        Column(
            modifier = Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(18.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(s.yourNumbers, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.ink, modifier = Modifier.weight(1f))
                Text("${picks.size} / 6", fontSize = 13.sp, color = c.muted)
            }
            NumberGrid(max = max, picks = pickSet, enabled = !busy) { n ->
                picks = when {
                    n in pickSet -> picks - n
                    picks.size < 6 -> (picks + n).sorted()
                    else -> picks
                }
                if (phase == Phase.DONE) resetDrum(max)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picks = quickPick(max); resetDrum(max) }, enabled = !busy, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(s.quickPick, color = c.ink, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = { picks = emptyList(); stats = null; resetDrum(max) }, enabled = !busy, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(s.clear, color = c.ink, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        FinePrint(s.randomNote)
    }

    if (showRunInfo) {
        AlertDialog(
            onDismissRequest = { showRunInfo = false },
            title = { Text(s.run1000InfoTitle) },
            text = { Text(s.run1000InfoBody) },
            confirmButton = { TextButton(onClick = { showRunInfo = false }) { Text(s.ok, color = c.ink) } },
        )
    }
}

@Composable
private fun StatsCard(stats: Stats) {
    val c = Tambiolo.colors
    val s = S.str
    val top = stats.tally.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(
        modifier = Modifier.fillMaxWidth().background(c.card, RoundedCornerShape(18.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(s.simulatedDraws(String.format(Locale.US, "%,d", stats.runs)), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.ink)
        for (h in 6 downTo 0) {
            val count = stats.tally[h]
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(s.matches(h), fontSize = 13.sp, color = c.ink, modifier = Modifier.width(78.dp))
                Box(Modifier.weight(1f).height(10.dp).background(c.chip, RoundedCornerShape(5.dp))) {
                    if (count > 0) {
                        Box(
                            Modifier.fillMaxWidth((count.toFloat() / top).coerceAtLeast(0.02f)).fillMaxHeight()
                                .background(c.red, RoundedCornerShape(5.dp))
                        )
                    }
                }
                Text("$count", fontSize = 13.sp, color = c.ink, textAlign = TextAlign.End, modifier = Modifier.width(44.dp))
            }
        }
        Text(
            s.oddsAll(gameFor(stats.max).name, String.format(Locale.US, "%,d", combinations(stats.max))),
            fontSize = 13.sp, color = c.muted,
        )
    }
}
